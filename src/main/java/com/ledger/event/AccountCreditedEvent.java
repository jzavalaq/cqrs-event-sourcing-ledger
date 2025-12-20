package com.ledger.event;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class AccountCreditedEvent extends DomainEvent {

    private final UUID accountId;
    private final BigDecimal amount;
    private final BigDecimal newBalance;
    private final String description;
    private final UUID transactionId;

    public AccountCreditedEvent(UUID accountId, BigDecimal amount, BigDecimal newBalance,
                                String description, UUID transactionId) {
        super();
        this.accountId = accountId;
        this.amount = amount;
        this.newBalance = newBalance;
        this.description = description;
        this.transactionId = transactionId;
    }
}
