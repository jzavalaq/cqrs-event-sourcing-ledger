package com.ledger.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for event store entries.
 * <p>
 * Represents a single event in the event sourcing system.
 * Events are immutable records of state changes.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Event store entry response")
public class EventResponse {

    @Schema(description = "Database ID of the stored event", example = "1")
    private Long id;

    @Schema(description = "Unique event identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID eventId;

    @Schema(description = "ID of the aggregate this event belongs to", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID aggregateId;

    @Schema(description = "Type of aggregate", example = "Account")
    private String aggregateType;

    @Schema(description = "Event type name", example = "AccountOpenedEvent")
    private String eventType;

    @Schema(description = "JSON representation of the event data", example = "{\"accountId\":\"...\",\"accountNumber\":\"ACC123\"}")
    private String eventData;

    @Schema(description = "When the event occurred", example = "2024-01-20T14:45:30Z")
    private Instant occurredAt;

    @Schema(description = "Version for optimistic locking", example = "1")
    private Long version;
}
