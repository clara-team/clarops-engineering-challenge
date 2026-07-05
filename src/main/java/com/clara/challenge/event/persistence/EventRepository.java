package com.clara.challenge.event.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<EventEntity, UUID> {

  Optional<EventEntity> findByEventId(String eventId);
}
