package com.fintrack.account.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j @Component @RequiredArgsConstructor
public final class KafkaMessagingStrategy implements MessagingStrategy {
    public static final String NAME = "kafka";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override public String brokerName() { return NAME; }

    @Override
    public void publish(String topic, String key, Object event) {
        log.debug("Kafka send → topic={} key={}", topic, key);
        kafkaTemplate.send(topic, key, event);
    }
}
