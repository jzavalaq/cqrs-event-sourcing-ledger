package com.ledger.event;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Stored event for event sourcing persistence.
 * All domain events are stored as immutable records in the event store.
 */
@Entity
@Table(name = "stored_events", indexes = {
    @Index(name = "idx_events_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_events_occurred_at", columnList = "occurred_at"),
    @Index(name = "idx_events_type", columnList = "event_type")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false)
    private String aggregateType;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_data", columnDefinition = "TEXT", nullable = false)
    private String eventData;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Version
    private Long version;
}
