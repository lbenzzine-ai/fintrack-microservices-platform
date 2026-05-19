package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Java 21 — FeeStrategy is a sealed interface, so the registry can only see the five
 * permitted impls. The "duplicate type" and "missing type" failure modes are checked in the
 * java17 stack (where anonymous strategies are possible); here we cover the happy paths.
 */
class FeeStrategyRegistryTest {

    private static List<FeeStrategy> allRealStrategies() {
        return List.of(
                new DomesticFeeStrategy(new BigDecimal("0.005"), new BigDecimal("0.50")),
                new InternationalFeeStrategy(new BigDecimal("0.015"), new BigDecimal("5.00")),
                new ATMWithdrawalFeeStrategy(new BigDecimal("2.00"), new BigDecimal("2.0")),
                new BillPaymentFeeStrategy(new BigDecimal("1.00")),
                new ZeroFeeStrategy()
        );
    }

    @Test
    void strategyFor_returnsRegisteredImplPerTransactionType() {
        FeeStrategyRegistry registry = new FeeStrategyRegistry(allRealStrategies());
        registry.verify();
        assertThat(registry.strategyFor(TransactionType.DOMESTIC_TRANSFER).name()).isEqualTo("domestic");
        assertThat(registry.strategyFor(TransactionType.INTERNATIONAL_TRANSFER).name()).isEqualTo("international");
        assertThat(registry.strategyFor(TransactionType.ATM_WITHDRAWAL).name()).isEqualTo("atm");
        assertThat(registry.strategyFor(TransactionType.BILL_PAYMENT).name()).isEqualTo("bill-payment");
        assertThat(registry.strategyFor(TransactionType.INTERNAL_TRANSFER).name()).isEqualTo("zero");
    }

    @Test
    void byName_returnsRegisteredImpl() {
        FeeStrategyRegistry registry = new FeeStrategyRegistry(allRealStrategies());
        assertThat(registry.by("domestic")).isInstanceOf(DomesticFeeStrategy.class);
        assertThat(registry.by("international")).isInstanceOf(InternationalFeeStrategy.class);
        assertThat(registry.by("zero")).isInstanceOf(ZeroFeeStrategy.class);
    }

    @Test
    void byName_unknownThrows() {
        FeeStrategyRegistry registry = new FeeStrategyRegistry(allRealStrategies());
        assertThatThrownBy(() -> registry.by("never-registered"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("never-registered");
    }
}
