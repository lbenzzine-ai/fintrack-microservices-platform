package com.fintrack.user.messaging;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Java 21 — enum-keyed {@code EnumMap} for O(1) Strategy lookup, combined with a
 * pattern-matching {@code switch} to resolve a strategy bean to its enum key. The
 * compiler enforces exhaustiveness over {@link MessagingStrategy}'s sealed permits set.
 */
@Slf4j
@Component
public class MessagingStrategyRegistry {

    public enum Broker { KAFKA, RABBITMQ }

    private final Map<Broker, MessagingStrategy> strategies = new EnumMap<>(Broker.class);
    private final Broker active;

    public MessagingStrategyRegistry(List<MessagingStrategy> available,
                                     @Value("${fintrack.messaging.broker:kafka}") String activeName) {
        for (MessagingStrategy s : available) {
            // Pattern-matching switch over the sealed hierarchy — exhaustive by compiler check
            Broker key = switch (s) {
                case KafkaMessagingStrategy ignored    -> Broker.KAFKA;
                case RabbitMqMessagingStrategy ignored -> Broker.RABBITMQ;
            };
            strategies.put(key, s);
        }
        this.active = switch (activeName.toLowerCase()) {
            case "kafka"    -> Broker.KAFKA;
            case "rabbitmq" -> Broker.RABBITMQ;
            default -> throw new IllegalStateException(
                    "fintrack.messaging.broker=" + activeName + " — supported: kafka | rabbitmq");
        };
    }

    @PostConstruct
    void verify() {
        if (!strategies.containsKey(active)) {
            throw new IllegalStateException("No MessagingStrategy bean for broker " + active
                    + ". Registered: " + strategies.keySet());
        }
        log.info("MessagingStrategy active = {}, registered = {}", active, strategies.keySet());
    }

    public MessagingStrategy active() {
        return strategies.get(active);
    }

    public MessagingStrategy by(Broker broker) {
        return strategies.get(broker);
    }
}
