package com.fintrack.user.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class RabbitMqMessagingStrategy implements MessagingStrategy {

    public static final String NAME = "rabbitmq";

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    public RabbitMqMessagingStrategy(RabbitTemplate rabbitTemplate,
                                     @Value("${fintrack.messaging.rabbitmq.exchange:fintrack.events}") String exchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    @Override
    public String brokerName() { return NAME; }

    @Override
    public void publish(String topic, String key, Object event) {
        // For RabbitMQ we use the Kafka "topic" as the routing key.
        log.debug("Rabbit send → exchange={} routingKey={} payload={}", exchange, topic, event);
        rabbitTemplate.convertAndSend(exchange, topic, event);
    }
}
