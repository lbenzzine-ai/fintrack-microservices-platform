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
        log.warn("[NOTIFY] tx-failed tx={} reason={}", event.transactionUuid(), event.reasonCode());
        notificationService.createAsync(new SendNotificationRequest(
                event.fromAccountUuid() + "@example.local",
                NotificationChannel.EMAIL,
                "transaction-failed",
                "We couldn't complete your transaction",
                Map.of(
                        "amount", event.amount(),
                        "currency", event.currencyCode(),
                        "reasonCode", event.reasonCode(),
                        "reason", String.valueOf(event.reason())),
                event.fromAccountUuid(),
                event.transactionUuid()));
    }
}
