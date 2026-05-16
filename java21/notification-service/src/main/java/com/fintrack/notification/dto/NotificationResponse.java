package com.fintrack.notification.dto;

import com.fintrack.notification.entity.NotificationChannel;
import com.fintrack.notification.entity.NotificationStatus;

import java.io.Serializable;
import java.time.Instant;

public record NotificationResponse(
        String uuid,
        String accountUuid,
        String transactionUuid,
        String recipient,
        NotificationChannel channel,
        String template,
        String subject,
        NotificationStatus status,
        String failureReason,
        String deliveryProvider,
        Instant sentAt,
        Instant createdAt
) implements Serializable {}
