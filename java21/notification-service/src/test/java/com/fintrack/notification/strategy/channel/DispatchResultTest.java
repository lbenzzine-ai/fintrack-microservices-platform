package com.fintrack.notification.strategy.channel;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchResultTest {

    @Test
    void ok_buildsDeliveredResult() {
        DispatchResult r = DispatchResult.ok("p1");
        assertThat(r.delivered()).isTrue();
        assertThat(r.provider()).isEqualTo("p1");
        assertThat(r.failureReason()).isNull();
    }

    @Test
    void fail_buildsFailedResultWithReason() {
        DispatchResult r = DispatchResult.fail("p2", "boom");
        assertThat(r.delivered()).isFalse();
        assertThat(r.provider()).isEqualTo("p2");
        assertThat(r.failureReason()).isEqualTo("boom");
    }

    @Test
    void recordCanonical_setsAllFields() {
        DispatchResult r = new DispatchResult(true, "p", "reason");
        assertThat(r.delivered()).isTrue();
        assertThat(r.provider()).isEqualTo("p");
        assertThat(r.failureReason()).isEqualTo("reason");
    }
}
