package com.fintrack.user.messaging;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Strategy registry — picks the active {@link MessagingStrategy} from {@code fintrack.messaging.broker}.
 *
 * <p>This is the pattern replacement for the classic {@code switch(broker) { case "kafka": ...; }}
 * dispatch. Adding a third broker requires only a new {@code MessagingStrategy} bean — no edits here.
 */
@Slf4j
@Component
public class MessagingStrategyRegistry {

    private final Map<String, MessagingStrategy> strategies;
    private final String activeBroker;

    public MessagingStrategyRegistry(List<MessagingStrategy> available,
                                     @Value("${fintrack.messaging.broker:kafka}") String activeBroker) {
        this.strategies = available.stream()
                .collect(Collectors.toUnmodifiableMap(MessagingStrategy::brokerName, Function.identity()));
        this.activeBroker = activeBroker;
    }

    @PostConstruct
    void verify() {
        if (!strategies.containsKey(activeBroker)) {
            throw new IllegalStateException(
                    "fintrack.messaging.broker=" + activeBroker + " — no matching MessagingStrategy bean. Available: " + strategies.keySet());
        }
        log.info("MessagingStrategy active broker = '{}', available = {}", activeBroker, strategies.keySet());
    }

    public MessagingStrategy active() {
        return strategies.get(activeBroker);
    }

    public MessagingStrategy by(String name) {
        return strategies.get(name);
    }
}
