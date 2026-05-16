package com.fintrack.account.strategy.interest;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class CompoundInterestStrategy implements InterestStrategy {

    public static final String NAME = "compound";
    private static final MathContext MC = MathContext.DECIMAL64;

    @Override
    public String name() { return NAME; }

    @Override
    public BigDecimal compute(BigDecimal principal, BigDecimal annualRate, int months) {
        // A = P (1 + r/n)^(n·t) with monthly compounding (n=12, t=months/12)
        BigDecimal n = BigDecimal.valueOf(12);
        BigDecimal ratePerPeriod = annualRate.divide(n, MC);
        BigDecimal base = BigDecimal.ONE.add(ratePerPeriod);
        BigDecimal factor = base.pow(months, MC);
        BigDecimal accrued = principal.multiply(factor, MC).subtract(principal);
        return accrued.setScale(4, RoundingMode.HALF_EVEN);
    }
}
