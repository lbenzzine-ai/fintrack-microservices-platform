package com.fintrack.account.strategy.interest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class CompoundInterestStrategyTest {

    private static final MathContext MC = MathContext.DECIMAL64;

    /** Reference impl: A = P(1+r/n)^(n*t) with n=12. */
    private static BigDecimal expected(BigDecimal principal, BigDecimal annualRate, int months) {
        BigDecimal n = BigDecimal.valueOf(12);
        BigDecimal ratePerPeriod = annualRate.divide(n, MC);
        BigDecimal base = BigDecimal.ONE.add(ratePerPeriod);
        BigDecimal factor = base.pow(months, MC);
        BigDecimal accrued = principal.multiply(factor, MC).subtract(principal);
        return accrued.setScale(4, RoundingMode.HALF_EVEN);
    }

    @Test
    void name_is_compound() {
        assertThat(new CompoundInterestStrategy().name()).isEqualTo("compound");
        assertThat(CompoundInterestStrategy.NAME).isEqualTo("compound");
    }

    @ParameterizedTest
    @CsvSource({
            // principal, annualRate, months
            "1000,    0.05,  12",
            "1000,    0.10,  24",
            "5000,    0.025, 6",
            "100,     0.01,  1",
            "10000,   0.075, 36",
    })
    void compute_matches_compound_formula(String principal, String annualRate, int months) {
        BigDecimal p = new BigDecimal(principal);
        BigDecimal r = new BigDecimal(annualRate);
        BigDecimal out = new CompoundInterestStrategy().compute(p, r, months);
        assertThat(out).isEqualByComparingTo(expected(p, r, months));
        assertThat(out.scale()).isEqualTo(4);
    }

    @Test
    void zero_principal_returns_zero() {
        BigDecimal out = new CompoundInterestStrategy().compute(BigDecimal.ZERO, new BigDecimal("0.05"), 12);
        assertThat(out).isEqualByComparingTo("0");
    }

    @Test
    void zero_rate_returns_zero() {
        BigDecimal out = new CompoundInterestStrategy().compute(new BigDecimal("1000"), BigDecimal.ZERO, 12);
        assertThat(out).isEqualByComparingTo("0");
    }

    @Test
    void zero_months_returns_zero() {
        // (1+r)^0 = 1, so interest is principal - principal = 0.
        BigDecimal out = new CompoundInterestStrategy().compute(new BigDecimal("1000"), new BigDecimal("0.05"), 0);
        assertThat(out).isEqualByComparingTo("0");
    }

    @Test
    void compound_grows_with_more_periods() {
        CompoundInterestStrategy s = new CompoundInterestStrategy();
        BigDecimal twelve = s.compute(new BigDecimal("1000"), new BigDecimal("0.05"), 12);
        BigDecimal twentyFour = s.compute(new BigDecimal("1000"), new BigDecimal("0.05"), 24);
        assertThat(twentyFour).isGreaterThan(twelve);
    }
}
