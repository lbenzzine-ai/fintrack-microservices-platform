package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

@Component
public final class BillPaymentFeeStrategy implements FeeStrategy {

    public static final String NAME = "bill-payment";

    private final BigDecimal flatFee;

    public BillPaymentFeeStrategy(
            @Value("${fintrack.transaction.fees.bill-payment.flat:1.00}") BigDecimal flatFee) {
        this.flatFee = flatFee;
    }

    @Override public String name() { return NAME; }

    @Override
    public Set<TransactionType> supports() { return Set.of(TransactionType.BILL_PAYMENT); }

    @Override
    public BigDecimal calculate(FeeCalculationContext ctx) { return flatFee; }
}
