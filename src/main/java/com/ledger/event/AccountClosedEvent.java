package com.ledger.event;

import lombok.Getter;

import java.util.UUID;

@Getter
public class AccountClosedEvent extends DomainEvent {

    private final UUID accountId;
    private final String reason;

    public AccountClosedEvent(UUID accountId, String reason) {
        super();
        this.accountId = accountId;
        this.reason = reason;
    }
}
