package com.fintrack.transaction.strategy.fee;

import com.fintrack.transaction.entity.TransactionType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Discovers all {@link FeeStrategy} beans and indexes them by the {@link TransactionType}s
 * they support. Adding a new fee Strategy is a drop-in — just register a new bean.
 */
@Slf4j
@Component
public class FeeStrategyRegistry {

    private final Map<TransactionType, FeeStrategy> byType = new EnumMap<>(TransactionType.class);
    private final Map<String, FeeStrategy> byName;

    public FeeStrategyRegistry(List<FeeStrategy> strategies) {
        this.byName = strategies.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                FeeStrategy::name, s -> s));
        for (FeeStrategy s : strategies) {
            for (TransactionType t : s.supports()) {
                FeeStrategy existing = byType.put(t, s);
                if (existing != null && existing != s) {
                    throw new IllegalStateException("Duplicate FeeStrategy for type " + t
                            + ": " + existing.name() + " and " + s.name());
                }
            }
        }
    }

    @PostConstruct
    void verify() {
        for (TransactionType t : TransactionType.values()) {
            if (!byType.containsKey(t)) {
                throw new IllegalStateException("No FeeStrategy registered for TransactionType " + t);
            }
        }
        log.info("FeeStrategyRegistry ready: {}", byName.keySet());
    }

    public FeeStrategy strategyFor(TransactionType type) {
        FeeStrategy s = byType.get(type);
        if (s == null) throw new IllegalStateException("No FeeStrategy for type " + type);
        return s;
    }

    public FeeStrategy by(String name) {
        FeeStrategy s = byName.get(name);
        if (s == null) throw new IllegalArgumentException("Unknown fee strategy: " + name);
        return s;
    }
}
