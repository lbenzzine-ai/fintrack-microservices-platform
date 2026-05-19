package com.fintrack.transaction.risk;

import com.fintrack.transaction.entity.Transaction;

import java.util.Optional;

@FunctionalInterface
public interface RiskRule {
    Optional<RiskFinding> evaluate(Transaction tx);
}
