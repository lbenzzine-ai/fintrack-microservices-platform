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

        assertThat(r.getUuid()).isEqualTo("u-1");
        assertThat(r.getAccountUuid()).isEqualTo("acct-1");
        assertThat(r.getTransactionUuid()).isEqualTo("tx-1");
        assertThat(r.getRecipient()).isEqualTo("to@example.com");
        assertThat(r.getChannel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(r.getTemplate()).isEqualTo("welcome");
        assertThat(r.getSubject()).isEqualTo("Hi");
        assertThat(r.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(r.getDeliveryProvider()).isEqualTo("javamail-smtp");
        assertThat(r.getSentAt()).isEqualTo(now);
        assertThat(r.getCreatedAt()).isEqualTo(now);
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

        assertThat(r.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(r.getFailureReason()).isEqualTo("gateway down");
    }
}
