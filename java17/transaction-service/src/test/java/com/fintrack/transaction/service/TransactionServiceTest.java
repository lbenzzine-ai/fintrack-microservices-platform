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
import com.fintrack.transaction.messaging.MessagingStrategy;
import com.fintrack.transaction.messaging.MessagingStrategyRegistry;
import com.fintrack.transaction.repository.TransactionRepository;
import com.fintrack.transaction.risk.RiskEngine;
import com.fintrack.transaction.risk.RiskFinding;
import com.fintrack.transaction.risk.RiskLevel;
import com.fintrack.transaction.risk.RiskScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock TransactionRepository transactionRepository;
    @Mock TransactionMapper mapper;
    @Mock FeeService feeService;
    @Mock MessagingStrategyRegistry messaging;
    @Mock MessagingStrategy strategy;
    @Mock RiskEngine riskEngine;

    @InjectMocks TransactionService service;

    @BeforeEach
    void setUp() {
        // @Lazy self-injection — point self at the same instance so internal calls flow through.
        ReflectionTestUtils.setField(service, "self", service);
        ReflectionTestUtils.setField(service, "txInitiatedTopic", "tx.initiated");
        ReflectionTestUtils.setField(service, "txCompletedTopic", "tx.completed");
        ReflectionTestUtils.setField(service, "txFailedTopic", "tx.failed");
        ReflectionTestUtils.setField(service, "notificationRequestedTopic", "notify");
        ReflectionTestUtils.setField(service, "riskAssessedTopic", "risk.assessed");
    }

    private static CreateTransactionRequest request(TransactionType type, String to, BigDecimal amount) {
        CreateTransactionRequest r = new CreateTransactionRequest();
        r.setType(type);
        r.setFromAccountUuid("src");
        r.setToAccountUuid(to);
        r.setAmount(amount);
        r.setCurrencyCode("USD");
        r.setDescription("test");
        return r;
    }

    private static Transaction tx(String uuid, TransactionStatus status) {
        return Transaction.builder()
                .uuid(uuid)
                .fromAccountUuid("src")
                .toAccountUuid("dst")
                .amount(new BigDecimal("100"))
                .fee(new BigDecimal("0.50"))
                .currencyCode("USD")
                .type(TransactionType.DOMESTIC_TRANSFER)
                .status(status)
                .build();
    }

    private static FeeQuote quote() {
        return FeeQuote.builder()
                .strategy("domestic")
                .principal(new BigDecimal("100"))
                .fee(new BigDecimal("0.50"))
                .total(new BigDecimal("100.50"))
                .build();
    }

    private static RiskScore cleanScore() {
        return RiskScore.clean("tx");
    }

    private static RiskScore reviewScore() {


        return RiskScore.from("tx", List.of(
                RiskFinding.builder().level(RiskLevel.HIGH).reason("r").ruleName("R").build()));
    }

    private static RiskScore blockedScore() {
        return RiskScore.from("tx", List.of(
                RiskFinding.builder().level(RiskLevel.CRITICAL).reason("self").ruleName("S").build()));
    }
    // ── create ───────────────────────────────────────────────────────────────────────
    @Test
    void shouldSaveTransactionAndPublishInitiatedEvent() {
        CreateTransactionRequest req = request(TransactionType.DOMESTIC_TRANSFER, "dst", new BigDecimal("100"));
        when(feeService.computeAndAudit(any(), any())).thenReturn(quote());
        when(riskEngine.assess(any())).thenReturn(blockedScore());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(messaging.active()).thenReturn(strategy);
        when(mapper.toResponse(any())).thenReturn(TransactionResponse.builder().uuid("u").build());
        TransactionResponse out = service.create(req);
        assertThat(out).isNotNull();
        verify(transactionRepository).save(any(Transaction.class));
        ArgumentCaptor<TransactionInitiatedEvent> evtCaptor = ArgumentCaptor.forClass(TransactionInitiatedEvent.class);
        verify(strategy).publish(eq("tx.initiated"), any(), evtCaptor.capture());
        assertThat(evtCaptor.getValue().getFromAccountUuid()).isEqualTo("src");
        assertThat(evtCaptor.getValue().getAmount()).isEqualByComparingTo("100");
    }

    @Test
    void shouldReturnMappedResponseFromCreate() {
        CreateTransactionRequest req = request(TransactionType.DOMESTIC_TRANSFER, "dst", new BigDecimal("50"));
        when(feeService.computeAndAudit(any(), any())).thenReturn(quote());
        when(riskEngine.assess(any())).thenReturn(cleanScore());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messaging.active()).thenReturn(strategy);
        TransactionResponse mapped = TransactionResponse.builder().uuid("MAPPED").build();
        when(mapper.toResponse(any())).thenReturn(mapped);

        assertThat(service.create(req)).isSameAs(mapped);
    }

    // ── validate ─────────────────────────────────────────────────────────────────────
    @Test
    void shouldThrowWhenFromEqualsTo() {
        CreateTransactionRequest req = request(TransactionType.DOMESTIC_TRANSFER, "src", new BigDecimal("10"));
        assertThatThrownBy(() -> service.create(req)).isInstanceOf(InvalidTransactionException.class);
    }

    @Test
    void shouldThrowWhenTransferMissingDestination() {
        CreateTransactionRequest req = request(TransactionType.INTERNATIONAL_TRANSFER, null, new BigDecimal("10"));
        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("toAccountUuid");
    }

    // ── findByUuid ───────────────────────────────────────────────────────────────────
    @Test
    void shouldReturnMappedTransactionWhenFound() {
        Transaction t = tx("u1", TransactionStatus.COMPLETED);
        TransactionResponse r = TransactionResponse.builder().uuid("u1").build();
        when(transactionRepository.findByUuid("u1")).thenReturn(Optional.of(t));
        when(mapper.toResponse(t)).thenReturn(r);

        assertThat(service.findByUuid("u1")).isSameAs(r);
    }

    @Test
    void shouldThrowWhenFindByUuidMissing() {
        when(transactionRepository.findByUuid("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByUuid("missing"))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("missing");
    }

    // ── findByAccount ────────────────────────────────────────────────────────────────
    @Test
    void shouldMapPageInFindByAccount() {
        Transaction t = tx("u1", TransactionStatus.COMPLETED);
        Pageable p = PageRequest.of(0, 10);
        Page<Transaction> page = new PageImpl<>(List.of(t), p, 1);
        when(transactionRepository.findByFromAccountUuidOrToAccountUuid("a", "a", p)).thenReturn(page);
        when(mapper.toResponse(t)).thenReturn(TransactionResponse.builder().uuid("u1").build());

        Page<TransactionResponse> out = service.findByAccount("a", p);
        assertThat(out.getContent()).hasSize(1);
    }

    // ── markDebited ──────────────────────────────────────────────────────────────────
    @Test
    void shouldSetDebitedAndCallComplete() {
        Transaction t = tx("u1", TransactionStatus.INITIATED);
        when(transactionRepository.findByUuid("u1")).thenReturn(Optional.of(t));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(riskEngine.assess(t)).thenReturn(cleanScore());
        when(messaging.active()).thenReturn(strategy);

        service.markDebited("u1");

        // After markDebited → complete: status flips DEBITED then COMPLETED.
        assertThat(t.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(strategy).publish(eq("tx.completed"), any(), any(TransactionCompletedEvent.class));
    }

    @Test
    void shouldIgnoreMarkDebitedWhenAlreadyCompleted() {
        Transaction t = tx("u1", TransactionStatus.COMPLETED);
        when(transactionRepository.findByUuid("u1")).thenReturn(Optional.of(t));

        service.markDebited("u1");

        verify(transactionRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreMarkDebitedWhenAlreadyFailed() {
        Transaction t = tx("u1", TransactionStatus.FAILED);
        when(transactionRepository.findByUuid("u1")).thenReturn(Optional.of(t));

        service.markDebited("u1");

        verify(transactionRepository, never()).save(any());
    }

    // ── complete ─────────────────────────────────────────────────────────────────────
    @Test
    void shouldPublishCompletedAndNotificationOnNormalComplete() {
        Transaction t = tx("u1", TransactionStatus.DEBITED);
        when(riskEngine.assess(t)).thenReturn(cleanScore());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messaging.active()).thenReturn(strategy);

        service.complete(t);

        assertThat(t.getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        verify(strategy).publish(eq("tx.completed"), any(), any(TransactionCompletedEvent.class));
        verify(strategy).publish(eq("notify"), any(), any(NotificationRequestedEvent.class));
    }

    @Test
    void shouldPublishRiskAssessedWhenRequiresReview() {
        Transaction t = tx("u1", TransactionStatus.DEBITED);
        when(riskEngine.assess(t)).thenReturn(reviewScore());
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messaging.active()).thenReturn(strategy);

        service.complete(t);

        verify(strategy).publish(eq("risk.assessed"), any(), any());
        verify(strategy).publish(eq("tx.completed"), any(), any());
    }

    @Test
    void shouldMarkFailedWhenRiskBlocked() {
        Transaction t = tx("u1", TransactionStatus.DEBITED);
        when(riskEngine.assess(t)).thenReturn(blockedScore());
        when(transactionRepository.findByUuid("u1")).thenReturn(Optional.of(t));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messaging.active()).thenReturn(strategy);

        service.complete(t);

        assertThat(t.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(t.getFailureReason()).contains("Blocked by Risk Engine");
        verify(strategy).publish(eq("tx.failed"), any(), any(TransactionFailedEvent.class));
        verify(strategy, never()).publish(eq("tx.completed"), any(), any());
    }

    // ── markFailed ───────────────────────────────────────────────────────────────────
    @Test
    void shouldSetFailedAndPublishOnMarkFailed() {
        Transaction t = tx("u1", TransactionStatus.DEBITED);
        when(transactionRepository.findByUuid("u1")).thenReturn(Optional.of(t));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messaging.active()).thenReturn(strategy);

        service.markFailed("u1", "REASON_CODE", "human reason", true);

        assertThat(t.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(t.getFailureReason()).isEqualTo("REASON_CODE:human reason");
        ArgumentCaptor<TransactionFailedEvent> captor = ArgumentCaptor.forClass(TransactionFailedEvent.class);
        verify(strategy).publish(eq("tx.failed"), any(), captor.capture());
        assertThat(captor.getValue().getReasonCode()).isEqualTo("REASON_CODE");
        assertThat(captor.getValue().getReason()).isEqualTo("human reason");
        assertThat(captor.getValue().isAlreadyDebited()).isTrue();
    }

    @Test
    void shouldNotMarkFailedWhenAlreadyCompleted() {
        Transaction t = tx("u1", TransactionStatus.COMPLETED);
        when(transactionRepository.findByUuid("u1")).thenReturn(Optional.of(t));

        service.markFailed("u1", "X", "y", false);

        verify(transactionRepository, never()).save(any());
        verify(messaging, never()).active();
    }

    @Test
    void shouldNotMarkFailedWhenAlreadyFailed() {
        Transaction t = tx("u1", TransactionStatus.FAILED);
        when(transactionRepository.findByUuid("u1")).thenReturn(Optional.of(t));

        service.markFailed("u1", "X", "y", false);

        verify(transactionRepository, never()).save(any());
    }

    // ── markCompensated ──────────────────────────────────────────────────────────────
    @Test
    void shouldSetCompensatedStatusAndReason() {
        Transaction t = tx("u1", TransactionStatus.FAILED);
        when(transactionRepository.findByUuid("u1")).thenReturn(Optional.of(t));
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markCompensated("u1", "CODE", "reason");

        assertThat(t.getStatus()).isEqualTo(TransactionStatus.COMPENSATED);
        assertThat(t.getFailureReason()).isEqualTo("CODE:reason");
        verify(transactionRepository).save(t);
    }

    // ── lookup (covered indirectly) ──────────────────────────────────────────────────
    @Test
    void shouldThrowFromLookupWhenNotFound() {
        when(transactionRepository.findByUuid("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markDebited("ghost"))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("ghost");
    }
}
