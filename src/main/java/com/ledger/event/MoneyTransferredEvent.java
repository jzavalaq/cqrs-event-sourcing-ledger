package com.ledger.event;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class MoneyTransferredEvent extends DomainEvent {

    private final UUID fromAccountId;
    private final UUID toAccountId;
    private final BigDecimal amount;
    private final String description;
    private final UUID transactionId;

    public MoneyTransferredEvent(UUID fromAccountId, UUID toAccountId, BigDecimal amount,
                                 String description, UUID transactionId) {
        super();
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.description = description;
        this.transactionId = transactionId;
    }
}
