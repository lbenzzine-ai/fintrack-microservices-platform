package com.fintrack.account.strategy.interest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterestStrategyRegistryTest {

    private static FlatInterestStrategy flat() { return new FlatInterestStrategy(new BigDecimal("0.01")); }
    private static TieredInterestStrategy tiered() { return new TieredInterestStrategy(); }
    private static CompoundInterestStrategy compound() { return new CompoundInterestStrategy(); }

    @Test
    void active_returns_default_strategy() {
        FlatInterestStrategy flat = flat();
        TieredInterestStrategy tiered = tiered();
        InterestStrategyRegistry r = new InterestStrategyRegistry(List.of(flat, tiered), "tiered");
        r.verify();
        assertThat(r.active()).isSameAs(tiered);
    }

    @Test
    void by_returns_named_strategy() {
        FlatInterestStrategy flat = flat();
        CompoundInterestStrategy compound = compound();
        InterestStrategyRegistry r = new InterestStrategyRegistry(List.of(flat, compound), "compound");
        assertThat(r.by("flat")).isSameAs(flat);
        assertThat(r.by("compound")).isSameAs(compound);
    }

    @Test
    void by_name_is_case_insensitive() {
        FlatInterestStrategy flat = flat();
        InterestStrategyRegistry r = new InterestStrategyRegistry(List.of(flat), "flat");
        assertThat(r.by("FLAT")).isSameAs(flat);
        assertThat(r.by("Flat")).isSameAs(flat);
    }

    @Test
    void by_unknown_throws_illegal_argument() {
        InterestStrategyRegistry r = new InterestStrategyRegistry(List.of(flat()), "flat");
        assertThatThrownBy(() -> r.by("does-not-exist"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does-not-exist");
    }

    @Test
    void constructor_unknown_default_name_throws() {
        assertThatThrownBy(() -> new InterestStrategyRegistry(List.of(flat()), "bogus"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bogus");
    }

    @Test
    void verify_throws_when_default_kind_not_registered() {
        // Default "compound" valid name, but no CompoundInterestStrategy bean present.
        InterestStrategyRegistry r = new InterestStrategyRegistry(List.of(flat()), "compound");
        assertThatThrownBy(r::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("COMPOUND");
    }

    @Test
    void by_returns_null_when_kind_resolves_but_no_bean_registered() {
        // "compound" resolves to Kind.COMPOUND, but no CompoundInterestStrategy bean registered.
        InterestStrategyRegistry r = new InterestStrategyRegistry(List.of(flat()), "flat");
        assertThat(r.by("compound")).isNull();
    }
}
