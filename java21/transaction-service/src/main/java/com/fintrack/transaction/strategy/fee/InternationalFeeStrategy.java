package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Component
public final class InternationalFeeStrategy implements FeeStrategy {

    public static final String NAME = "international";

    private final BigDecimal rate;
    private final BigDecimal surcharge;

    public InternationalFeeStrategy(
            @Value("${fintrack.transaction.fees.international.rate:0.015}") BigDecimal rate,
            @Value("${fintrack.transaction.fees.international.surcharge:5.00}") BigDecimal surcharge) {
        this.rate = rate;
        this.surcharge = surcharge;
    }

    @Override public String name() { return NAME; }

    @Override
    public Set<TransactionType> supports() { return Set.of(TransactionType.INTERNATIONAL_TRANSFER); }

    @Override
    public BigDecimal calculate(FeeCalculationContext ctx) {
        BigDecimal percent = ctx.amount().multiply(rate).setScale(4, RoundingMode.HALF_EVEN);
        return percent.add(surcharge);
    }
}
