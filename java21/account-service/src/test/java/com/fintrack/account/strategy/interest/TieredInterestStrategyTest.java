package com.fintrack.account.strategy.interest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TieredInterestStrategyTest {

    private TieredInterestStrategy strategy;

    private static TieredInterestStrategy.Tier tier(String min, String max, String rate) {
        TieredInterestStrategy.Tier t = new TieredInterestStrategy.Tier();
        t.setMin(new BigDecimal(min));
        t.setMax(new BigDecimal(max));
        t.setRate(new BigDecimal(rate));
        return t;
    }

    private static BigDecimal expectedAt(BigDecimal rate, BigDecimal principal, int months) {
        BigDecimal monthly = rate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN);
        return principal.multiply(monthly).multiply(BigDecimal.valueOf(months))
                .setScale(4, RoundingMode.HALF_EVEN);
    }

    @BeforeEach
    void setUp() {
        strategy = new TieredInterestStrategy();
        strategy.setTiered(List.of(
                tier("0",      "1000",   "0.005"),
                tier("1000",   "10000",  "0.015"),
                tier("10000",  "100000", "0.025"),
                tier("100000", "9.9E18", "0.04")
        ));
    }

    @Test
    void name_is_tiered() {
        assertThat(strategy.name()).isEqualTo("tiered");
        assertThat(TieredInterestStrategy.NAME).isEqualTo("tiered");
    }

    @ParameterizedTest
    @CsvSource({
            "500,        0.005",
            "999.99,     0.005",
            "1000,       0.015",
            "5000,       0.015",
            "9999.99,    0.015",
            "10000,      0.025",
            "50000,      0.025",
            "99999.99,   0.025",
            "100000,     0.04",
            "5000000,    0.04",
    })
    void each_principal_uses_correct_tier(String principal, String expectedRate) {
        BigDecimal p = new BigDecimal(principal);
        BigDecimal out = strategy.compute(p, new BigDecimal("999"), 12);
        assertThat(out).isEqualByComparingTo(expectedAt(new BigDecimal(expectedRate), p, 12));
        assertThat(out.scale()).isEqualTo(4);
    }

    @Test
    void tier_boundary_min_is_inclusive() {
        BigDecimal out = strategy.compute(new BigDecimal("1000"), BigDecimal.ZERO, 12);
        assertThat(out).isEqualByComparingTo(expectedAt(new BigDecimal("0.015"), new BigDecimal("1000"), 12));
    }

    @Test
    void principal_below_lowest_tier_returns_zero() {
        TieredInterestStrategy s = new TieredInterestStrategy();
        s.setTiered(List.of(tier("100", "1000", "0.01")));
        BigDecimal out = s.compute(new BigDecimal("50"), BigDecimal.ZERO, 12);
        assertThat(out).isEqualByComparingTo("0");
    }

    @Test
    void empty_tiers_returns_zero() {
        TieredInterestStrategy s = new TieredInterestStrategy();
        assertThat(s.getTiered()).isEmpty();
        BigDecimal out = s.compute(new BigDecimal("1000"), BigDecimal.ZERO, 12);
        assertThat(out).isEqualByComparingTo("0");
    }

    @Test
    void getters_setters_roundtrip() {
        TieredInterestStrategy s = new TieredInterestStrategy();
        List<TieredInterestStrategy.Tier> tiers = List.of(tier("1", "2", "0.1"));
        s.setTiered(tiers);
        assertThat(s.getTiered()).isSameAs(tiers);

        TieredInterestStrategy.Tier t = tier("3", "4", "0.05");
        assertThat(t.getMin()).isEqualByComparingTo("3");
        assertThat(t.getMax()).isEqualByComparingTo("4");
        assertThat(t.getRate()).isEqualByComparingTo("0.05");
    }

    @Test
    void zero_months_returns_zero() {
        BigDecimal out = strategy.compute(new BigDecimal("5000"), BigDecimal.ZERO, 0);
        assertThat(out).isEqualByComparingTo("0");
    }
}
