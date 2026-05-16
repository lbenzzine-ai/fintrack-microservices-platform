package com.fintrack.notification.saga;

import com.fintrack.notification.dto.SendNotificationRequest;
import com.fintrack.notification.entity.NotificationChannel;
import com.fintrack.notification.event.AccountCreatedEvent;
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
public class AccountCreatedConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "${fintrack.messaging.kafka.topics.account-created}",
        groupId = "${spring.application.name}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAccountCreated(@Payload AccountCreatedEvent event) {
        log.info("[NOTIFY] account-created account={} user={}", event.getAccountUuid(), event.getUserUuid());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipient(event.getUserUuid() + "@example.local");
        req.setChannel(NotificationChannel.EMAIL);
        req.setTemplate("account-created");
        req.setSubject("Your FinTrack wallet is ready");
        req.setAccountUuid(event.getAccountUuid());
        req.setPayload(Map.of(
                "openingBalance", event.getOpeningBalance(),
                "currency", event.getCurrencyCode()));
        notificationService.createAsync(req);
    }
}
