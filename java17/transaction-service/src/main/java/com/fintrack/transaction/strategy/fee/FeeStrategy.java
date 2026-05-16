package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Strategy contract for transaction fees. Implementations advertise which {@link TransactionType}s
 * they handle via {@link #supports()}; {@code FeeService} selects the first matching impl.
 *
 * <p>Java 17 — plain interface. The Java 21 stack uses a {@code sealed interface}.
 */
public interface FeeStrategy {

    String name();

    Set<TransactionType> supports();

    BigDecimal calculate(FeeCalculationContext ctx);
}
