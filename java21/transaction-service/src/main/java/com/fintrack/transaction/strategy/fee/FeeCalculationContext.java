package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;

import java.math.BigDecimal;

/**
 * Inputs to a {@link FeeStrategy}. Record-shaped so it's trivially shareable across threads
 * and can be used as a Jackson/Cache key without surprises.
 */
public record FeeCalculationContext(
        TransactionType type,
        BigDecimal amount,
        String currencyCode,
        String fromAccountUuid,
        String toAccountUuid,
        boolean crossBorder,
        boolean weekend
) {
    public FeeCalculationContext withWeekend(boolean weekend) {
        return new FeeCalculationContext(type, amount, currencyCode, fromAccountUuid, toAccountUuid, crossBorder, weekend);
    }
}
