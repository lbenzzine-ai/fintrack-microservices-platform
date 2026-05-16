package com.fintrack.account.strategy.interest;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Strategy registry — Spring discovers all {@link InterestStrategy} beans automatically; the
 * active one is selected at runtime via {@code fintrack.account.interest.strategy}. Adding a
 * fourth strategy requires only a new {@link InterestStrategy} bean — no edits here.
 */
@Slf4j
@Component
public class InterestStrategyRegistry {

    private final Map<String, InterestStrategy> strategies;
    private final String defaultStrategy;

    public InterestStrategyRegistry(List<InterestStrategy> all,
                                    @Value("${fintrack.account.interest.strategy:tiered}") String defaultStrategy) {
        this.strategies = all.stream()
                .collect(Collectors.toUnmodifiableMap(InterestStrategy::name, Function.identity()));
        this.defaultStrategy = defaultStrategy;
    }

    @PostConstruct
    void verify() {
        if (!strategies.containsKey(defaultStrategy)) {
            throw new IllegalStateException("fintrack.account.interest.strategy=" + defaultStrategy
                    + " — no matching InterestStrategy bean. Available: " + strategies.keySet());
        }
        log.info("InterestStrategy default='{}', available={}", defaultStrategy, strategies.keySet());
    }

    public InterestStrategy active() { return strategies.get(defaultStrategy); }

    public InterestStrategy by(String name) {
        InterestStrategy s = strategies.get(name);
        if (s == null) throw new IllegalArgumentException("Unknown interest strategy: " + name);
        return s;
    }
}
