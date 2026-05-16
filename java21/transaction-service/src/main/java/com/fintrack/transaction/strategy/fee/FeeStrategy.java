package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Java 21 — {@code sealed interface} so the compiler can enforce that every
 * {@link TransactionType} has a Strategy and that no out-of-tree implementation slips in.
 */
public sealed interface FeeStrategy
        permits DomesticFeeStrategy,
                InternationalFeeStrategy,
                ATMWithdrawalFeeStrategy,
                BillPaymentFeeStrategy,
                ZeroFeeStrategy {

    String name();

    Set<TransactionType> supports();

    BigDecimal calculate(FeeCalculationContext ctx);
}
