package com.fintrack.notification.strategy.channel;

import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationStrategyContextTest {

    private static NotificationStrategy stub(NotificationChannel ch) {
        return new NotificationStrategy() {
            @Override public NotificationChannel channel() { return ch; }
            @Override public DispatchResult send(Notification n) {
                return DispatchResult.builder().delivered(true).provider("stub-" + ch).build();
            }
        };
    }

    @ParameterizedTest
    @EnumSource(NotificationChannel.class)
    void strategyFor_returnsRegisteredStrategy(NotificationChannel channel) {
        NotificationStrategy email = stub(NotificationChannel.EMAIL);
        NotificationStrategy sms = stub(NotificationChannel.SMS);
        NotificationStrategy push = stub(NotificationChannel.PUSH);

        NotificationStrategyContext ctx = new NotificationStrategyContext(List.of(email, sms, push));

        NotificationStrategy s = ctx.strategyFor(channel);
        assertThat(s.channel()).isEqualTo(channel);
    }

    @Test
    void constructor_throws_onDuplicateChannel() {
        NotificationStrategy a = stub(NotificationChannel.EMAIL);
        NotificationStrategy b = stub(NotificationChannel.EMAIL);

        assertThatThrownBy(() -> new NotificationStrategyContext(List.of(a, b)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void strategyFor_throws_whenChannelMissing() {
        // Only register EMAIL; SMS will be missing.
        NotificationStrategyContext ctx =
                new NotificationStrategyContext(List.of(stub(NotificationChannel.EMAIL)));

        assertThatThrownBy(() -> ctx.strategyFor(NotificationChannel.SMS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No strategy");
    }
}
