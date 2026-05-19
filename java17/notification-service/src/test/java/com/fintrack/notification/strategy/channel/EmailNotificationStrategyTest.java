package com.fintrack.notification.strategy.channel;

import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationStrategyTest {

    @Mock
    JavaMailSender mailSender;

    EmailNotificationStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new EmailNotificationStrategy(mailSender);
        ReflectionTestUtils.setField(strategy, "from", "from@fintrack.local");
    }

    @Test
    void channel_isEmail() {
        assertThat(strategy.channel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Test
    void send_success_returnsDelivered() {
        Notification n = Notification.builder()
                .recipient("to@example.com")
                .subject("Hi")
                .body("Body")
                .channel(NotificationChannel.EMAIL)
                .build();

        DispatchResult r = strategy.send(n);

        assertThat(r.isDelivered()).isTrue();
        assertThat(r.getProvider()).isEqualTo(EmailNotificationStrategy.PROVIDER);
        assertThat(r.getFailureReason()).isNull();

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getFrom()).isEqualTo("from@fintrack.local");
        assertThat(sent.getTo()).containsExactly("to@example.com");
        assertThat(sent.getSubject()).isEqualTo("Hi");
        assertThat(sent.getText()).isEqualTo("Body");
    }

    @Test
    void send_nullSubjectAndBody_useDefaults() {
        Notification n = Notification.builder()
                .recipient("to@example.com")
                .channel(NotificationChannel.EMAIL)
                .build();

        DispatchResult r = strategy.send(n);

        assertThat(r.isDelivered()).isTrue();

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getSubject()).isEqualTo("FinTrack notification");
        assertThat(captor.getValue().getText()).isEmpty();
    }

    @Test
    void send_failure_returnsFailureReason() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        Notification n = Notification.builder()
                .recipient("to@example.com")
                .subject("S")
                .body("B")
                .channel(NotificationChannel.EMAIL)
                .build();

        DispatchResult r = strategy.send(n);

        assertThat(r.isDelivered()).isFalse();
        assertThat(r.getProvider()).isEqualTo(EmailNotificationStrategy.PROVIDER);
        assertThat(r.getFailureReason()).contains("SMTP down");
    }
}
