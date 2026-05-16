package com.fintrack.account.messaging;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            throw new IllegalStateException("fintrack.messaging.broker=" + activeBroker
                    + " — no matching MessagingStrategy bean. Available: " + strategies.keySet());
        }
        log.info("MessagingStrategy active broker = '{}', available = {}", activeBroker, strategies.keySet());
    }

    public MessagingStrategy active() { return strategies.get(activeBroker); }
}
