package com.ledger.cqrs.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Final event emitted when money transfer saga fails.
 * Used for notifications and audit trail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyTransferFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID transferId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private BigDecimal amount;
    private String failureReason;
    private boolean compensated;     // Was debit rolled back?
    private Instant failedAt;

    public static MoneyTransferFailedEvent of(UUID transferId, UUID fromAccountId,
            UUID toAccountId, BigDecimal amount, String failureReason, boolean compensated) {
        return MoneyTransferFailedEvent.builder()
            .transferId(transferId)
            .fromAccountId(fromAccountId)
            .toAccountId(toAccountId)
            .amount(amount)
            .failureReason(failureReason)
            .compensated(compensated)
            .failedAt(Instant.now())
            .build();
    }
}
