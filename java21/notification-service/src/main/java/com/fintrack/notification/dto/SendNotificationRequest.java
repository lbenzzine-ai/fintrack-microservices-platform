package com.fintrack.notification.dto;

import com.fintrack.notification.entity.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record SendNotificationRequest(
        @NotBlank @Size(max = 255) String recipient,
        @NotNull NotificationChannel channel,
        @NotBlank @Size(max = 64) String template,
        @Size(max = 255) String subject,
        Map<String, Object> payload,
        @Size(min = 36, max = 36) String accountUuid,
        @Size(min = 36, max = 36) String transactionUuid
) {}
