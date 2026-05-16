package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Inputs to a {@link FeeStrategy}. Kept open (not all strategies use every field) so adding a new
 * dimension (e.g. risk score, channel) doesn't ripple through callers.
 */
@Data
@Builder
public class FeeCalculationContext {
    private TransactionType type;
    private BigDecimal amount;
    private String currencyCode;
    private String fromAccountUuid;
    private String toAccountUuid;
    private boolean crossBorder;
    private boolean weekend;
}
