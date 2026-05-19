package com.fintrack.transaction.risk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskScoreTest {

    private static RiskFinding finding(RiskLevel level) {
        return RiskFinding.builder()
                .level(level)
                .reason("for-test")
                .ruleName(level.name() + "_RULE")
                .build();
    }

    @Test
    void cleanFactory_returnsLowZeroEmpty() {
        RiskScore s = RiskScore.clean("tx-1");

        assertThat(s.getTransactionUuid()).isEqualTo("tx-1");
        assertThat(s.getLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(s.getScore()).isZero();
        assertThat(s.getFindings()).isEmpty();
        assertThat(s.isBlocked()).isFalse();
        assertThat(s.isRequiresReview()).isFalse();
        assertThat(s.shouldBlock()).isFalse();
        assertThat(s.shouldAlert()).isFalse();
        assertThat(s.getAssessedAt()).isNotNull();
    }

    @Test
    void fromFindings_derivesMaxLevelAndSumScore() {
        RiskScore s = RiskScore.from("tx-2", List.of(
                finding(RiskLevel.LOW),
                finding(RiskLevel.HIGH),
                finding(RiskLevel.MEDIUM)));

        assertThat(s.getLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(s.getScore()).isEqualTo(RiskLevel.LOW.weight()
                + RiskLevel.HIGH.weight()
                + RiskLevel.MEDIUM.weight());
        assertThat(s.isBlocked()).isFalse();
        assertThat(s.isRequiresReview()).isTrue();
    }

    @Test
    void fromFindings_emptyList_isLowAndNotBlocked() {
        RiskScore s = RiskScore.from("tx-3", List.of());
        assertThat(s.getLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(s.getScore()).isZero();
        assertThat(s.isBlocked()).isFalse();
        assertThat(s.isRequiresReview()).isFalse();
    }

    @Test
    void shouldBlock_trueOnlyForCritical() {
        for (RiskLevel l : RiskLevel.values()) {
            RiskScore s = RiskScore.builder().level(l).build();
            assertThat(s.shouldBlock()).isEqualTo(l == RiskLevel.CRITICAL);
        }
    }

    @ParameterizedTest
    @EnumSource(value = RiskLevel.class, names = {"HIGH", "CRITICAL"})
    void shouldAlert_trueForHighOrCritical(RiskLevel level) {
        RiskScore s = RiskScore.builder().level(level).build();
        assertThat(s.shouldAlert()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = RiskLevel.class, names = {"LOW", "MEDIUM"})
    void shouldAlert_falseForLowOrMedium(RiskLevel level) {
        RiskScore s = RiskScore.builder().level(level).build();
        assertThat(s.shouldAlert()).isFalse();
    }

    @Test
    void shouldAlert_handlesNullLevelSafely() {
        RiskScore s = RiskScore.builder().build();
        assertThat(s.shouldAlert()).isFalse();
    }

    @Test
    void fromFindings_exactlyHighWeight_isRequiresReview() {
        // Boundary: score == HIGH.weight() (70). isRequiresReview must be true.
        RiskScore s = RiskScore.from("tx-boundary", List.of(finding(RiskLevel.HIGH)));
        assertThat(s.getScore()).isEqualTo(RiskLevel.HIGH.weight());
        assertThat(s.isRequiresReview()).isTrue();
        assertThat(s.isBlocked()).isFalse();
    }

    @Test
    void fromFindings_belowHighWeight_doesNotRequireReview() {
        // Score 60 (< HIGH weight 70), level MEDIUM. Should not require review.
        RiskScore s = RiskScore.from("tx-low",
                List.of(finding(RiskLevel.MEDIUM), finding(RiskLevel.MEDIUM)));
        assertThat(s.getScore()).isLessThan(RiskLevel.HIGH.weight());
        assertThat(s.isRequiresReview()).isFalse();
    }

    @Test
    void riskLevel_isAtLeastByWeight() {
        assertThat(RiskLevel.CRITICAL.isAtLeast(RiskLevel.LOW)).isTrue();
        assertThat(RiskLevel.CRITICAL.isAtLeast(RiskLevel.CRITICAL)).isTrue();
        assertThat(RiskLevel.HIGH.isAtLeast(RiskLevel.MEDIUM)).isTrue();
        assertThat(RiskLevel.LOW.isAtLeast(RiskLevel.MEDIUM)).isFalse();
        assertThat(RiskLevel.MEDIUM.isAtLeast(RiskLevel.HIGH)).isFalse();
    }

    @Test
    void riskLevel_weightsAreStrictlyIncreasing() {
        assertThat(RiskLevel.LOW.weight()).isLessThan(RiskLevel.MEDIUM.weight());
        assertThat(RiskLevel.MEDIUM.weight()).isLessThan(RiskLevel.HIGH.weight());
        assertThat(RiskLevel.HIGH.weight()).isLessThan(RiskLevel.CRITICAL.weight());
    }
}
