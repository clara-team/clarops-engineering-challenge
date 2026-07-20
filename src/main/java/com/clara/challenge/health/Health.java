package com.clara.challenge.health;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * Pre-existing JPA entity for database connectivity verification.
 *
 * <p>Mapped to {@code clarops_challenge_schema.health}. The table is seeded with
 * a row by the DDL initialization script. The {@code /health} endpoint reads this
 * table to confirm the application can reach PostgreSQL.</p>
 *
 * <p>This class is outside the challenge scope — it was provided with the base project
 * as a smoke test for the database connection.</p>
 */
@Getter
@Entity
@Table(schema = "clarops_challenge_schema", name = "health")
public class Health {

  /** Auto-generated primary key. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The health-check message seeded by the DDL initialization script. */
  @Column(nullable = false)
  private String message;
}
