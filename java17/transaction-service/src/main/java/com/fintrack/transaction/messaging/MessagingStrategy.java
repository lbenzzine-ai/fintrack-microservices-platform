package com.fintrack.transaction.messaging;

public interface MessagingStrategy {
    String brokerName();
    void publish(String topic, String key, Object event);
}
