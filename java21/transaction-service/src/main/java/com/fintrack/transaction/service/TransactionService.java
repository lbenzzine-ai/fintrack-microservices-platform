package com.fintrack.transaction.service;

import com.fintrack.transaction.dto.CreateTransactionRequest;
import com.fintrack.transaction.dto.FeeQuote;
import com.fintrack.transaction.dto.TransactionResponse;
import com.fintrack.transaction.entity.Transaction;
import com.fintrack.transaction.entity.TransactionStatus;
import com.fintrack.transaction.entity.TransactionType;
import com.fintrack.transaction.event.NotificationRequestedEvent;
import com.fintrack.transaction.event.TransactionCompletedEvent;
import com.fintrack.transaction.event.TransactionFailedEvent;
import com.fintrack.transaction.event.TransactionInitiatedEvent;
import com.fintrack.transaction.exception.InvalidTransactionException;
import com.fintrack.transaction.exception.TransactionNotFoundException;
import com.fintrack.transaction.mapper.TransactionMapper;
import com.fintrack.transaction.messaging.MessagingStrategyRegistry;
import com.fintrack.transaction.repository.TransactionRepository;
import com.fintrack.transaction.strategy.fee.FeeCalculationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Saga orchestrator — Java 21 stack uses pattern-matching switch in {@link #validate} to enforce
 * the destination-account requirement per {@link TransactionType}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper mapper;
    private final FeeService feeService;
    private final MessagingStrategyRegistry messaging;

    @Value("${fintrack.messaging.kafka.topics.transaction-initiated:fintrack.tx.initiated}")
    private String txInitiatedTopic;

    @Value("${fintrack.messaging.kafka.topics.transaction-completed:fintrack.tx.completed}")
    private String txCompletedTopic;

    @Value("${fintrack.messaging.kafka.topics.transaction-failed:fintrack.tx.failed}")
    private String txFailedTopic;

    @Value("${fintrack.messaging.kafka.topics.notification-requested:fintrack.notification.requested}")
    private String notificationRequestedTopic;

    @Transactional
    @CacheEvict(value = "transactions:byUuid", allEntries = true)
    public TransactionResponse create(CreateTransactionRequest req) {
        validate(req);

        FeeCalculationContext ctx = new FeeCalculationContext(
                req.type(),
                req.amount(),
                req.currencyCode(),
                req.fromAccountUuid(),
                req.toAccountUuid(),
                req.type() == TransactionType.INTERNATIONAL_TRANSFER,
                false);

        String uuid = UUID.randomUUID().toString();
        FeeQuote quote = feeService.computeAndAudit(uuid, ctx);

        Transaction tx = Transaction.builder()
                .uuid(uuid)
                .fromAccountUuid(req.fromAccountUuid())
                .toAccountUuid(req.toAccountUuid())
                .amount(req.amount())
                .fee(quote.fee())
                .currencyCode(req.currencyCode())
                .type(req.type())
                .description(req.description())
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
        complete(t);
    }

    @Transactional
    public void complete(Transaction t) {
        t.setStatus(TransactionStatus.COMPLETED);
        transactionRepository.save(t);

        TransactionCompletedEvent completed = new TransactionCompletedEvent(
                UUID.randomUUID().toString(),
                t.getUuid(),
                t.getFromAccountUuid(),
                t.getToAccountUuid(),
                t.getAmount(),
                t.getFee(),
                t.getCurrencyCode(),
                t.getType().name(),
                Instant.now());
        messaging.active().publish(txCompletedTopic, t.getUuid(), completed);

        NotificationRequestedEvent notify = new NotificationRequestedEvent(
                UUID.randomUUID().toString(),
                t.getUuid(),
                t.getFromAccountUuid(),
                "EMAIL",
                "transaction.completed",
                "Your transaction has completed",
                Map.of(
                        "amount", t.getAmount(),
                        "fee", t.getFee(),
                        "currency", t.getCurrencyCode(),
                        "type", t.getType().name()),
                Instant.now());
        messaging.active().publish(notificationRequestedTopic, t.getUuid(), notify);
        log.info("[SAGA] tx={} COMPLETED, downstream notify published", t.getUuid());
    }

    @Transactional
    @CacheEvict(value = "transactions:byUuid", allEntries = true)
    public void markFailed(String transactionUuid, String reasonCode, String reason, boolean wasDebited) {
        Transaction t = lookup(transactionUuid);
        if (t.getStatus() == TransactionStatus.COMPLETED || t.getStatus() == TransactionStatus.FAILED) return;

        t.setStatus(TransactionStatus.FAILED);
        t.setFailureReason(reasonCode + ":" + reason);
        transactionRepository.save(t);

        TransactionFailedEvent failed = new TransactionFailedEvent(
                UUID.randomUUID().toString(),
                t.getUuid(),
                t.getFromAccountUuid(),
                t.getToAccountUuid(),
                t.getAmount(),
                t.getFee(),
                t.getCurrencyCode(),
                reasonCode,
                reason,
                wasDebited,
                Instant.now());
        messaging.active().publish(txFailedTopic, t.getUuid(), failed);
        log.warn("[SAGA] tx={} FAILED reason={} wasDebited={}", t.getUuid(), reasonCode, wasDebited);
    }

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

    private void publishInitiated(Transaction t) {
        var event = new TransactionInitiatedEvent(
                UUID.randomUUID().toString(),
                t.getUuid(),
                t.getFromAccountUuid(),
                t.getToAccountUuid(),
                t.getAmount(),
                t.getFee(),
                t.getCurrencyCode(),
                t.getType().name(),
                Instant.now());
        messaging.active().publish(txInitiatedTopic, t.getUuid(), event);
    }

    private void validate(CreateTransactionRequest req) {
        if (req.toAccountUuid() != null && req.fromAccountUuid().equals(req.toAccountUuid())) {
            throw new InvalidTransactionException("fromAccountUuid and toAccountUuid must differ");
        }
        // Java 21 — exhaustive pattern-matching switch (compiler enforces every case).
        switch (req.type()) {
            case DOMESTIC_TRANSFER, INTERNATIONAL_TRANSFER, INTERNAL_TRANSFER -> {
                if (req.toAccountUuid() == null) {
                    throw new InvalidTransactionException("toAccountUuid is required for " + req.type());
                }
            }
            case ATM_WITHDRAWAL, BILL_PAYMENT -> { /* no destination needed */ }
        }
    }
}
