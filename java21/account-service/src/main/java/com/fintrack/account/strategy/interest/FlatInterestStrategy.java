package com.fintrack.account.strategy.interest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public final class FlatInterestStrategy implements InterestStrategy {

    public static final String NAME = "flat";

    private final BigDecimal flatRate;

    public FlatInterestStrategy(@Value("${fintrack.account.interest.flat-rate:0.01}") BigDecimal flatRate) {
        this.flatRate = flatRate;
    }

    @Override
    public String name() { return NAME; }

    @Override
    public BigDecimal compute(BigDecimal principal, BigDecimal annualRate, int months) {
        // Single flat percentage applied pro-rata over the period (months/12).
        BigDecimal monthly = flatRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN);
        return principal.multiply(monthly).multiply(BigDecimal.valueOf(months)).setScale(4, RoundingMode.HALF_EVEN);
    }
}
