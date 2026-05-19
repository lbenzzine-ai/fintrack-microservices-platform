package com.fintrack.notification.strategy.channel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchResultTest {

    @Test
    void builder_setsAllFields() {
        DispatchResult r = DispatchResult.builder()
                .delivered(true)
                .provider("p1")
                .failureReason("nope")
                .build();
        assertThat(r.isDelivered()).isTrue();
        assertThat(r.getProvider()).isEqualTo("p1");
        assertThat(r.getFailureReason()).isEqualTo("nope");
    }

    @Test
    void defaultFailureReason_isNull_whenSuccess() {
        DispatchResult r = DispatchResult.builder().delivered(true).provider("p").build();
        assertThat(r.getFailureReason()).isNull();
    }
}
