package com.fintrack.account.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;

/**
 * Currency catalogue — small, near-immutable lookup table.
 * Annotated with {@code READ_ONLY} on the second-level cache for maximum throughput
 * (no invalidation overhead).
 */
@Entity
@Table(name = "currencies",
        uniqueConstraints = @UniqueConstraint(name = "uk_currencies_code", columnNames = "code"))
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = "currencies")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Currency implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 3)
    private String code;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "minor_units", nullable = false)
    private int minorUnits;
}
