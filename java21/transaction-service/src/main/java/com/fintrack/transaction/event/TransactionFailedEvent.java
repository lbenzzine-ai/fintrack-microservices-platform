package com.fintrack.transaction.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionFailedEvent(
        String eventId,
        String transactionUuid,
        String fromAccountUuid,
        String toAccountUuid,
        BigDecimal amount,
        BigDecimal fee,
        String currencyCode,
        String reasonCode,
        String reason,
        boolean alreadyDebited,
        Instant occurredAt
) implements Serializable {}
