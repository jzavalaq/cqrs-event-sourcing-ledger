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
 * Emitted when a credit operation fails during money transfer.
 * Triggers saga compensation (must reverse the debit).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID accountId;
    private BigDecimal attemptedAmount;
    private String reason;           // "ACCOUNT_CLOSED", "ACCOUNT_NOT_FOUND", etc.
    private UUID transferId;         // Links to saga
    private Instant occurredAt;

    public static CreditFailedEvent of(UUID accountId, BigDecimal attemptedAmount,
            String reason, UUID transferId) {
        return CreditFailedEvent.builder()
            .accountId(accountId)
            .attemptedAmount(attemptedAmount)
            .reason(reason)
            .transferId(transferId)
            .occurredAt(Instant.now())
            .build();
    }
}
