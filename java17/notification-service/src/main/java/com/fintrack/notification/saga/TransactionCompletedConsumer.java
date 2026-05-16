package com.fintrack.notification.saga;

import com.fintrack.notification.dto.SendNotificationRequest;
import com.fintrack.notification.entity.NotificationChannel;
import com.fintrack.notification.event.TransactionCompletedEvent;
import com.fintrack.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Send a "transaction completed" email when the saga terminates successfully. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCompletedConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "${fintrack.messaging.kafka.topics.transaction-completed}",
        groupId = "${spring.application.name}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionCompleted(@Payload TransactionCompletedEvent event) {
        log.info("[NOTIFY] tx-completed tx={} amount={}", event.getTransactionUuid(), event.getAmount());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipient(event.getFromAccountUuid() + "@example.local");
        req.setChannel(NotificationChannel.EMAIL);
        req.setTemplate("transaction-completed");
        req.setSubject("Transaction completed");
        req.setAccountUuid(event.getFromAccountUuid());
        req.setTransactionUuid(event.getTransactionUuid());
        req.setPayload(Map.of(
                "amount", event.getAmount(),
                "fee", event.getFee(),
                "currency", event.getCurrencyCode(),
                "type", event.getType()));
        notificationService.createAsync(req);
    }
}
