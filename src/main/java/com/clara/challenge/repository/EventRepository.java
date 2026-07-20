package com.clara.challenge.repository;

import com.clara.challenge.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link EventEntity}.
 *
 * <p>Provides standard CRUD operations on the {@code clarops_challenge_schema.events}
 * table. The critical operation is {@code existsById(eventId)}, called at the start of
 * every {@code POST /events} to enforce idempotency before any trace mutation occurs.</p>
 *
 * <p>Used by {@code WatchdogService} for:</p>
 * <ul>
 *   <li>{@code existsById} — idempotency check (returns true if event was already processed)</li>
 *   <li>{@code save} / {@code saveAndFlush} — persist the immutable event record</li>
 * </ul>
 */
public interface EventRepository extends JpaRepository<EventEntity, String> {}
