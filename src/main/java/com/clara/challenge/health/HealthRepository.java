package com.clara.challenge.health;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Pre-existing Spring Data JPA repository for the health-check table.
 *
 * <p>Provides standard CRUD operations on {@code clarops_challenge_schema.health}.
 * Used by {@link HealthService} to read the database connectivity message.
 * Outside the challenge scope.</p>
 */
public interface HealthRepository extends JpaRepository<Health, Long> {}
