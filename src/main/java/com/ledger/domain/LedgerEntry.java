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
 * Ledger entry representing a single transaction record.
 * This is part of the read-side projection in CQRS.
 */
@Entity
@Table(name = "ledger_entries", indexes = {
    @Index(name = "idx_ledger_account_id", columnList = "account_id"),
    @Index(name = "idx_ledger_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_id", nullable = false, unique = true)
    private UUID entryId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "entry_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EntryType entryType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "description")
    private String description;

    @Column(name = "related_account_id")
    private UUID relatedAccountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private Long version;

    public enum EntryType {
        CREDIT, DEBIT, TRANSFER_IN, TRANSFER_OUT
    }
}
