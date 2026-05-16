package com.fintrack.account.dto;

import java.math.BigDecimal;

public record InterestPreview(
        String strategy,
        BigDecimal principal,
        BigDecimal interest,
        BigDecimal projectedBalance
) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String strategy;
        private BigDecimal principal;
        private BigDecimal interest;
        private BigDecimal projectedBalance;
        public Builder strategy(String v)              { this.strategy = v;         return this; }
        public Builder principal(BigDecimal v)         { this.principal = v;        return this; }
        public Builder interest(BigDecimal v)          { this.interest = v;         return this; }
        public Builder projectedBalance(BigDecimal v)  { this.projectedBalance = v; return this; }
        public InterestPreview build() { return new InterestPreview(strategy, principal, interest, projectedBalance); }
    }
}
