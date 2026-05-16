package com.fintrack.transaction.dto;

import com.fintrack.transaction.entity.TransactionStatus;
import com.fintrack.transaction.entity.TransactionType;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String uuid,
        String fromAccountUuid,
        String toAccountUuid,
        BigDecimal amount,
        BigDecimal fee,
        String currencyCode,
        TransactionType type,
        TransactionStatus status,
        String description,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {}
