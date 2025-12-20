package com.ledger.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Account projection for the read side of CQRS.
 * Optimized for queries.
 */
@Entity
@Table(name = "account_projections")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountProjection {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "account_holder", nullable = false)
    private String accountHolder;

    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Account.AccountStatus status;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;
}
