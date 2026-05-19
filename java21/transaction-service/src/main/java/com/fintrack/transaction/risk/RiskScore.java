package com.fintrack.transaction.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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
