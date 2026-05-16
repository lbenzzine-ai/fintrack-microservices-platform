package com.fintrack.account.dto;

import com.fintrack.account.entity.AccountStatus;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        String uuid,
        String userUuid,
        BigDecimal balance,
        String currencyCode,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {}
