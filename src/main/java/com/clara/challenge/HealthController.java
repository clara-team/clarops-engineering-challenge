package com.clara.challenge;

import com.clara.challenge.health.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pre-existing health-check endpoint for database connectivity verification.
 *
 * <p>Reads the first row from the {@code clarops_challenge_schema.health} table, seeded
 * by the DDL initialization script. Returns the stored message or {@code "unavailable"}
 * if the table is empty. Outside the challenge scope — serves as a smoke test that the
 * application can reach PostgreSQL.</p>
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

  private final HealthService healthService;

  /**
   * Returns the database health message.
   *
   * @return the message from the health table, or {@code "unavailable"} if empty
   */
  @GetMapping("/health")
  public String index() {
    return healthService.getMessage();
  }
}
