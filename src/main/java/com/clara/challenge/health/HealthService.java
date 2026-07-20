package com.clara.challenge.health;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Pre-existing service for the health-check endpoint.
 *
 * <p>Reads the first row from the {@code clarops_challenge_schema.health}
 * table and returns its message. Falls back to {@code "unavailable"} if the
 * table is empty (e.g. before the DDL init script has run).</p>
 *
 * <p>Outside the challenge scope — provided with the base project as a database
 * connectivity smoke test.</p>
 */
@Service
@RequiredArgsConstructor
public class HealthService {

  private final HealthRepository healthRepository;

  /**
   * Returns the database health-check message.
   *
   * @return the message from the first health table row, or {@code "unavailable"} if empty
   */
  public String getMessage() {
    return healthRepository.findAll().stream()
        .findFirst()
        .map(Health::getMessage)
        .orElse("unavailable");
  }
}
