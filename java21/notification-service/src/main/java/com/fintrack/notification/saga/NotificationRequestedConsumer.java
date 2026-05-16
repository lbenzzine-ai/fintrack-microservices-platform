package com.fintrack.notification.saga;

import com.fintrack.notification.dto.SendNotificationRequest;
import com.fintrack.notification.entity.NotificationChannel;
import com.fintrack.notification.event.NotificationRequestedEvent;
import com.fintrack.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRequestedConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
        topics = "${fintrack.messaging.kafka.topics.notification-requested}",
        groupId = "${spring.application.name}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onNotificationRequested(@Payload NotificationRequestedEvent event) {
        log.info("[NOTIFY] notification-requested account={} channel={} template={}",
                event.accountUuid(), event.channel(), event.template());
        notificationService.createAsync(new SendNotificationRequest(
                event.accountUuid() + "@example.local",
                NotificationChannel.valueOf(event.channel()),
                event.template(),
                event.subject(),
                event.payload(),
                event.accountUuid(),
                event.transactionUuid()));
    }
}
