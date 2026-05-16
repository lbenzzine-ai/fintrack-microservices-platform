package com.fintrack.account.messaging;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class MessagingStrategyRegistry {

    public enum Broker { KAFKA, RABBITMQ }

    private final Map<Broker, MessagingStrategy> strategies = new EnumMap<>(Broker.class);
    private final Broker active;

    public MessagingStrategyRegistry(List<MessagingStrategy> available,
                                     @Value("${fintrack.messaging.broker:kafka}") String activeName) {
        for (MessagingStrategy s : available) {
            Broker key = switch (s) {
                case KafkaMessagingStrategy ignored    -> Broker.KAFKA;
                case RabbitMqMessagingStrategy ignored -> Broker.RABBITMQ;
            };
            strategies.put(key, s);
        }
        this.active = switch (activeName.toLowerCase()) {
            case "kafka"    -> Broker.KAFKA;
            case "rabbitmq" -> Broker.RABBITMQ;
            default -> throw new IllegalStateException("Unknown broker: " + activeName);
        };
    }

    @PostConstruct
    void verify() {
        if (!strategies.containsKey(active)) {
            throw new IllegalStateException("No bean for broker " + active);
        }
        log.info("MessagingStrategy active={}, registered={}", active, strategies.keySet());
    }

    public MessagingStrategy active() { return strategies.get(active); }
}
