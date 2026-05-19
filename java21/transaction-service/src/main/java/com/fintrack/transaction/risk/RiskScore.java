package com.fintrack.transaction.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RiskScore {
    private RiskLevel level;
    private int score;
    private List<RiskFinding> findings;
    private boolean blocked;
    private boolean requiresReview;
    private Instant assessedAt;
    private String transactionUuid;

    /** No findings — used when a tx passes every rule. */
    public static RiskScore clean(String transactionUuid) {
        return RiskScore.builder()
                .transactionUuid(transactionUuid)
                .level(RiskLevel.LOW)
                .score(0)
                .findings(Collections.emptyList())
                .blocked(false)
                .requiresReview(false)
                .assessedAt(Instant.now())
                .build();
    }

    /** Returns true only when the overall level is CRITICAL (i.e. the saga must abort). */
    public boolean shouldBlock() {
        return level == RiskLevel.CRITICAL;
    }

    /** Returns true when reviewers should be alerted — HIGH or CRITICAL. */
    public boolean shouldAlert() {
        return level != null && level.isAtLeast(RiskLevel.HIGH);
    }

    public static RiskScore from(String transactionUuid, List<RiskFinding> findings) {
        RiskLevel level = findings.stream()
                .map(RiskFinding::getLevel)
                .max(Comparator.comparingInt(RiskLevel::weight))
                .orElse(RiskLevel.LOW);
        int score = findings.stream().mapToInt(f -> f.getLevel().weight()).sum();
        boolean blocked = findings.stream().anyMatch(f -> f.getLevel() == RiskLevel.CRITICAL);
        boolean requiresReview = !blocked &&
                (level == RiskLevel.HIGH || score >= RiskLevel.HIGH.weight());
        return RiskScore.builder()
                .transactionUuid(transactionUuid)
                .level(level)
                .score(score)
                .findings(findings)
                .blocked(blocked)
                .requiresReview(requiresReview)
                .assessedAt(Instant.now())
                .build();
    }
}
