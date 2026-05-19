package com.fintrack.notification.strategy.channel;

import com.fintrack.notification.entity.NotificationChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class NotificationStrategyContextTest {

    private static NotificationStrategy email() {
        // The real EmailNotificationStrategy needs a JavaMailSender; we only call channel() in these tests.
        return new EmailNotificationStrategy(mock(JavaMailSender.class));
    }

    private static NotificationStrategy sms() {
        return new SMSNotificationStrategy("http://x", "FT");
    }

    private static NotificationStrategy push() {
        return new PushNotificationStrategy("http://x", "");
    }

    @ParameterizedTest
    @EnumSource(NotificationChannel.class)
    void strategyFor_returnsRegisteredStrategy(NotificationChannel channel) {
        NotificationStrategyContext ctx = new NotificationStrategyContext(List.of(email(), sms(), push()));

        NotificationStrategy s = ctx.strategyFor(channel);
        assertThat(s.channel()).isEqualTo(channel);
    }

    @Test
    void constructor_throws_onDuplicateChannel() {
        // Two distinct EmailNotificationStrategy instances → both report EMAIL.
        NotificationStrategy a = email();
        NotificationStrategy b = email();

        assertThatThrownBy(() -> new NotificationStrategyContext(List.of(a, b)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void strategyFor_throws_whenChannelMissing() {
        NotificationStrategyContext ctx = new NotificationStrategyContext(List.of(email()));

        assertThatThrownBy(() -> ctx.strategyFor(NotificationChannel.SMS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No strategy");
    }
}
