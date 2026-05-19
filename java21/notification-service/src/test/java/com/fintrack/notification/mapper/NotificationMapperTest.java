package com.fintrack.notification.mapper;

import com.fintrack.notification.dto.NotificationResponse;
import com.fintrack.notification.entity.Notification;
import com.fintrack.notification.entity.NotificationChannel;
import com.fintrack.notification.entity.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMapperTest {

    private final NotificationMapper mapper = Mappers.getMapper(NotificationMapper.class);

    @Test
    void toResponse_copiesAllFields() {
        Instant now = Instant.now();
        Notification n = Notification.builder()
                .uuid("u-1")
                .accountUuid("acct-1")
                .transactionUuid("tx-1")
                .recipient("to@example.com")
                .channel(NotificationChannel.EMAIL)
                .template("welcome")
                .subject("Hi")
                .body("body-ignored-in-response")
                .status(NotificationStatus.SENT)
                .failureReason(null)
                .deliveryProvider("javamail-smtp")
                .sentAt(now)
                .createdAt(now)
                .build();

        NotificationResponse r = mapper.toResponse(n);

        assertThat(r.uuid()).isEqualTo("u-1");
        assertThat(r.accountUuid()).isEqualTo("acct-1");
        assertThat(r.transactionUuid()).isEqualTo("tx-1");
        assertThat(r.recipient()).isEqualTo("to@example.com");
        assertThat(r.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(r.template()).isEqualTo("welcome");
        assertThat(r.subject()).isEqualTo("Hi");
        assertThat(r.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(r.deliveryProvider()).isEqualTo("javamail-smtp");
        assertThat(r.sentAt()).isEqualTo(now);
        assertThat(r.createdAt()).isEqualTo(now);
    }

    @Test
    void toResponse_nullEntity_returnsNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void toResponse_failedNotification_carriesFailureReason() {
        Notification n = Notification.builder()
                .uuid("u-2")
                .recipient("x")
                .channel(NotificationChannel.SMS)
                .status(NotificationStatus.FAILED)
                .failureReason("gateway down")
                .build();

        NotificationResponse r = mapper.toResponse(n);

        assertThat(r.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(r.failureReason()).isEqualTo("gateway down");
    }
}
