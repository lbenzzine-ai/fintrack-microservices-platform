package com.fintrack.notification.saga;

import com.fintrack.notification.dto.SendNotificationRequest;
import com.fintrack.notification.entity.NotificationChannel;
import com.fintrack.notification.event.AccountCreatedEvent;
import com.fintrack.notification.event.NotificationRequestedEvent;
import com.fintrack.notification.event.TransactionCompletedEvent;
import com.fintrack.notification.event.TransactionFailedEvent;
import com.fintrack.notification.event.UserRegisteredEvent;
import com.fintrack.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * One surgical test per saga consumer — kills the VoidMethodCall mutators on every
 * {@code req.setX(...)} call plus the final {@code notificationService.createAsync(req)}.
 */
@ExtendWith(MockitoExtension.class)
class ConsumersTest {

    @Mock NotificationService notificationService;

    @InjectMocks TransactionFailedConsumer transactionFailedConsumer;
    @InjectMocks TransactionCompletedConsumer transactionCompletedConsumer;
    @InjectMocks NotificationRequestedConsumer notificationRequestedConsumer;
    @InjectMocks AccountCreatedConsumer accountCreatedConsumer;
    @InjectMocks UserRegisteredConsumer userRegisteredConsumer;

    private SendNotificationRequest capture() {
        ArgumentCaptor<SendNotificationRequest> captor = ArgumentCaptor.forClass(SendNotificationRequest.class);
        verify(notificationService).createAsync(captor.capture());
        return captor.getValue();
    }

    @Test
    void onTransactionFailed_buildsAndSubmitsRequestWithAllFields() {
        TransactionFailedEvent event = new TransactionFailedEvent();
        event.setEventId("evt-1");
        event.setTransactionUuid("tx-1");
        event.setFromAccountUuid("acc-src");
        event.setAmount(new BigDecimal("100"));
        event.setCurrencyCode("USD");
        event.setReasonCode("INSUFFICIENT_FUNDS");
        event.setReason("not enough balance");

        transactionFailedConsumer.onTransactionFailed(event);

        SendNotificationRequest req = capture();
        assertThat(req.getRecipient()).isEqualTo("acc-src@example.local");
        assertThat(req.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(req.getTemplate()).isEqualTo("transaction-failed");
        assertThat(req.getSubject()).isEqualTo("We couldn't complete your transaction");
        assertThat(req.getAccountUuid()).isEqualTo("acc-src");
        assertThat(req.getTransactionUuid()).isEqualTo("tx-1");
        assertThat(req.getPayload()).containsEntry("amount", new BigDecimal("100"))
                                    .containsEntry("currency", "USD")
                                    .containsEntry("reasonCode", "INSUFFICIENT_FUNDS")
                                    .containsEntry("reason", "not enough balance");
    }

    @Test
    void onTransactionCompleted_buildsAndSubmitsRequestWithAllFields() {
        TransactionCompletedEvent event = new TransactionCompletedEvent();
        event.setEventId("evt-2");
        event.setTransactionUuid("tx-2");
        event.setFromAccountUuid("acc-from");
        event.setToAccountUuid("acc-to");
        event.setAmount(new BigDecimal("50"));
        event.setFee(new BigDecimal("0.5"));
        event.setCurrencyCode("USD");
        event.setType("DOMESTIC_TRANSFER");

        transactionCompletedConsumer.onTransactionCompleted(event);

        SendNotificationRequest req = capture();
        assertThat(req.getRecipient()).isEqualTo("acc-from@example.local");
        assertThat(req.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(req.getTemplate()).isEqualTo("transaction-completed");
        assertThat(req.getSubject()).isEqualTo("Transaction completed");
        assertThat(req.getAccountUuid()).isEqualTo("acc-from");
        assertThat(req.getTransactionUuid()).isEqualTo("tx-2");
        assertThat(req.getPayload()).containsEntry("amount", new BigDecimal("50"))
                                    .containsEntry("fee", new BigDecimal("0.5"))
                                    .containsEntry("currency", "USD")
                                    .containsEntry("type", "DOMESTIC_TRANSFER");
    }

    @Test
    void onNotificationRequested_forwardsEventFieldsIntoRequest() {
        NotificationRequestedEvent event = new NotificationRequestedEvent();
        event.setEventId("evt-3");
        event.setTransactionUuid("tx-3");
        event.setAccountUuid("acc-x");
        event.setChannel("EMAIL");
        event.setTemplate("custom");
        event.setSubject("Heads up");
        event.setPayload(Map.of("k", "v"));

        notificationRequestedConsumer.onNotificationRequested(event);

        SendNotificationRequest req = capture();
        assertThat(req.getRecipient()).isEqualTo("acc-x@example.local");
        assertThat(req.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(req.getTemplate()).isEqualTo("custom");
        assertThat(req.getSubject()).isEqualTo("Heads up");
        assertThat(req.getAccountUuid()).isEqualTo("acc-x");
        assertThat(req.getTransactionUuid()).isEqualTo("tx-3");
        assertThat(req.getPayload()).containsEntry("k", "v");
    }

    @Test
    void onAccountCreated_buildsAndSubmitsWelcomeRequest() {
        AccountCreatedEvent event = new AccountCreatedEvent();
        event.setEventId("evt-4");
        event.setAccountUuid("acc-new");
        event.setUserUuid("user-1");
        event.setCurrencyCode("USD");
        event.setOpeningBalance(BigDecimal.ZERO);

        accountCreatedConsumer.onAccountCreated(event);

        SendNotificationRequest req = capture();
        assertThat(req.getRecipient()).isEqualTo("user-1@example.local");
        assertThat(req.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(req.getTemplate()).isEqualTo("account-created");
        assertThat(req.getSubject()).isEqualTo("Your FinTrack wallet is ready");
        assertThat(req.getAccountUuid()).isEqualTo("acc-new");
        assertThat(req.getPayload()).containsEntry("openingBalance", BigDecimal.ZERO)
                                    .containsEntry("currency", "USD");
    }

    @Test
    void onUserRegistered_buildsWelcomeEmail() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setEventId("evt-5");
        event.setUserUuid("user-1");
        event.setEmail("alice@example.com");
        event.setUsername("alice");

        userRegisteredConsumer.onUserRegistered(event);

        SendNotificationRequest req = capture();
        assertThat(req.getRecipient()).isEqualTo("alice@example.com");
        assertThat(req.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(req.getTemplate()).isEqualTo("welcome");
        assertThat(req.getSubject()).isEqualTo("Welcome to FinTrack");
        assertThat(req.getPayload()).containsEntry("username", "alice");
    }
}
