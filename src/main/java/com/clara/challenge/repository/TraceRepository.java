package com.clara.challenge.repository;

import com.clara.challenge.entity.TraceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link TraceEntity}.
 *
 * <p>Provides standard CRUD operations on the {@code clarops_challenge_schema.traces}
 * table. No custom query methods are needed — all access is by the trace's primary key
 * ({@code traceId}).</p>
 *
 * <p>Used by {@code WatchdogService} for:</p>
 * <ul>
 *   <li>{@code findById} — find or create trace state on event ingestion</li>
 *   <li>{@code save} / {@code saveAndFlush} — persist new or updated trace state</li>
 *   <li>{@code existsById} — available but not currently used (traceId existence is checked via findById)</li>
 * </ul>
 */
public interface TraceRepository extends JpaRepository<TraceEntity, String> {}
