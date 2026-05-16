package com.fintrack.notification.saga;

import com.fintrack.notification.dto.SendNotificationRequest;
import com.fintrack.notification.entity.NotificationChannel;
import com.fintrack.notification.event.UserRegisteredEvent;
import com.fintrack.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Welcome email — also seeds a default preference row with email opted-in. */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "${fintrack.messaging.kafka.topics.user-registered}",
        groupId = "${spring.application.name}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onUserRegistered(@Payload UserRegisteredEvent event) {
        log.info("[NOTIFY] user-registered user={} email={}", event.getUserUuid(), event.getEmail());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipient(event.getEmail());
        req.setChannel(NotificationChannel.EMAIL);
        req.setTemplate("welcome");
        req.setSubject("Welcome to FinTrack");
        req.setPayload(Map.of("username", event.getUsername()));
        notificationService.createAsync(req);
    }
}
