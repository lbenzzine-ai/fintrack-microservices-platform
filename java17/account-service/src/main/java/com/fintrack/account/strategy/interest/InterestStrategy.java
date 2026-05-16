package com.fintrack.account.strategy.interest;

import java.math.BigDecimal;

/**
 * Strategy contract for interest computation. Implementations are picked at runtime by
 * {@link InterestStrategyRegistry} based on the {@code fintrack.account.interest.strategy}
 * property — this replaces the legacy {@code switch (strategy) { case "flat": ... }} dispatch.
 *
 * <p>Java 17 — plain interface. The Java 21 stack uses a {@code sealed interface}
 * with explicit {@code permits} for {@link FlatInterestStrategy}, {@link TieredInterestStrategy}
 * and {@link CompoundInterestStrategy}.
 */
public interface InterestStrategy {

    /** Strategy identifier — must match a value of {@code fintrack.account.interest.strategy}. */
    String name();

    /**
     * Compute interest for the given principal.
     *
     * @param principal     current account balance
     * @param annualRate    base rate (0.025 = 2.5% APR) — strategies are free to ignore
     * @param months        accrual period in months
     */
    BigDecimal compute(BigDecimal principal, BigDecimal annualRate, int months);
}
