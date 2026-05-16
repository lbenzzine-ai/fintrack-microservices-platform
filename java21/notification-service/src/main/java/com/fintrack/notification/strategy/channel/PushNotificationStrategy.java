package com.fintrack.notification.strategy.channel;

import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class PushNotificationStrategy implements NotificationStrategy {

    public static final String PROVIDER = "fcm-stub";

    private final String fcmEndpoint;
    private final String apiKey;

    public PushNotificationStrategy(
            @Value("${fintrack.notification.push.fcm-endpoint:https://fcm.googleapis.com/fcm/send}") String fcmEndpoint,
            @Value("${fintrack.notification.push.api-key:}") String apiKey) {
        this.fcmEndpoint = fcmEndpoint;
        this.apiKey = apiKey;
    }

    @Override
    public NotificationChannel channel() { return NotificationChannel.PUSH; }

    @Override
    public DispatchResult send(Notification n) {
        log.info("[STUB] Push to deviceToken={} via {} (apiKey set: {})",
                mask(n.getRecipient()), fcmEndpoint, !apiKey.isBlank());
        return DispatchResult.ok(PROVIDER);
    }

    private String mask(String token) {
        if (token == null || token.length() < 8) return "****";
        return token.substring(0, 4) + "…" + token.substring(token.length() - 4);
    }
}
