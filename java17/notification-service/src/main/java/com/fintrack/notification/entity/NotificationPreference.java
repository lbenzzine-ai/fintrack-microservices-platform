package com.fintrack.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/** Per-user opt-in flags. A missing row implies all channels are enabled (legacy default). */
@Entity
@Table(name = "notification_preferences",
        indexes = @Index(name = "ix_notif_pref_user", columnList = "user_uuid"),
        uniqueConstraints = @UniqueConstraint(name = "uk_notif_pref_user", columnNames = "user_uuid"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationPreference implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uuid", nullable = false, length = 36)
    private String userUuid;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Column(name = "email_address", length = 255)
    private String emailAddress;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(name = "push_token", length = 255)
    private String pushToken;
}
