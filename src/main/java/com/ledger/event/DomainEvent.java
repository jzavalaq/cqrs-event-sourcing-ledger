package com.ledger.event;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.Instant;
import java.util.UUID;

/**
 * Base class for all domain events in the event sourcing system.
 */
@Getter
public abstract class DomainEvent {

    @Transient
    private final UUID eventId;
    @Transient
    private final Instant occurredAt;
    @Transient
    private final String eventType;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredAt = Instant.now();
        this.eventType = this.getClass().getSimpleName();
    }
}
