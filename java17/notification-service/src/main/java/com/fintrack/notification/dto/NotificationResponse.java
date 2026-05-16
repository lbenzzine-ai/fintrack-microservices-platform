package com.fintrack.notification.dto;

import com.fintrack.notification.entity.NotificationChannel;
import com.fintrack.notification.entity.NotificationStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
public class NotificationResponse implements Serializable {
    private String uuid;
    private String accountUuid;
    private String transactionUuid;
    private String recipient;
    private NotificationChannel channel;
    private String template;
    private String subject;
    private NotificationStatus status;
    private String failureReason;
    private String deliveryProvider;
    private Instant sentAt;
    private Instant createdAt;
}
