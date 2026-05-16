package com.fintrack.transaction.entity;

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
 * Transaction aggregate — saga subject. PENDING → INITIATED → DEBITED → COMPLETED, with
 * FAILED/COMPENSATED branches written by the saga consumers.
 *
 * <p>Java 21 keeps the entity as a class — JPA needs a no-arg constructor and mutable fields,
 * so records aren't a fit for the aggregate root itself.
 */
@Entity
@Table(name = "transactions",
        indexes = {
            @Index(name = "ix_tx_from_account", columnList = "from_account_uuid"),
            @Index(name = "ix_tx_to_account",   columnList = "to_account_uuid"),
            @Index(name = "ix_tx_status",       columnList = "status"),
            @Index(name = "ix_tx_type",         columnList = "type"),
            @Index(name = "ix_tx_created_at",   columnList = "created_at")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_tx_uuid", columnNames = "uuid")
        })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "transactions")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Transaction implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", nullable = false, length = 36, updatable = false)
    private String uuid;

    @Column(name = "from_account_uuid", nullable = false, length = 36)
    private String fromAccountUuid;

    @Column(name = "to_account_uuid", length = 36)
    private String toAccountUuid;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal fee;

    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TransactionStatus status;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

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
        if (status == null) status = TransactionStatus.PENDING;
        if (fee == null) fee = BigDecimal.ZERO;
    }
}
