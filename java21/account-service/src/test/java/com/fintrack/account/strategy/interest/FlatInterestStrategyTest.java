package com.fintrack.account.strategy.interest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

class FlatInterestStrategyTest {

    /** Reference impl mirroring production formula so we don't hand-code rounding. */
    private static BigDecimal expected(BigDecimal flatRate, BigDecimal principal, int months) {
        BigDecimal monthly = flatRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN);
        return principal.multiply(monthly).multiply(BigDecimal.valueOf(months))
                .setScale(4, RoundingMode.HALF_EVEN);
    }

    @Test
    void name_is_flat() {
        FlatInterestStrategy s = new FlatInterestStrategy(new BigDecimal("0.01"));
        assertThat(s.name()).isEqualTo("flat");
        assertThat(FlatInterestStrategy.NAME).isEqualTo("flat");
    }

    @ParameterizedTest
    @CsvSource({
            "0.01,    1000,   12",
            "0.02,    5000,    6",
            "0.05,  10000,    1",
            "0.10,  12345.67, 9",
            "0.00,   1000,   12",
    })
    void compute_matches_formula(String flatRate, String principal, int months) {
        BigDecimal rate = new BigDecimal(flatRate);
        BigDecimal p = new BigDecimal(principal);

        FlatInterestStrategy strategy = new FlatInterestStrategy(rate);
        BigDecimal result = strategy.compute(p, new BigDecimal("999"), months);

        assertThat(result).isEqualByComparingTo(expected(rate, p, months));
        assertThat(result.scale()).isEqualTo(4);
    }

    @Test
    void zero_principal_returns_zero_with_scale_4() {
        FlatInterestStrategy s = new FlatInterestStrategy(new BigDecimal("0.05"));
        BigDecimal out = s.compute(BigDecimal.ZERO, new BigDecimal("0.05"), 12);
        assertThat(out).isEqualByComparingTo("0");
        assertThat(out.scale()).isEqualTo(4);
    }

    @Test
    void zero_months_returns_zero() {
        FlatInterestStrategy s = new FlatInterestStrategy(new BigDecimal("0.05"));
        BigDecimal out = s.compute(new BigDecimal("1000"), new BigDecimal("0.05"), 0);
        assertThat(out).isEqualByComparingTo("0");
    }

    @Test
    void annual_rate_argument_is_ignored() {
        FlatInterestStrategy s = new FlatInterestStrategy(new BigDecimal("0.10"));
        BigDecimal a = s.compute(new BigDecimal("1000"), new BigDecimal("0.01"), 12);
        BigDecimal b = s.compute(new BigDecimal("1000"), new BigDecimal("0.99"), 12);
        assertThat(a).isEqualByComparingTo(b);
    }
}
