package com.fintrack.transaction.risk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class RiskFinding {
    private RiskLevel level;
    private String reason;
    private String ruleName;
}
