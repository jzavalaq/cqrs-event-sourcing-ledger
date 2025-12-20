package com.ledger.repository;

import com.ledger.event.StoredEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventStoreRepository extends JpaRepository<StoredEvent, Long> {

    List<StoredEvent> findByAggregateIdOrderByOccurredAtAsc(UUID aggregateId);

    Page<StoredEvent> findAllByOrderByOccurredAtDesc(Pageable pageable);

    boolean existsByEventId(UUID eventId);
}
