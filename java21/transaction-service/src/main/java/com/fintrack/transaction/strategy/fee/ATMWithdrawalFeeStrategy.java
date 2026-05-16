package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
public final class ATMWithdrawalFeeStrategy implements FeeStrategy {

    public static final String NAME = "atm";

    private final BigDecimal baseFee;
    private final BigDecimal weekendMultiplier;

    public ATMWithdrawalFeeStrategy(
            @Value("${fintrack.transaction.fees.atm.base:2.00}") BigDecimal baseFee,
            @Value("${fintrack.transaction.fees.atm.weekend-multiplier:2.0}") BigDecimal weekendMultiplier) {
        this.baseFee = baseFee;
        this.weekendMultiplier = weekendMultiplier;
    }

    @Override public String name() { return NAME; }

    @Override
    public Set<TransactionType> supports() { return Set.of(TransactionType.ATM_WITHDRAWAL); }

    @Override
    public BigDecimal calculate(FeeCalculationContext ctx) {
        return ctx.weekend() ? baseFee.multiply(weekendMultiplier) : baseFee;
    }
}
