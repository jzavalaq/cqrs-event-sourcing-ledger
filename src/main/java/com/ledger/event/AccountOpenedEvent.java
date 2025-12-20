package com.ledger.event;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class AccountOpenedEvent extends DomainEvent {

    private final UUID accountId;
    private final String accountNumber;
    private final String accountHolder;
    private final BigDecimal initialBalance;
    private final String currency;

    public AccountOpenedEvent(UUID accountId, String accountNumber, String accountHolder,
                              BigDecimal initialBalance, String currency) {
        super();
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.initialBalance = initialBalance;
        this.currency = currency;
    }
}
