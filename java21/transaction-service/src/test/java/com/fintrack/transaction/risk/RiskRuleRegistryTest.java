package com.fintrack.transaction.risk;

import com.fintrack.transaction.entity.Transaction;
import com.fintrack.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RiskRuleRegistryTest {

    private final RiskRuleRegistry registry = new RiskRuleRegistry();

    private Map<String, RiskRule> rules() {
        return registry.byName();
    }

    private static Transaction.TransactionBuilder baseTx() {
        return Transaction.builder()
                .uuid("u")
                .fromAccountUuid("A")
                .toAccountUuid("B")
                .amount(new BigDecimal("100"))
                .fee(BigDecimal.ZERO)
                .currencyCode("USD")
                .type(TransactionType.DOMESTIC_TRANSFER)
                .description("food");
    }

    // ── SELF_TRANSFER ────────────────────────────────────────────────────────
    @Test
    void selfTransferRule_firesWhenFromEqualsTo() {
        Transaction t = baseTx().fromAccountUuid("X").toAccountUuid("X").build();
        Optional<RiskFinding> f = rules().get("SELF_TRANSFER").evaluate(t);
        assertThat(f).isPresent();
        assertThat(f.get().getLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(f.get().getRuleName()).isEqualTo("SELF_TRANSFER");
    }

    @Test
    void selfTransferRule_doesNotFireForDifferentWallets() {
        Transaction t = baseTx().fromAccountUuid("X").toAccountUuid("Y").build();
        assertThat(rules().get("SELF_TRANSFER").evaluate(t)).isEmpty();
    }

    @Test
    void selfTransferRule_doesNotFireWhenToIsNull() {
        Transaction t = baseTx().toAccountUuid(null)
                .type(TransactionType.ATM_WITHDRAWAL).build();
        assertThat(rules().get("SELF_TRANSFER").evaluate(t)).isEmpty();
    }

    // ── LARGE_AMOUNT (>= 10000) ──────────────────────────────────────────────
    @ParameterizedTest
    @CsvSource({
            "10000, true",
            "9999.99, false",
            "50000, true",
            "0.01, false"
    })
    void largeAmountRule(BigDecimal amount, boolean expectFire) {
        Transaction t = baseTx().amount(amount).build();
        Optional<RiskFinding> f = rules().get("LARGE_AMOUNT").evaluate(t);
        assertThat(f.isPresent()).isEqualTo(expectFire);
        if (expectFire) assertThat(f.get().getLevel()).isEqualTo(RiskLevel.HIGH);
    }

    // ── STRUCTURING_PATTERN (round multiples of 1000, >= 1000) ───────────────
    @ParameterizedTest
    @CsvSource({
            "1000, true",
            "2000, true",
            "1500, false",
            "999, false",
            "1000.01, false"
    })
    void structuringRule(BigDecimal amount, boolean expectFire) {
        Transaction t = baseTx().amount(amount).build();
        Optional<RiskFinding> f = rules().get("STRUCTURING_PATTERN").evaluate(t);
        assertThat(f.isPresent()).isEqualTo(expectFire);
        if (expectFire) assertThat(f.get().getLevel()).isEqualTo(RiskLevel.MEDIUM);
    }

    // ── HIGH_VALUE_INTERNATIONAL (INTERNATIONAL_TRANSFER >= 5000) ────────────
    @Test
    void highValueIntlRule_firesAtThreshold() {
        Transaction t = baseTx()
                .type(TransactionType.INTERNATIONAL_TRANSFER)
                .amount(new BigDecimal("5000"))
                .build();
        Optional<RiskFinding> f = rules().get("HIGH_VALUE_INTERNATIONAL").evaluate(t);
        assertThat(f).isPresent();
        assertThat(f.get().getLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void highValueIntlRule_doesNotFireForDomestic() {
        Transaction t = baseTx()
                .type(TransactionType.DOMESTIC_TRANSFER)
                .amount(new BigDecimal("999999"))
                .build();
        assertThat(rules().get("HIGH_VALUE_INTERNATIONAL").evaluate(t)).isEmpty();
    }

    @Test
    void highValueIntlRule_doesNotFireBelowThreshold() {
        Transaction t = baseTx()
                .type(TransactionType.INTERNATIONAL_TRANSFER)
                .amount(new BigDecimal("4999.99"))
                .build();
        assertThat(rules().get("HIGH_VALUE_INTERNATIONAL").evaluate(t)).isEmpty();
    }

    // ── ATM_HIGH_VALUE (ATM_WITHDRAWAL > 1000) ───────────────────────────────
    @Test
    void atmHighValueRule_firesAbove1000() {
        Transaction t = baseTx()
                .type(TransactionType.ATM_WITHDRAWAL)
                .amount(new BigDecimal("1500"))
                .toAccountUuid(null)
                .build();
        Optional<RiskFinding> f = rules().get("ATM_HIGH_VALUE").evaluate(t);
        assertThat(f).isPresent();
        assertThat(f.get().getLevel()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    void atmHighValueRule_doesNotFireExactlyAt1000() {
        Transaction t = baseTx()
                .type(TransactionType.ATM_WITHDRAWAL)
                .amount(new BigDecimal("1000"))
                .toAccountUuid(null)
                .build();
        assertThat(rules().get("ATM_HIGH_VALUE").evaluate(t)).isEmpty();
    }

    @Test
    void atmHighValueRule_doesNotFireForNonAtm() {
        Transaction t = baseTx()
                .type(TransactionType.DOMESTIC_TRANSFER)
                .amount(new BigDecimal("5000"))
                .build();
        assertThat(rules().get("ATM_HIGH_VALUE").evaluate(t)).isEmpty();
    }

    // ── MISSING_DESCRIPTION (>= 5000 with blank desc) ────────────────────────
    @ParameterizedTest
    @CsvSource(value = {
            "5000,'',true",
            "5000,'   ',true",
            "5000,'rent',false",
            "4999.99,'',false"
    })
    void missingDescriptionRule(BigDecimal amount, String description, boolean expectFire) {
        Transaction t = baseTx().amount(amount).description(description).build();
        Optional<RiskFinding> f = rules().get("MISSING_DESCRIPTION").evaluate(t);
        assertThat(f.isPresent()).isEqualTo(expectFire);
        if (expectFire) assertThat(f.get().getLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void missingDescriptionRule_firesWhenDescriptionNull() {
        Transaction t = baseTx().amount(new BigDecimal("5000")).description(null).build();
        assertThat(rules().get("MISSING_DESCRIPTION").evaluate(t)).isPresent();
    }

    // ── UNKNOWN_DESTINATION (transfer-type without toAccountUuid) ────────────
    @Test
    void unknownDestinationRule_firesForDomesticWithoutDest() {
        Transaction t = baseTx()
                .type(TransactionType.DOMESTIC_TRANSFER)
                .toAccountUuid(null)
                .build();
        Optional<RiskFinding> f = rules().get("UNKNOWN_DESTINATION").evaluate(t);
        assertThat(f).isPresent();
        assertThat(f.get().getLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void unknownDestinationRule_firesForBlankDestination() {
        Transaction t = baseTx()
                .type(TransactionType.INTERNATIONAL_TRANSFER)
                .toAccountUuid("   ")
                .build();
        assertThat(rules().get("UNKNOWN_DESTINATION").evaluate(t)).isPresent();
    }

    @Test
    void unknownDestinationRule_doesNotFireForAtmWithdrawal() {
        Transaction t = baseTx()
                .type(TransactionType.ATM_WITHDRAWAL)
                .toAccountUuid(null)
                .build();
        assertThat(rules().get("UNKNOWN_DESTINATION").evaluate(t)).isEmpty();
    }

    @Test
    void unknownDestinationRule_doesNotFireWhenDestinationPresent() {
        Transaction t = baseTx()
                .type(TransactionType.DOMESTIC_TRANSFER)
                .toAccountUuid("B")
                .build();
        assertThat(rules().get("UNKNOWN_DESTINATION").evaluate(t)).isEmpty();
    }

    @Test
    void registryExposesAllSevenRules() {
        assertThat(registry.byName()).hasSize(7);
        assertThat(registry.all()).hasSize(7);
    }
}
