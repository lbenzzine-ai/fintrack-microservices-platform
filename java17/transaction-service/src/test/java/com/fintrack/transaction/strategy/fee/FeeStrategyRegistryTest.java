package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeeStrategyRegistryTest {

    private static FeeStrategy fakeStrategy(String name, Set<TransactionType> types) {
        return new FeeStrategy() {
            @Override public String name() { return name; }
            @Override public Set<TransactionType> supports() { return types; }
            @Override public BigDecimal calculate(FeeCalculationContext ctx) { return BigDecimal.ONE; }
        };
    }

    private static List<FeeStrategy> allTypesCovered() {
        return List.of(
                fakeStrategy("domestic", Set.of(TransactionType.DOMESTIC_TRANSFER)),
                fakeStrategy("international", Set.of(TransactionType.INTERNATIONAL_TRANSFER)),
                fakeStrategy("atm", Set.of(TransactionType.ATM_WITHDRAWAL)),
                fakeStrategy("bill", Set.of(TransactionType.BILL_PAYMENT)),
                fakeStrategy("zero", Set.of(TransactionType.INTERNAL_TRANSFER))
        );
    }

    @Test
    void strategyFor_returnsRegisteredImpl() {
        FeeStrategyRegistry registry = new FeeStrategyRegistry(allTypesCovered());
        registry.verify();
        assertThat(registry.strategyFor(TransactionType.DOMESTIC_TRANSFER).name()).isEqualTo("domestic");
        assertThat(registry.strategyFor(TransactionType.INTERNATIONAL_TRANSFER).name()).isEqualTo("international");
        assertThat(registry.strategyFor(TransactionType.ATM_WITHDRAWAL).name()).isEqualTo("atm");
        assertThat(registry.strategyFor(TransactionType.BILL_PAYMENT).name()).isEqualTo("bill");
        assertThat(registry.strategyFor(TransactionType.INTERNAL_TRANSFER).name()).isEqualTo("zero");
    }

    @Test
    void byName_returnsRegisteredImpl() {
        FeeStrategyRegistry registry = new FeeStrategyRegistry(allTypesCovered());
        assertThat(registry.by("domestic").name()).isEqualTo("domestic");
    }

    @Test
    void byName_unknownThrows() {
        FeeStrategyRegistry registry = new FeeStrategyRegistry(allTypesCovered());
        assertThatThrownBy(() -> registry.by("never-registered"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("never-registered");
    }

    @Test
    void constructor_throwsOnDuplicateTypeRegistration() {
        List<FeeStrategy> dup = List.of(
                fakeStrategy("a", Set.of(TransactionType.DOMESTIC_TRANSFER)),
                fakeStrategy("b", Set.of(TransactionType.DOMESTIC_TRANSFER))
        );
        assertThatThrownBy(() -> new FeeStrategyRegistry(dup))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate FeeStrategy");
    }

    @Test
    void verify_throwsWhenAnyTypeMissing() {
        // omit ATM_WITHDRAWAL
        List<FeeStrategy> incomplete = List.of(
                fakeStrategy("domestic", Set.of(TransactionType.DOMESTIC_TRANSFER)),
                fakeStrategy("international", Set.of(TransactionType.INTERNATIONAL_TRANSFER)),
                fakeStrategy("bill", Set.of(TransactionType.BILL_PAYMENT)),
                fakeStrategy("zero", Set.of(TransactionType.INTERNAL_TRANSFER))
        );
        FeeStrategyRegistry registry = new FeeStrategyRegistry(incomplete);
        assertThatThrownBy(registry::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ATM_WITHDRAWAL");
    }
}
