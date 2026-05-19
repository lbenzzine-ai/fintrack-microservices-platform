package com.fintrack.account.strategy.interest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InterestStrategyRegistryTest {

    private static InterestStrategy stub(String name) {
        InterestStrategy s = mock(InterestStrategy.class);
        when(s.name()).thenReturn(name);
        when(s.compute(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(BigDecimal.TEN);
        return s;
    }

    @Test
    void active_returns_default_strategy() {
        InterestStrategy flat = stub("flat");
        InterestStrategy tiered = stub("tiered");
        InterestStrategyRegistry r = new InterestStrategyRegistry(List.of(flat, tiered), "tiered");
        r.verify();
        assertThat(r.active()).isSameAs(tiered);
    }

    @Test
    void by_returns_named_strategy() {
        InterestStrategy flat = stub("flat");
        InterestStrategy compound = stub("compound");
        InterestStrategyRegistry r = new InterestStrategyRegistry(List.of(flat, compound), "compound");
        assertThat(r.by("flat")).isSameAs(flat);
        assertThat(r.by("compound")).isSameAs(compound);
    }

    @Test
    void by_unknown_throws_illegal_argument() {
        InterestStrategy flat = stub("flat");
        InterestStrategyRegistry r = new InterestStrategyRegistry(List.of(flat), "flat");
        assertThatThrownBy(() -> r.by("does-not-exist"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does-not-exist");
    }

    @Test
    void verify_throws_when_default_missing() {
        InterestStrategy flat = stub("flat");
        InterestStrategyRegistry r = new InterestStrategyRegistry(List.of(flat), "compound");
        assertThatThrownBy(r::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("compound");
    }
}
