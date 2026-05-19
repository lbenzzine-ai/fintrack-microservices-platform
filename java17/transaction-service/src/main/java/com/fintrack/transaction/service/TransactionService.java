package com.fintrack.transaction.service;

import com.fintrack.transaction.dto.CreateTransactionRequest;
import com.fintrack.transaction.dto.FeeQuote;
import com.fintrack.transaction.dto.TransactionResponse;
import com.fintrack.transaction.entity.Transaction;
import com.fintrack.transaction.entity.TransactionStatus;
import com.fintrack.transaction.event.NotificationRequestedEvent;
import com.fintrack.transaction.event.RiskAssessedEvent;
import com.fintrack.transaction.event.TransactionCompletedEvent;
import com.fintrack.transaction.event.TransactionFailedEvent;
import com.fintrack.transaction.event.TransactionInitiatedEvent;
import com.fintrack.transaction.exception.InvalidTransactionException;
import com.fintrack.transaction.exception.TransactionNotFoundException;
import com.fintrack.transaction.mapper.TransactionMapper;
import com.fintrack.transaction.messaging.MessagingStrategyRegistry;
import com.fintrack.transaction.repository.TransactionRepository;
import com.fintrack.transaction.risk.RiskEngine;
import com.fintrack.transaction.risk.RiskFinding;
import com.fintrack.transaction.risk.RiskScore;
import com.fintrack.transaction.strategy.fee.FeeCalculationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Saga orchestrator for the transaction lifecycle:
 *
 * <pre>
 *   PENDING ──create──▶ INITIATED ──tx-initiated──▶ (account-service debit)
 *                          │
 *                          ▼ account-debited
 *                       DEBITED ──complete──▶ COMPLETED + notify
 *                          │
 *                          ▼ downstream failure
 *                       FAILED + emit tx-failed(alreadyDebited=true)
 * </pre>
 *
 * <p>State transitions are written here; the Kafka listeners in {@code saga/*Consumer.java}
 * delegate to these methods, keeping the listeners thin and testable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper mapper;
    private final FeeService feeService;
    private final MessagingStrategyRegistry messaging;
    private final RiskEngine riskEngine;

    /** Self-reference via the Spring proxy so internal calls to public @Transactional /
     *  @CacheEvict methods go through the AOP interceptor chain instead of bypassing it. */
    @Autowired @Lazy
    private TransactionService self;

    @Value("${fintrack.messaging.kafka.topics.transaction-initiated:fintrack.tx.initiated}")
    private String txInitiatedTopic;

    @Value("${fintrack.messaging.kafka.topics.transaction-completed:fintrack.tx.completed}")
    private String txCompletedTopic;

    @Value("${fintrack.messaging.kafka.topics.transaction-failed:fintrack.tx.failed}")
    private String txFailedTopic;

    @Value("${fintrack.messaging.kafka.topics.notification-requested:fintrack.notification.requested}")
    private String notificationRequestedTopic;

    @Value("${fintrack.messaging.kafka.topics.risk-assessed:fintrack.risk-assessed}")
    private String riskAssessedTopic;

    @Transactional
    @CacheEvict(value = "transactions:byUuid", allEntries = true)
    public TransactionResponse create(CreateTransactionRequest req) {
        validate(req);

        FeeCalculationContext ctx = FeeCalculationContext.builder()
                .type(req.getType())
                .amount(req.getAmount())
                .currencyCode(req.getCurrencyCode())
                .fromAccountUuid(req.getFromAccountUuid())
                .toAccountUuid(req.getToAccountUuid())
                .crossBorder(req.getType() == com.fintrack.transaction.entity.TransactionType.INTERNATIONAL_TRANSFER)
                .build();

        String uuid = UUID.randomUUID().toString();
        FeeQuote quote = feeService.computeAndAudit(uuid, ctx);

        Transaction tx = Transaction.builder()
                .uuid(uuid)
                .fromAccountUuid(req.getFromAccountUuid())
                .toAccountUuid(req.getToAccountUuid())
                .amount(req.getAmount())
                .fee(quote.getFee())
                .currencyCode(req.getCurrencyCode())
                .type(req.getType())
                .description(req.getDescription())
                .status(TransactionStatus.INITIATED)
                .build();
        Transaction saved = transactionRepository.save(tx);
        publishInitiated(saved);
        log.info("Transaction created uuid={} type={} amount={} fee={}",
                saved.getUuid(), saved.getType(), saved.getAmount(), saved.getFee());
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "transactions:byUuid", key = "#uuid", unless = "#result == null")
    public TransactionResponse findByUuid(String uuid) {
        Transaction t = transactionRepository.findByUuid(uuid)
                .orElseThrow(() -> new TransactionNotFoundException(uuid));
        return mapper.toResponse(t);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> findByAccount(String accountUuid, Pageable pageable) {
        return transactionRepository.findByFromAccountUuidOrToAccountUuid(accountUuid, accountUuid, pageable)
                .map(mapper::toResponse);
    }

    /** Step 2 of the saga — account-service has debited the source. Mark DEBITED and complete. */
    @Transactional
    @CacheEvict(value = "transactions:byUuid", allEntries = true)
    public void markDebited(String transactionUuid) {
        Transaction t = lookup(transactionUuid);
        if (t.getStatus() == TransactionStatus.COMPLETED || t.getStatus() == TransactionStatus.FAILED) {
            log.debug("Tx {} already terminal ({}), ignoring debited event", transactionUuid, t.getStatus());
            return;
        }
        t.setStatus(TransactionStatus.DEBITED);
        transactionRepository.save(t);
        self.complete(t);
    }

    /** Step 3 — emit COMPLETED + ask notification-service to ping the user. */
    @Transactional
    public void complete(Transaction t) {
        // Tx is DEBITED on entry (set by markDebited). Assess risk BEFORE flipping to
        // COMPLETED so a blocked tx still satisfies markFailed's non-terminal guard.
        RiskScore score = riskEngine.assess(t);
        if (score.isBlocked()) {
            String topReason = topFindingReason(score);
            self.markFailed(t.getUuid(), "RISK_BLOCKED",
                    "Blocked by Risk Engine: " + topReason, true);
            return;
        }

        t.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(t);

        if (score.isRequiresReview()) {
            publishRiskAssessed(t, score);
        }

        TransactionCompletedEvent completed = TransactionCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionUuid(t.getUuid())
                .fromAccountUuid(t.getFromAccountUuid())
                .toAccountUuid(t.getToAccountUuid())
                .amount(t.getAmount())
                .fee(t.getFee())
                .currencyCode(t.getCurrencyCode())
                .type(t.getType().name())
                .occurredAt(Instant.now())
                .build();
        messaging.active().publish(txCompletedTopic, t.getUuid(), completed);

        NotificationRequestedEvent notify = NotificationRequestedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionUuid(t.getUuid())
                .accountUuid(t.getFromAccountUuid())
                .channel("EMAIL")
                .template("transaction.completed")
                .subject("Your transaction has completed")
                .payload(Map.of(
                        "amount", t.getAmount(),
                        "fee", t.getFee(),
                        "currency", t.getCurrencyCode(),
                        "type", t.getType().name()))
                .occurredAt(Instant.now())
                .build();
        messaging.active().publish(notificationRequestedTopic, t.getUuid(), notify);
        log.info("[SAGA] tx={} COMPLETED, downstream notify published", t.getUuid());
    }

    /** Saga compensation — mark FAILED and emit tx-failed for account-service to compensate. */
    @Transactional
    @CacheEvict(value = "transactions:byUuid", allEntries = true)
    public void markFailed(String transactionUuid, String reasonCode, String reason, boolean wasDebited) {
        Transaction t = lookup(transactionUuid);
        if (t.getStatus() == TransactionStatus.COMPLETED || t.getStatus() == TransactionStatus.FAILED) return;

        t.setStatus(TransactionStatus.FAILED);
        t.setFailureReason(reasonCode + ":" + reason);
        transactionRepository.save(t);

        TransactionFailedEvent failed = TransactionFailedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionUuid(t.getUuid())
                .fromAccountUuid(t.getFromAccountUuid())
                .toAccountUuid(t.getToAccountUuid())
                .amount(t.getAmount())
                .fee(t.getFee())
                .currencyCode(t.getCurrencyCode())
                .reasonCode(reasonCode)
                .reason(reason)
                .alreadyDebited(wasDebited)
                .occurredAt(Instant.now())
                .build();
        messaging.active().publish(txFailedTopic, t.getUuid(), failed);
        log.warn("[SAGA] tx={} FAILED reason={} wasDebited={}", t.getUuid(), reasonCode, wasDebited);
    }

    /** Called by the tx-failed consumer to record the compensated terminal state on our side. */
    @Transactional
    @CacheEvict(value = "transactions:byUuid", allEntries = true)
    public void markCompensated(String transactionUuid, String reasonCode, String reason) {
        Transaction t = lookup(transactionUuid);
        t.setStatus(TransactionStatus.COMPENSATED);
        t.setFailureReason(reasonCode + ":" + reason);
        transactionRepository.save(t);
        log.info("[SAGA] tx={} COMPENSATED reason={}", t.getUuid(), reasonCode);
    }

    private Transaction lookup(String uuid) {
        return transactionRepository.findByUuid(uuid)
                .orElseThrow(() -> new TransactionNotFoundException(uuid));
    }

    private void publishRiskAssessed(Transaction t, RiskScore score) {
        List<String> triggered = score.getFindings().stream()
                .map(RiskFinding::getRuleName)
                .collect(Collectors.toList());
        RiskAssessedEvent event = RiskAssessedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionUuid(t.getUuid())
                .level(score.getLevel().name())
                .score(score.getScore())
                .blocked(score.isBlocked())
                .requiresReview(score.isRequiresReview())
                .triggeredRules(triggered)
                .assessedAt(score.getAssessedAt())
                .occurredAt(Instant.now())
                .build();
        messaging.active().publish(riskAssessedTopic, t.getUuid(), event);
        log.info("[RISK] tx={} REVIEW level={} score={} rules={}",
                t.getUuid(), score.getLevel(), score.getScore(), triggered);
    }

    private static String topFindingReason(RiskScore score) {
        return score.getFindings().stream()
                .max(Comparator.comparingInt(f -> f.getLevel().weight()))
                .map(RiskFinding::getReason)
                .orElse("unspecified");
    }

    private void publishInitiated(Transaction t) {
        TransactionInitiatedEvent event = TransactionInitiatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .transactionUuid(t.getUuid())
                .fromAccountUuid(t.getFromAccountUuid())
                .toAccountUuid(t.getToAccountUuid())
                .amount(t.getAmount())
                .fee(t.getFee())
                .currencyCode(t.getCurrencyCode())
                .type(t.getType().name())
                .occurredAt(Instant.now())
                .build();
        messaging.active().publish(txInitiatedTopic, t.getUuid(), event);
    }

    private void validate(CreateTransactionRequest req) {
        if (req.getToAccountUuid() != null && req.getFromAccountUuid().equals(req.getToAccountUuid())) {
            throw new InvalidTransactionException("fromAccountUuid and toAccountUuid must differ");
        }
        switch (req.getType()) {
            case DOMESTIC_TRANSFER, INTERNATIONAL_TRANSFER, INTERNAL_TRANSFER -> {
                if (req.getToAccountUuid() == null) {
                    throw new InvalidTransactionException("toAccountUuid is required for " + req.getType());
                }
            }
            case ATM_WITHDRAWAL, BILL_PAYMENT -> { /* no destination needed */ }
        }
    }
}
