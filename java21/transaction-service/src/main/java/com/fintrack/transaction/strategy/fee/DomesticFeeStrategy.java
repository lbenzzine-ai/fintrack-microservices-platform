package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Component
public final class DomesticFeeStrategy implements FeeStrategy {

    public static final String NAME = "domestic";

    private final BigDecimal rate;
    private final BigDecimal floor;

    public DomesticFeeStrategy(
            @Value("${fintrack.transaction.fees.domestic.rate:0.005}") BigDecimal rate,
            @Value("${fintrack.transaction.fees.domestic.floor:0.50}") BigDecimal floor) {
        this.rate = rate;
        this.floor = floor;
    }

    @Override public String name() { return NAME; }

    @Override
    public Set<TransactionType> supports() { return Set.of(TransactionType.DOMESTIC_TRANSFER); }

    @Override
    public BigDecimal calculate(FeeCalculationContext ctx) {
        BigDecimal computed = ctx.amount().multiply(rate).setScale(4, RoundingMode.HALF_EVEN);
        return computed.max(floor);
    }
}
