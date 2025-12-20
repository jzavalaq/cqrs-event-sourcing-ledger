package com.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private UUID eventId;
    private UUID aggregateId;
    private String aggregateType;
    private String eventType;
    private String eventData;
    private Instant occurredAt;
}
