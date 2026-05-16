package com.fintrack.account.messaging;

public sealed interface MessagingStrategy
        permits KafkaMessagingStrategy, RabbitMqMessagingStrategy {

    String brokerName();
    void publish(String topic, String key, Object event);
}
