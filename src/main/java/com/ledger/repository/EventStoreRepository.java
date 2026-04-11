package com.ledger.repository;

import com.ledger.event.StoredEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for StoredEvent entities (Event Store).
 * <p>
 * Provides access to the event sourcing event store for replay and audit purposes.
 * </p>
 */
@Repository
public interface EventStoreRepository extends JpaRepository<StoredEvent, Long> {

    /**
     * Finds all events for an aggregate, ordered by occurrence time (oldest first).
     *
     * @param aggregateId the aggregate ID
     * @return list of events in chronological order
     */
    List<StoredEvent> findByAggregateIdOrderByOccurredAtAsc(UUID aggregateId);

    /**
     * Finds all events with pagination, ordered by occurrence time (newest first).
     *
     * @param pageable pagination parameters
     * @return paginated events
     */
    Page<StoredEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);

    /**
     * Checks if an event exists with the given event ID.
     *
     * @param eventId the event ID
     * @return true if the event exists
     */
    boolean existsByEventId(UUID eventId);
}
