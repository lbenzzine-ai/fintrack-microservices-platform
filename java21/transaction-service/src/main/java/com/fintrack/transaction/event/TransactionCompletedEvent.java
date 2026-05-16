package com.fintrack.transaction.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
