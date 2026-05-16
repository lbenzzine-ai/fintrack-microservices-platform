package com.fintrack.account.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record BalanceResponse(
        String accountUuid,
        BigDecimal balance,
        String currencyCode
) implements Serializable {}
