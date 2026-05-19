package com.fintrack.transaction.risk;

public enum RiskLevel {
    LOW(10),
    MEDIUM(30),
    HIGH(70),
    CRITICAL(100);

    private final int weight;

    RiskLevel(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
