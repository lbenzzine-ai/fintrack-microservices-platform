package com.fintrack.account.saga;

import com.fintrack.account.event.TransactionFailedEvent;
import com.fintrack.account.event.TransactionInitiatedEvent;
import com.fintrack.account.exception.AccountFrozenException;
import com.fintrack.account.exception.AccountNotFoundException;
import com.fintrack.account.exception.InsufficientFundsException;
import com.fintrack.account.messaging.KafkaMessagingStrategy;
import com.fintrack.account.messaging.MessagingStrategyRegistry;
import com.fintrack.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountConsumersTest {

    @Mock AccountService accountService;
    @Mock MessagingStrategyRegistry messaging;
    // sealed interface — mock permitted concrete impl
    @Mock KafkaMessagingStrategy strategy;

    @InjectMocks TransactionInitiatedConsumer initiatedConsumer;
    @InjectMocks TransactionFailedConsumer failedConsumer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(initiatedConsumer, "txFailedTopic", "tx.failed");
    }

    private TransactionInitiatedEvent event() {
        return new TransactionInitiatedEvent(
                "evt", "tx-1", "acc-src", "acc-dst",
                new BigDecimal("100"), new BigDecimal("0.5"),
                "USD",  "type", Instant.now());
    }

    @Test
    void shouldDebitOnTransactionInitiatedHappyPath() {
        initiatedConsumer.onTransactionInitiated(event());
        verify(accountService).debit("acc-src", "tx-1",
                new BigDecimal("100"), new BigDecimal("0.5"));
        verify(messaging, never()).active();
    }

    @Test
    void shouldPublishFailedWhenInsufficientFunds() {
        doThrow(new InsufficientFundsException("acc-src"))
                .when(accountService).debit(any(), any(), any(), any());
        when(messaging.active()).thenReturn(strategy);

        initiatedConsumer.onTransactionInitiated(event());

        ArgumentCaptor<TransactionFailedEvent> captor = ArgumentCaptor.forClass(TransactionFailedEvent.class);
        verify(strategy).publish(eq("tx.failed"), eq("tx-1"), captor.capture());
        assertReason(captor.getValue(), "INSUFFICIENT_FUNDS");
    }

    @Test
    void shouldPublishFailedWhenAccountFrozen() {
        doThrow(new AccountFrozenException("acc-src"))
                .when(accountService).debit(any(), any(), any(), any());
        when(messaging.active()).thenReturn(strategy);

        initiatedConsumer.onTransactionInitiated(event());

        ArgumentCaptor<TransactionFailedEvent> captor = ArgumentCaptor.forClass(TransactionFailedEvent.class);
        verify(strategy).publish(eq("tx.failed"), eq("tx-1"), captor.capture());
        assertReason(captor.getValue(), "ACCOUNT_FROZEN");
    }

    @Test
    void shouldPublishFailedWhenAccountNotFound() {
        doThrow(new AccountNotFoundException("acc-src"))
                .when(accountService).debit(any(), any(), any(), any());
        when(messaging.active()).thenReturn(strategy);

        initiatedConsumer.onTransactionInitiated(event());

        ArgumentCaptor<TransactionFailedEvent> captor = ArgumentCaptor.forClass(TransactionFailedEvent.class);
        verify(strategy).publish(eq("tx.failed"), eq("tx-1"), captor.capture());
        assertReason(captor.getValue(), "ACCOUNT_NOT_FOUND");
    }

    @Test
    void shouldPublishFailedOnUnexpectedException() {
        doThrow(new RuntimeException("boom"))
                .when(accountService).debit(any(), any(), any(), any());
        when(messaging.active()).thenReturn(strategy);

        initiatedConsumer.onTransactionInitiated(event());

        ArgumentCaptor<TransactionFailedEvent> captor = ArgumentCaptor.forClass(TransactionFailedEvent.class);
        verify(strategy).publish(eq("tx.failed"), eq("tx-1"), captor.capture());
        assertReason(captor.getValue(), "DOWNSTREAM");
    }

    @Test
    void shouldCompensateOnTransactionFailedWhenAlreadyDebited() {
        TransactionFailedEvent event = TransactionFailedEvent.builder()
                .eventId("evt").transactionUuid("tx-1")
                .fromAccountUuid("acc-src").toAccountUuid("acc-dst")
                .amount(new BigDecimal("100")).fee(new BigDecimal("0.5"))
                .currencyCode("USD").reasonCode("RISK_BLOCKED").reason("blocked")
                .alreadyDebited(true).occurredAt(Instant.now())
                .build();

        failedConsumer.onTransactionFailed(event);

        verify(accountService).compensateCredit("acc-src",
                new BigDecimal("100"), new BigDecimal("0.5"), "RISK_BLOCKED");
    }

    @Test
    void shouldSkipCompensationOnTransactionFailedWhenNotDebited() {
        TransactionFailedEvent event = TransactionFailedEvent.builder()
                .eventId("evt").transactionUuid("tx-1")
                .fromAccountUuid("acc-src").toAccountUuid("acc-dst")
                .amount(new BigDecimal("100")).fee(new BigDecimal("0.5"))
                .currencyCode("USD").reasonCode("INSUFFICIENT_FUNDS").reason("nope")
                .alreadyDebited(false).occurredAt(Instant.now())
                .build();

        failedConsumer.onTransactionFailed(event);

        verify(accountService, never()).compensateCredit(any(), any(), any(), any());
    }

    private static void assertReason(TransactionFailedEvent out, String expected) {
        org.assertj.core.api.Assertions.assertThat(out.reasonCode()).isEqualTo(expected);
        org.assertj.core.api.Assertions.assertThat(out.alreadyDebited()).isFalse();
    }
}
