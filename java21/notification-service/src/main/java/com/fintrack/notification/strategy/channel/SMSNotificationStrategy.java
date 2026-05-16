package com.fintrack.notification.strategy.channel;

import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class SMSNotificationStrategy implements NotificationStrategy {

    public static final String PROVIDER = "sms-gateway";

    private final String gatewayUrl;
    private final String senderId;

    public SMSNotificationStrategy(
            @Value("${fintrack.notification.sms.gateway-url:http://localhost:9999/sms}") String gatewayUrl,
            @Value("${fintrack.notification.sms.sender-id:FINTRACK}") String senderId) {
        this.gatewayUrl = gatewayUrl;
        this.senderId = senderId;
    }

    @Override
    public NotificationChannel channel() { return NotificationChannel.SMS; }

    @Override
    public DispatchResult send(Notification n) {
        log.info("[STUB] SMS gateway={} senderId={} to={} body={}",
                gatewayUrl, senderId, n.getRecipient(), n.getBody());
        return DispatchResult.ok(PROVIDER);
    }
}
