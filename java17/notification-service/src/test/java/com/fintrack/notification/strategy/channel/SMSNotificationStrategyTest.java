package com.fintrack.notification.strategy.channel;

import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SMSNotificationStrategyTest {

    private final SMSNotificationStrategy strategy =
            new SMSNotificationStrategy("http://localhost:9999/sms", "FINTRACK");

    @Test
    void channel_isSms() {
        assertThat(strategy.channel()).isEqualTo(NotificationChannel.SMS);
    }

    @Test
    void send_returnsDelivered_withProvider() {
        Notification n = Notification.builder()
                .recipient("+15551234567")
                .body("Hello")
                .channel(NotificationChannel.SMS)
                .build();

        DispatchResult r = strategy.send(n);

        assertThat(r.isDelivered()).isTrue();
        assertThat(r.getProvider()).isEqualTo(SMSNotificationStrategy.PROVIDER);
        assertThat(r.getFailureReason()).isNull();
    }
}
