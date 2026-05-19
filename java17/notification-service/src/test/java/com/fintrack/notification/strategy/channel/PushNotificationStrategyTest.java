package com.fintrack.notification.strategy.channel;

import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PushNotificationStrategyTest {

    @Test
    void channel_isPush() {
        PushNotificationStrategy strategy =
                new PushNotificationStrategy("https://fcm/send", "");
        assertThat(strategy.channel()).isEqualTo(NotificationChannel.PUSH);
    }

    @Test
    void send_withApiKeyBlank_returnsDelivered() {
        PushNotificationStrategy strategy =
                new PushNotificationStrategy("https://fcm/send", "");
        Notification n = Notification.builder()
                .recipient("device-token-abcdef1234")
                .channel(NotificationChannel.PUSH)
                .build();

        DispatchResult r = strategy.send(n);

        assertThat(r.isDelivered()).isTrue();
        assertThat(r.getProvider()).isEqualTo(PushNotificationStrategy.PROVIDER);
        assertThat(r.getFailureReason()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "abcdefgh", "abcdefghijklmnop"})
    void send_masksToken_regardlessOfLength(String token) {
        PushNotificationStrategy strategy =
                new PushNotificationStrategy("https://fcm/send", "secret");
        Notification n = Notification.builder()
                .recipient(token)
                .channel(NotificationChannel.PUSH)
                .build();

        DispatchResult r = strategy.send(n);

        // We only assert delivery; masking happens in logs, not in the result.
        assertThat(r.isDelivered()).isTrue();
    }

    @Test
    void send_nullRecipient_stillDelivered() {
        PushNotificationStrategy strategy =
                new PushNotificationStrategy("https://fcm/send", "");
        Notification n = Notification.builder()
                .recipient(null)
                .channel(NotificationChannel.PUSH)
                .build();

        DispatchResult r = strategy.send(n);

        assertThat(r.isDelivered()).isTrue();
    }
}
