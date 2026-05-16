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
        log.info("[NOTIFY] tx-completed tx={} amount={}", event.transactionUuid(), event.amount());
        notificationService.createAsync(new SendNotificationRequest(
                event.fromAccountUuid() + "@example.local",
                NotificationChannel.EMAIL,
                "transaction-completed",
                "Transaction completed",
                Map.of(
                        "amount", event.amount(),
                        "fee", event.fee(),
                        "currency", event.currencyCode(),
                        "type", event.type()),
                event.fromAccountUuid(),
                event.transactionUuid()));
    }
}
