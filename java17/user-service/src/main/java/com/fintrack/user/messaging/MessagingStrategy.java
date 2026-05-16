package com.fintrack.user.messaging;

/**
 * Broker-agnostic publisher. The active strategy is selected at runtime by
 * {@link MessagingStrategyRegistry} based on the {@code fintrack.messaging.broker} property,
 * so the service code never branches on broker type.
 *
 * <p>Java 17 — declared as a plain interface. The Java 21 stack uses a {@code sealed interface}
 * with explicit {@code permits} for the two known implementations.
 */
public interface MessagingStrategy {

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
