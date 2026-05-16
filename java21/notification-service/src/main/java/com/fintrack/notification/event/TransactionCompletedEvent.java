package com.fintrack.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionCompletedEvent(
        String eventId,
        String transactionUuid,
        String fromAccountUuid,
        String toAccountUuid,
        BigDecimal amount,
        BigDecimal fee,
        String currencyCode,
        String type,
        Instant occurredAt
) implements Serializable {}
