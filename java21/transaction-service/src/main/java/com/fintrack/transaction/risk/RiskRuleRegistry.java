package com.fintrack.transaction.risk;

import com.fintrack.transaction.entity.Transaction;
import com.fintrack.transaction.entity.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

@Component
public class RiskRuleRegistry {

    private static final BigDecimal LARGE_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal INTL_HIGH_VALUE_THRESHOLD = new BigDecimal("5000");
    private static final BigDecimal ATM_HIGH_VALUE_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal STRUCTURING_BUCKET = new BigDecimal("1000");
    private static final BigDecimal MISSING_DESC_THRESHOLD = new BigDecimal("5000");

    private final Map<String, RiskRule> rules = Map.ofEntries(
            entry("SELF_TRANSFER", RiskLevel.CRITICAL,
                    "Source and destination wallets are identical",
                    tx -> tx.getToAccountUuid() != null
                            && Objects.equals(tx.getFromAccountUuid(), tx.getToAccountUuid())),

            entry("LARGE_AMOUNT", RiskLevel.HIGH,
                    "Amount exceeds large-transfer threshold (10,000)",
                    tx -> tx.getAmount() != null && tx.getAmount().compareTo(LARGE_THRESHOLD) >= 0),

            entry("STRUCTURING_PATTERN", RiskLevel.MEDIUM,
                    "Round-thousand amount — possible structuring",
                    tx -> tx.getAmount() != null
                            && tx.getAmount().compareTo(STRUCTURING_BUCKET) >= 0
                            && tx.getAmount().remainder(STRUCTURING_BUCKET).signum() == 0),

            entry("HIGH_VALUE_INTERNATIONAL", RiskLevel.HIGH,
                    "International transfer above review threshold (5,000)",
                    tx -> tx.getType() == TransactionType.INTERNATIONAL_TRANSFER
                            && tx.getAmount() != null
                            && tx.getAmount().compareTo(INTL_HIGH_VALUE_THRESHOLD) >= 0),

            entry("ATM_HIGH_VALUE", RiskLevel.MEDIUM,
                    "ATM withdrawal above per-tx limit (1,000)",
                    tx -> tx.getType() == TransactionType.ATM_WITHDRAWAL
                            && tx.getAmount() != null
                            && tx.getAmount().compareTo(ATM_HIGH_VALUE_THRESHOLD) > 0),

            entry("MISSING_DESCRIPTION", RiskLevel.LOW,
                    "Large transfer without description",
                    tx -> tx.getAmount() != null
                            && tx.getAmount().compareTo(MISSING_DESC_THRESHOLD) >= 0
                            && (tx.getDescription() == null || tx.getDescription().isBlank())),

            entry("UNKNOWN_DESTINATION", RiskLevel.HIGH,
                    "Transfer-type transaction missing destination wallet",
                    tx -> (tx.getType() == TransactionType.DOMESTIC_TRANSFER
                            || tx.getType() == TransactionType.INTERNATIONAL_TRANSFER
                            || tx.getType() == TransactionType.INTERNAL_TRANSFER)
                            && (tx.getToAccountUuid() == null || tx.getToAccountUuid().isBlank()))
    );

    public List<RiskRule> all() {
        return List.copyOf(rules.values());
    }

    public Map<String, RiskRule> byName() {
        return rules;
    }

    private static Map.Entry<String, RiskRule> entry(String name, RiskLevel level,
                                                     String reason, Predicate<Transaction> when) {
        RiskRule rule = tx -> when.test(tx)
                ? Optional.of(RiskFinding.builder()
                        .level(level)
                        .reason(reason)
                        .ruleName(name)
                        .build())
                : Optional.empty();
        return Map.entry(name, rule);
    }
}
