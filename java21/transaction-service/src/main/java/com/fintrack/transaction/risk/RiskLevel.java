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

    /** True if this level is at least as severe as {@code other} (by weight). */
    public boolean isAtLeast(RiskLevel other) {
        return this.weight >= other.weight;
    }
}
