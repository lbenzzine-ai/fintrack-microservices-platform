package com.fintrack.notification.dto;

import com.fintrack.notification.entity.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class SendNotificationRequest {

    @NotBlank @Size(max = 255)
    private String recipient;

    @NotNull
    private NotificationChannel channel;

    @NotBlank @Size(max = 64)
    private String template;

    @Size(max = 255)
    private String subject;

    private Map<String, Object> payload;

    @Size(min = 36, max = 36)
    private String accountUuid;

    @Size(min = 36, max = 36)
    private String transactionUuid;
}
