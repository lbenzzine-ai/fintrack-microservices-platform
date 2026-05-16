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

/**
 * Generic on-demand notification — services other than transaction-service can publish onto
 * {@code fintrack.notification.requested} to fan out a notification without inventing a new topic.
 */
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
                event.getAccountUuid(), event.getChannel(), event.getTemplate());
        SendNotificationRequest req = new SendNotificationRequest();
        req.setRecipient(event.getAccountUuid() + "@example.local");
        req.setChannel(NotificationChannel.valueOf(event.getChannel()));
        req.setTemplate(event.getTemplate());
        req.setSubject(event.getSubject());
        req.setAccountUuid(event.getAccountUuid());
        req.setTransactionUuid(event.getTransactionUuid());
        req.setPayload(event.getPayload());
        notificationService.createAsync(req);
    }
}
