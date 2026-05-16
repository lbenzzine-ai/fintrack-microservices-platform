package com.fintrack.transaction.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
