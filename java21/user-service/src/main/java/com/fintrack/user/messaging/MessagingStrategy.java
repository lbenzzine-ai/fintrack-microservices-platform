package com.fintrack.user.messaging;

/**
 * Java 21 — {@code sealed interface} for the messaging Strategy hierarchy. Locks the broker
 * options to the two known implementations so the compiler can enforce exhaustive {@code switch}
 * over them (see {@link MessagingStrategyRegistry}).
 */
public sealed interface MessagingStrategy
        permits KafkaMessagingStrategy, RabbitMqMessagingStrategy {

    /** Broker identifier — matches the {@code fintrack.messaging.broker} value (e.g. {@code "kafka"}). */
    String brokerName();

    /**
     * @param topic logical destination (Kafka topic name or RabbitMQ routing key — both are read
     *              from the {@code fintrack.messaging.kafka.topics} / {@code .rabbitmq.queues} maps)
     * @param key   partition key / correlation key
     * @param event payload (will be JSON-serialised)
     */
    void publish(String topic, String key, Object event);
}
