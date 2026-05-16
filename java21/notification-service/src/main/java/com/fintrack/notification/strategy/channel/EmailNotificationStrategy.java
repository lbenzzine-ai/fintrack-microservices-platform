package com.fintrack.notification.strategy.channel;

import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public final class EmailNotificationStrategy implements NotificationStrategy {

    public static final String PROVIDER = "javamail-smtp";

    private final JavaMailSender mailSender;

    @Value("${fintrack.notification.email.from:no-reply@fintrack.local}")
    private String from;

    @Override
    public NotificationChannel channel() { return NotificationChannel.EMAIL; }

    @Override
    public DispatchResult send(Notification n) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(n.getRecipient());
        msg.setSubject(n.getSubject() == null ? "FinTrack notification" : n.getSubject());
        msg.setText(n.getBody() == null ? "" : n.getBody());
        try {
            mailSender.send(msg);
            log.debug("Email sent to={} subject={}", n.getRecipient(), msg.getSubject());
            return DispatchResult.ok(PROVIDER);
        } catch (Exception ex) {
            log.warn("Email send failed to={}: {}", n.getRecipient(), ex.getMessage());
            return DispatchResult.fail(PROVIDER, ex.getMessage());
        }
    }
}
