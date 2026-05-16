package com.fintrack.account.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionInitiatedEvent(
        String eventId,
        String transactionUuid,
        String fromAccountUuid,
        String toAccountUuid,
        BigDecimal amount,
        BigDecimal fee,
        String currencyCode,
        Instant occurredAt
) implements Serializable {}
