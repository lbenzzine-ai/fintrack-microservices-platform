package com.fintrack.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications",
        indexes = {
            @Index(name = "ix_notif_account", columnList = "account_uuid"),
            @Index(name = "ix_notif_status",  columnList = "status"),
            @Index(name = "ix_notif_channel", columnList = "channel"),
            @Index(name = "ix_notif_tx",      columnList = "transaction_uuid"),
            @Index(name = "ix_notif_created", columnList = "created_at")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_notif_uuid", columnNames = "uuid"))
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Notification implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, length = 36, updatable = false)
    private String uuid;

    @Column(name = "account_uuid", length = 36)
    private String accountUuid;

    @Column(name = "transaction_uuid", length = 36)
    private String transactionUuid;

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private NotificationChannel channel;

    @Column(name = "template", length = 64)
    private String template;

    @Column(name = "subject", length = 255)
    private String subject;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private NotificationStatus status;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "delivery_provider", length = 64)
    private String deliveryProvider;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Version
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (uuid == null) uuid = UUID.randomUUID().toString();
        if (status == null) status = NotificationStatus.PENDING;
    }
}
