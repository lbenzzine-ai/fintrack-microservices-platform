package com.fintrack.transaction.messaging;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MessagingStrategyRegistryTest {

    private static MessagingStrategy fake(String name) {
        return new MessagingStrategy() {
            @Override public String brokerName() { return name; }
            @Override public void publish(String topic, String key, Object event) {}
        };
    }

    @Test
    void active_returnsImplMatchingActiveBroker() {
        MessagingStrategy k = fake("kafka");
        MessagingStrategy r = fake("rabbit");
        MessagingStrategyRegistry reg = new MessagingStrategyRegistry(List.of(k, r), "kafka");
        reg.verify();
        assertThat(reg.active()).isSameAs(k);
    }

    @Test
    void active_switchesWithBrokerProperty() {
        MessagingStrategy k = fake("kafka");
        MessagingStrategy r = fake("rabbit");
        MessagingStrategyRegistry reg = new MessagingStrategyRegistry(List.of(k, r), "rabbit");
        reg.verify();
        assertThat(reg.active()).isSameAs(r);
    }

    @Test
    void verify_throwsWhenActiveBrokerMissing() {
        MessagingStrategy k = fake("kafka");
        MessagingStrategyRegistry reg = new MessagingStrategyRegistry(List.of(k), "rabbit");
        assertThatThrownBy(reg::verify)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("rabbit");
    }
}
