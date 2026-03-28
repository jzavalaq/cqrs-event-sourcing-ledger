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
 * Emitted when a debit operation fails during money transfer.
 * Triggers saga compensation (no action needed - debit never happened).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebitFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID accountId;
    private BigDecimal attemptedAmount;
    private String reason;           // "INSUFFICIENT_FUNDS", "ACCOUNT_INACTIVE", etc.
    private UUID transferId;         // Links to saga
    private Instant occurredAt;

    public static DebitFailedEvent of(UUID accountId, BigDecimal attemptedAmount,
            String reason, UUID transferId) {
        return DebitFailedEvent.builder()
            .accountId(accountId)
            .attemptedAmount(attemptedAmount)
            .reason(reason)
            .transferId(transferId)
            .occurredAt(Instant.now())
            .build();
    }
}
