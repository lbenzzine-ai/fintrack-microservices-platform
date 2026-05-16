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
        log.info("[NOTIFY] user-registered user={} email={}", event.userUuid(), event.email());
        notificationService.createAsync(new SendNotificationRequest(
                event.email(),
                NotificationChannel.EMAIL,
                "welcome",
                "Welcome to FinTrack",
                Map.of("username", event.username()),
                null,
                null));
    }
}
