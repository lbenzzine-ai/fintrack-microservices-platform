package com.fintrack.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionFailedEvent(
        String eventId,
        String transactionUuid,
        String fromAccountUuid,
        BigDecimal amount,
        BigDecimal fee,
        String currencyCode,
        String reasonCode,
        String reason,
        boolean alreadyDebited,
        Instant occurredAt
) implements Serializable {}
