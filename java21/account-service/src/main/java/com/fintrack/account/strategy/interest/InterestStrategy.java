package com.fintrack.account.strategy.interest;

import java.math.BigDecimal;

/**
 * Java 21 — {@code sealed interface} locking the interest Strategy hierarchy to three
 * named impls so the compiler can enforce exhaustive {@code switch} over the type.
 */
public sealed interface InterestStrategy
        permits FlatInterestStrategy, TieredInterestStrategy, CompoundInterestStrategy {

    String name();

    BigDecimal compute(BigDecimal principal, BigDecimal annualRate, int months);
}
