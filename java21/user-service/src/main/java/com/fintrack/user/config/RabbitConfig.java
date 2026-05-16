package com.fintrack.user.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ wiring — only loaded when {@code fintrack.messaging.broker=rabbitmq}, but the
 * {@link com.fintrack.user.messaging.RabbitMqMessagingStrategy} bean is unconditional so the registry
 * always sees both broker options. The Rabbit infra beans (template, exchange, converter) are however
 * declared here so that switching brokers is a config-only change.
 */
@Configuration
public class RabbitConfig {

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter converter) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(converter);
        return t;
    }

    @Bean
    @ConditionalOnProperty(name = "fintrack.messaging.broker", havingValue = "rabbitmq")
    public TopicExchange fintrackEventsExchange(@Value("${fintrack.messaging.rabbitmq.exchange:fintrack.events}") String name) {
        return new TopicExchange(name, true, false);
    }
}
