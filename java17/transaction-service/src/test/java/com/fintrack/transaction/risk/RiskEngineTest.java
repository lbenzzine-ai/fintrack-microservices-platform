package com.fintrack.transaction.risk;

import com.fintrack.transaction.entity.Transaction;
import com.fintrack.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RiskEngineTest {

    private final RiskEngine engine = new RiskEngine(new RiskRuleRegistry());

    private static Transaction tx(TransactionType type, BigDecimal amount,
                                  String from, String to, String description) {
        return Transaction.builder()
                .uuid("tx-" + type)
                .fromAccountUuid(from)
                .toAccountUuid(to)
                .amount(amount)
                .fee(BigDecimal.ZERO)
                .currencyCode("USD")
                .type(type)
                .description(description)
                .build();
    }

    @Test
    void cleanTransaction_isLowAndNotBlocked() {
        Transaction t = tx(TransactionType.DOMESTIC_TRANSFER, new BigDecimal("123.45"),
                "src", "dst", "groceries");

        RiskScore s = engine.assess(t);

        assertThat(s.getLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(s.isBlocked()).isFalse();
        assertThat(s.shouldBlock()).isFalse();
        assertThat(s.getFindings()).isEmpty();
        assertThat(s.getTransactionUuid()).isEqualTo(t.getUuid());
    }

    @Test
    void selfTransfer_isCriticalAndBlocked() {
        Transaction t = tx(TransactionType.DOMESTIC_TRANSFER, new BigDecimal("10"),
                "wallet-A", "wallet-A", "oops");

        RiskScore s = engine.assess(t);

        assertThat(s.getLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(s.isBlocked()).isTrue();
        assertThat(s.shouldBlock()).isTrue();
        assertThat(s.getFindings())
                .extracting(RiskFinding::getRuleName)
                .contains("SELF_TRANSFER");
    }

    @Test
    void amountAtOrAboveLargeThreshold_isHighAndRequiresReview() {
        Transaction t = tx(TransactionType.DOMESTIC_TRANSFER, new BigDecimal("10000"),
                "src", "dst", "rent");

        RiskScore s = engine.assess(t);

        assertThat(s.getLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(s.isRequiresReview()).isTrue();
        assertThat(s.isBlocked()).isFalse();
        assertThat(s.getFindings())
                .extracting(RiskFinding::getRuleName)
                .contains("LARGE_AMOUNT");
    }

    @Test
    void internationalAtOrAboveHighValueThreshold_isHigh() {
        Transaction t = tx(TransactionType.INTERNATIONAL_TRANSFER, new BigDecimal("5000"),
                "src", "dst", "tuition");

        RiskScore s = engine.assess(t);

        assertThat(s.getLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(s.isRequiresReview()).isTrue();
        assertThat(s.getFindings())
                .extracting(RiskFinding::getRuleName)
                .contains("HIGH_VALUE_INTERNATIONAL");
    }

    @Test
    void atmWithdrawalAboveLimit_isMedium() {
        Transaction t = tx(TransactionType.ATM_WITHDRAWAL, new BigDecimal("1500"),
                "src", null, "cash");

        RiskScore s = engine.assess(t);

        assertThat(s.getFindings())
                .extracting(RiskFinding::getRuleName)
                .contains("ATM_HIGH_VALUE");
        assertThat(s.getLevel().isAtLeast(RiskLevel.MEDIUM)).isTrue();
    }

    @Test
    void roundThousandStructuringPattern_isMedium() {
        Transaction t = tx(TransactionType.DOMESTIC_TRANSFER, new BigDecimal("3000"),
                "src", "dst", "rent");

        RiskScore s = engine.assess(t);

        assertThat(s.getFindings())
                .extracting(RiskFinding::getRuleName)
                .contains("STRUCTURING_PATTERN");
        assertThat(s.getLevel().isAtLeast(RiskLevel.MEDIUM)).isTrue();
    }

    @Test
    void multipleRulesFire_worstLevelWins() {
        // self-transfer (CRITICAL) + large amount (HIGH) + structuring (MEDIUM) + missing desc (LOW)
        Transaction t = tx(TransactionType.INTERNATIONAL_TRANSFER, new BigDecimal("10000"),
                "wallet-X", "wallet-X", null);

        RiskScore s = engine.assess(t);

        assertThat(s.getLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(s.isBlocked()).isTrue();
        assertThat(s.getFindings().size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void nullTransaction_throwsNullPointerException() {
        assertThatThrownBy(() -> engine.assess(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void registryThatReturnsEmpty_yieldsCleanScore() {
        RiskRuleRegistry mockRegistry = mock(RiskRuleRegistry.class);
        when(mockRegistry.all()).thenReturn(List.<RiskRule>of());
        RiskEngine isolatedEngine = new RiskEngine(mockRegistry);

        Transaction t = tx(TransactionType.DOMESTIC_TRANSFER, new BigDecimal("50"),
                "a", "b", "lunch");

        RiskScore s = isolatedEngine.assess(t);

        assertThat(s.getLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(s.getScore()).isZero();
        assertThat(s.getFindings()).isEmpty();
    }

    @Test
    void rulesThatThrow_areSkippedWithoutFailingEngine() {
        RiskRuleRegistry mockRegistry = mock(RiskRuleRegistry.class);
        RiskRule throwing = tx -> { throw new RuntimeException("boom"); };
        RiskRule ok = tx -> Optional.of(
                RiskFinding.builder()
                        .level(RiskLevel.LOW)
                        .reason("ok rule")
                        .ruleName("OK")
                        .build());
        when(mockRegistry.all()).thenReturn(List.of(throwing, ok));
        RiskEngine isolatedEngine = new RiskEngine(mockRegistry);

        Transaction t = tx(TransactionType.DOMESTIC_TRANSFER, new BigDecimal("50"),
                "a", "b", "lunch");
        RiskScore s = isolatedEngine.assess(t);

        assertThat(s.getFindings()).extracting(RiskFinding::getRuleName).containsExactly("OK");
    }
}
