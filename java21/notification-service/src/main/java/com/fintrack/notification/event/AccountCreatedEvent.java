package com.fintrack.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountCreatedEvent(
        String eventId,
        String accountUuid,
        String userUuid,
        BigDecimal openingBalance,
        String currencyCode,
        Instant occurredAt
) implements Serializable {}
