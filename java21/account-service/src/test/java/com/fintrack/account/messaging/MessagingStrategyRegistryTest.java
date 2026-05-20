package com.fintrack.account.messaging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Java 21 — {@code MessagingStrategy} is sealed; the Registry resolves a strategy to its
 * {@link MessagingStrategyRegistry.Broker} enum via a pattern-matching switch over the
 * sealed hierarchy. Tests mock the permitted concrete (final) impls.
 */
@ExtendWith(MockitoExtension.class)
class MessagingStrategyRegistryTest {

    @Mock KafkaMessagingStrategy kafka;
    @Mock RabbitMqMessagingStrategy rabbit;

    @Test
    void active_returnsKafkaWhenConfiguredKafka() {
        MessagingStrategyRegistry reg = new MessagingStrategyRegistry(List.of(kafka, rabbit), "kafka");
        reg.verify();
        assertThat(reg.active()).isSameAs(kafka);
    }

    @Test
    void active_returnsRabbitWhenConfiguredRabbitmq() {
        MessagingStrategyRegistry reg = new MessagingStrategyRegistry(List.of(kafka, rabbit), "rabbitmq");
        reg.verify();
        assertThat(reg.active()).isSameAs(rabbit);
    }

    @Test
    void brokerNameIsCaseInsensitive() {
        MessagingStrategyRegistry reg = new MessagingStrategyRegistry(List.of(kafka), "KAFKA");
        reg.verify();
        assertThat(reg.active()).isSameAs(kafka);
    }

    @Test
    void verify_throwsWhenActiveBrokerHasNoBean() {
        // Only kafka registered, rabbitmq selected — bean missing.
        MessagingStrategyRegistry reg = new MessagingStrategyRegistry(List.of(kafka), "rabbitmq");
        assertThatThrownBy(reg::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RABBITMQ");
    }

    @Test
    void constructor_throwsOnUnknownBrokerName() {
        assertThatThrownBy(() -> new MessagingStrategyRegistry(List.of(kafka), "foobar"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown broker");
    }
}
