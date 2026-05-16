package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/** No-op — covers internal transfers within the same user's accounts. */
@Component
public class ZeroFeeStrategy implements FeeStrategy {

    public static final String NAME = "zero";

    @Override public String name() { return NAME; }

    @Override
    public Set<TransactionType> supports() { return Set.of(TransactionType.INTERNAL_TRANSFER); }

    @Override
    public BigDecimal calculate(FeeCalculationContext ctx) { return BigDecimal.ZERO; }
}
