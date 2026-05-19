package com.fintrack.account.saga;

import com.fintrack.account.event.TransactionFailedEvent;
import com.fintrack.account.event.TransactionInitiatedEvent;
import com.fintrack.account.exception.AccountFrozenException;
import com.fintrack.account.exception.AccountNotFoundException;
import com.fintrack.account.exception.InsufficientFundsException;
import com.fintrack.account.messaging.MessagingStrategy;
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

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock MessagingStrategy strategy;

    @InjectMocks TransactionInitiatedConsumer initiatedConsumer;
    @InjectMocks TransactionFailedConsumer failedConsumer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(initiatedConsumer, "txFailedTopic", "tx.failed");
    }

    private TransactionInitiatedEvent event() {
        TransactionInitiatedEvent e = new TransactionInitiatedEvent();
        e.setEventId("evt");
        e.setTransactionUuid("tx-1");
        e.setFromAccountUuid("acc-src");
        e.setToAccountUuid("acc-dst");
        e.setAmount(new BigDecimal("100"));
        e.setFee(new BigDecimal("0.5"));
        e.setCurrencyCode("USD");
        e.setOccurredAt(Instant.now());
        return e;
    }

    @Test
    void shouldDebitOnTransactionInitiatedHappyPath() {
        initiatedConsumer.onTransactionInitiated(event());
        verify(accountService).debit(eq("acc-src"), any(), eq("tx-1"),
                eq(new BigDecimal("100")), eq(new BigDecimal("0.5")));
        verify(messaging, never()).active();
    }

    @Test
    void shouldPublishFailedWhenInsufficientFunds() {
        doThrow(new InsufficientFundsException("acc-src"))
                .when(accountService).debit(any(), any(), any(), any(), any());
        when(messaging.active()).thenReturn(strategy);

        initiatedConsumer.onTransactionInitiated(event());

        ArgumentCaptor<TransactionFailedEvent> captor = ArgumentCaptor.forClass(TransactionFailedEvent.class);
        verify(strategy).publish(eq("tx.failed"), eq("tx-1"), captor.capture());
        assertThat(captor.getValue().getReasonCode()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(captor.getValue().isAlreadyDebited()).isFalse();
    }

    @Test
    void shouldPublishFailedWhenAccountFrozen() {
        doThrow(new AccountFrozenException("acc-src"))
                .when(accountService).debit(any(), any(), any(), any(), any());
        when(messaging.active()).thenReturn(strategy);

        initiatedConsumer.onTransactionInitiated(event());

        ArgumentCaptor<TransactionFailedEvent> captor = ArgumentCaptor.forClass(TransactionFailedEvent.class);
        verify(strategy).publish(eq("tx.failed"), eq("tx-1"), captor.capture());
        assertThat(captor.getValue().getReasonCode()).isEqualTo("ACCOUNT_FROZEN");
    }

    @Test
    void shouldPublishFailedWhenAccountNotFound() {
        doThrow(new AccountNotFoundException("acc-src"))
                .when(accountService).debit(any(), any(), any(), any(), any());
        when(messaging.active()).thenReturn(strategy);

        initiatedConsumer.onTransactionInitiated(event());

        ArgumentCaptor<TransactionFailedEvent> captor = ArgumentCaptor.forClass(TransactionFailedEvent.class);
        verify(strategy).publish(eq("tx.failed"), eq("tx-1"), captor.capture());
        assertThat(captor.getValue().getReasonCode()).isEqualTo("ACCOUNT_NOT_FOUND");
    }

    @Test
    void shouldPublishFailedOnUnexpectedException() {
        doThrow(new RuntimeException("boom"))
                .when(accountService).debit(any(), any(), any(), any(), any());
        when(messaging.active()).thenReturn(strategy);

        initiatedConsumer.onTransactionInitiated(event());

        ArgumentCaptor<TransactionFailedEvent> captor = ArgumentCaptor.forClass(TransactionFailedEvent.class);
        verify(strategy).publish(eq("tx.failed"), eq("tx-1"), captor.capture());
        assertThat(captor.getValue().getReasonCode()).isEqualTo("DOWNSTREAM");
    }

    @Test
    void shouldCompensateOnTransactionFailedWhenAlreadyDebited() {
        TransactionFailedEvent event = new TransactionFailedEvent();
        event.setEventId("evt");
        event.setTransactionUuid("tx-1");
        event.setFromAccountUuid("acc-src");
        event.setAmount(new BigDecimal("100"));
        event.setFee(new BigDecimal("0.5"));
        event.setReasonCode("RISK_BLOCKED");
        event.setReason("blocked");
        event.setAlreadyDebited(true);

        failedConsumer.onTransactionFailed(event);

        verify(accountService).compensateCredit("acc-src",
                new BigDecimal("100"), new BigDecimal("0.5"), "RISK_BLOCKED");
    }

    @Test
    void shouldSkipCompensationOnTransactionFailedWhenNotDebited() {
        TransactionFailedEvent event = new TransactionFailedEvent();
        event.setEventId("evt");
        event.setTransactionUuid("tx-1");
        event.setFromAccountUuid("acc-src");
        event.setAlreadyDebited(false);

        failedConsumer.onTransactionFailed(event);

        verify(accountService, never()).compensateCredit(any(), any(), any(), any());
    }
}
