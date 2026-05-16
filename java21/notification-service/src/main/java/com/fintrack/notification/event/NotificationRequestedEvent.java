package com.fintrack.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationRequestedEvent(
        String eventId,
        String transactionUuid,
        String accountUuid,
        String channel,
        String template,
        String subject,
        Map<String, Object> payload,
        Instant occurredAt
) implements Serializable {}
