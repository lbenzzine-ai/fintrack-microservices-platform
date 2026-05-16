package com.fintrack.account.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Account / wallet — owned by exactly one user (1:1 logical). The {@code version} field powers
 * JPA optimistic locking so concurrent debits don't drop balance updates.
 *
 * <p>{@code READ_WRITE} L2 cache region: rows are mutable but read-heavy. With READ_WRITE the
 * cache uses soft locks so concurrent updates don't expose stale balances.
 */
@Entity
@Table(name = "accounts",
        indexes = {
            @Index(name = "ix_accounts_user_uuid", columnList = "user_uuid"),
            @Index(name = "ix_accounts_status",    columnList = "status")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_accounts_uuid",      columnNames = "uuid"),
            @UniqueConstraint(name = "uk_accounts_user_uuid", columnNames = "user_uuid")
        })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "accounts")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Account implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, length = 36, updatable = false)
    private String uuid;

    @Column(name = "user_uuid", nullable = false, length = 36)
    private String userUuid;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private AccountStatus status;

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
        if (balance == null) balance = BigDecimal.ZERO;
        if (status == null) status = AccountStatus.ACTIVE;
        if (currencyCode == null) currencyCode = "USD";
    }
}
