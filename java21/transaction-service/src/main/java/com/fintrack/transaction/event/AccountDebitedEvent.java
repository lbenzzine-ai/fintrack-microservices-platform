package com.fintrack.transaction.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountDebitedEvent(
        String eventId,
        String transactionUuid,
        String accountUuid,
        BigDecimal amount,
        BigDecimal fee,
        BigDecimal newBalance,
        String currencyCode,
        Instant occurredAt
) implements Serializable {}
