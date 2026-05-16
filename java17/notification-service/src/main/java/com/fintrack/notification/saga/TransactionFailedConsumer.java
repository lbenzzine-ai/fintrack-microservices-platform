package com.fintrack.notification.saga;

import com.fintrack.notification.dto.SendNotificationRequest;
import com.fintrack.notification.entity.NotificationChannel;
import com.fintrack.notification.event.TransactionFailedEvent;
import com.fintrack.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionFailedConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "${fintrack.messaging.kafka.topics.transaction-failed}",
        groupId = "${spring.application.name}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onTransactionFailed(@Payload TransactionFailedEvent event) {
        log.warn("[NOTIFY] tx-failed tx={} reason={}", event.getTransactionUuid(), event.getReasonCode());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipient(event.getFromAccountUuid() + "@example.local");
        req.setChannel(NotificationChannel.EMAIL);
        req.setTemplate("transaction-failed");
        req.setSubject("We couldn't complete your transaction");
        req.setAccountUuid(event.getFromAccountUuid());
        req.setTransactionUuid(event.getTransactionUuid());
        req.setPayload(Map.of(
                "amount", event.getAmount(),
                "currency", event.getCurrencyCode(),
                "reasonCode", event.getReasonCode(),
                "reason", String.valueOf(event.getReason())));
        notificationService.createAsync(req);
    }
}
