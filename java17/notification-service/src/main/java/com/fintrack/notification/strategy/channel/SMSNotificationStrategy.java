package com.fintrack.notification.strategy.channel;

import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SMS delivery — stubbed against a generic gateway URL because real Twilio/Vonage credentials
 * vary per deployment. Replace {@link #dispatch} with the SDK call for the chosen provider.
 */
@Slf4j
@Component
public class SMSNotificationStrategy implements NotificationStrategy {

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
        boolean ok = dispatch(n);
        if (ok) {
            log.debug("SMS sent to={} senderId={} gateway={}", n.getRecipient(), senderId, gatewayUrl);
            return DispatchResult.builder().delivered(true).provider(PROVIDER).build();
        }
        return DispatchResult.builder().delivered(false).provider(PROVIDER)
                .failureReason("gateway returned non-2xx").build();
    }

    private boolean dispatch(Notification n) {
        // Stub: in production swap this for a Twilio/Vonage/SNS call.
        log.info("[STUB] SMS gateway={} senderId={} to={} body={}",
                gatewayUrl, senderId, n.getRecipient(), n.getBody());
        return true;
    }
}
