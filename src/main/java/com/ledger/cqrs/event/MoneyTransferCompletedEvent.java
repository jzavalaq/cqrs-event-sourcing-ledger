package com.ledger.cqrs.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Event: A money transfer has completed successfully.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyTransferCompletedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID transferId;
    private UUID fromAccountId;
    private UUID toAccountId;
    private Instant occurredAt;

    public static MoneyTransferCompletedEvent of(UUID transferId, UUID fromAccountId, UUID toAccountId) {
        return MoneyTransferCompletedEvent.builder()
            .transferId(transferId)
            .fromAccountId(fromAccountId)
            .toAccountId(toAccountId)
            .occurredAt(Instant.now())
            .build();
    }
}
