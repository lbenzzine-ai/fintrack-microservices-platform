package com.fintrack.notification.saga;

import com.fintrack.notification.event.AccountCreatedEvent;
import com.fintrack.notification.event.NotificationRequestedEvent;
import com.fintrack.notification.event.TransactionCompletedEvent;
import com.fintrack.notification.event.TransactionFailedEvent;
import com.fintrack.notification.event.UserRegisteredEvent;
import com.fintrack.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/** Kills the VoidMethodCallMutator on each consumer's notificationService.createAsync(...) call. */
@ExtendWith(MockitoExtension.class)
class ConsumersTest {

    @Mock NotificationService notificationService;

    @InjectMocks TransactionFailedConsumer transactionFailedConsumer;
    @InjectMocks TransactionCompletedConsumer transactionCompletedConsumer;
    @InjectMocks NotificationRequestedConsumer notificationRequestedConsumer;
    @InjectMocks AccountCreatedConsumer accountCreatedConsumer;
    @InjectMocks UserRegisteredConsumer userRegisteredConsumer;

    @Test
    void shouldDispatchOnTransactionFailed() {
        TransactionFailedEvent event = new TransactionFailedEvent(
                "evt", "tx-1", "acc-src", new BigDecimal("100"), BigDecimal.ZERO, "USD",
                "INSUFFICIENT", "no funds", true, Instant.now());

        transactionFailedConsumer.onTransactionFailed(event);

        verify(notificationService).createAsync(any());
    }

    @Test
    void shouldDispatchOnTransactionCompleted() {
        TransactionCompletedEvent event = new TransactionCompletedEvent(
                "evt", "tx-2", "acc-from", "acc-to", new BigDecimal("50"),
                new BigDecimal("0.5"), "USD", "DOMESTIC_TRANSFER", Instant.now());

        transactionCompletedConsumer.onTransactionCompleted(event);

        verify(notificationService).createAsync(any());
    }

    @Test
    void shouldDispatchOnNotificationRequested() {
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                "evt", "tx-3", "acc-x", "EMAIL", "custom", "subj",
                Map.of("k", "v"), Instant.now());

        notificationRequestedConsumer.onNotificationRequested(event);

        verify(notificationService).createAsync(any());
    }

    @Test
    void shouldDispatchOnAccountCreated() {
        AccountCreatedEvent event = new AccountCreatedEvent(
                "evt", "acc-new", "user-1", BigDecimal.ZERO, "USD", Instant.now());

        accountCreatedConsumer.onAccountCreated(event);

        verify(notificationService).createAsync(any());
    }

    @Test
    void shouldDispatchOnUserRegistered() {
        UserRegisteredEvent event = new UserRegisteredEvent(
                "evt", "user-1", "alice", "alice@example.com", Instant.now());

        userRegisteredConsumer.onUserRegistered(event);

        verify(notificationService).createAsync(any());
    }
}
