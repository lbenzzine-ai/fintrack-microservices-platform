package com.fintrack.account.strategy.interest;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Java 21 — enum-keyed {@code EnumMap} registry + pattern-matching switch over the
 * sealed {@link InterestStrategy} hierarchy. Adding a fourth strategy means: extend the
 * sealed interface's {@code permits}, add a {@code Kind} enum constant, add a {@code case}
 * here — the compiler enforces exhaustiveness.
 */
@Slf4j
@Component
public class InterestStrategyRegistry {

    public enum Kind { FLAT, TIERED, COMPOUND }

    private final Map<Kind, InterestStrategy> strategies = new EnumMap<>(Kind.class);
    private final Kind defaultKind;

    public InterestStrategyRegistry(List<InterestStrategy> available,
                                    @Value("${fintrack.account.interest.strategy:tiered}") String defaultName) {
        for (InterestStrategy s : available) {
            Kind k = switch (s) {
                case FlatInterestStrategy ignored     -> Kind.FLAT;
                case TieredInterestStrategy ignored   -> Kind.TIERED;
                case CompoundInterestStrategy ignored -> Kind.COMPOUND;
            };
            strategies.put(k, s);
        }
        this.defaultKind = resolve(defaultName);
    }

    @PostConstruct
    void verify() {
        if (!strategies.containsKey(defaultKind)) {
            throw new IllegalStateException("No bean for InterestStrategy " + defaultKind
                    + " — registered: " + strategies.keySet());
        }
        log.info("InterestStrategy default={}, registered={}", defaultKind, strategies.keySet());
    }

    public InterestStrategy active() { return strategies.get(defaultKind); }

    public InterestStrategy by(String name) { return strategies.get(resolve(name)); }

    private static Kind resolve(String name) {
        return switch (name == null ? "" : name.toLowerCase()) {
            case "flat"     -> Kind.FLAT;
            case "tiered"   -> Kind.TIERED;
            case "compound" -> Kind.COMPOUND;
            default -> throw new IllegalArgumentException("Unknown interest strategy: " + name);
        };
    }
}
