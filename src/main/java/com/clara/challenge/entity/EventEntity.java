package com.clara.challenge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.springframework.data.domain.Persistable;

/**
 * JPA entity representing an immutable event record in the append-only event log.
 *
 * <p>Mapped to {@code clarops_challenge_schema.events}. Each {@code POST /events} inserts
 * one row — events are never updated or deleted. This table serves as the idempotency
 * source of truth and the complete audit trail for every received event.</p>
 *
 * <h3>Idempotency via {@code Persistable.isNew()}</h3>
 * <p>This entity implements {@link Persistable}{@code <String>} with {@code isNew()}
 * returning {@code true}. This forces Spring Data JPA to use {@code persist()} (INSERT)
 * instead of {@code merge()} (INSERT OR UPDATE). Without this, entities with
 * manually-assigned PKs would silently overwrite existing rows, bypassing the
 * application-layer idempotency check.</p>
 *
 * <h3>JSONB metadata mapping</h3>
 * <p>The {@code metadata} field is stored as a {@code String} in Java but persisted as
 * {@code JSONB} in PostgreSQL. Serialization from {@code Map<String, Object>} to JSON
 * string is done by {@code WatchdogService.serializeMetadata()} using Jackson 3.x.
 * Hibernate's {@code @ColumnTransformer(write = "?::jsonb")} casts the string to the
 * database's JSONB type during INSERT/UPDATE.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(schema = "clarops_challenge_schema", name = "events")
public class EventEntity implements Persistable<String> {

  /** Caller-provided unique identifier of the event (primary key and idempotency key). */
  @Id
  @Column(name = "event_id", nullable = false, length = 255)
  private String eventId;

  /** Reference to the parent trace. Foreign key to {@code traces(trace_id)} with ON DELETE CASCADE. */
  @Column(name = "trace_id", nullable = false, length = 255)
  private String traceId;

  /** Name of the action that produced this event (e.g. {@code APPLICATION_RECEIVED}). */
  @Column(name = "event_name", nullable = false, length = 255)
  private String eventName;

  /** Outcome of this individual event: {@code SUCCESS} or {@code ERROR}. Event-level, not trace-level. */
  @Column(nullable = false, length = 50)
  private String result;

  /** Date and time when the event occurred, as reported by the origin system. */
  @Column(name = "occurred_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
  private Instant occurredAt;

  /** Server-side timestamp captured when the event was received ({@code Instant.now()}). Used for TTL calculation. */
  @Column(name = "received_at", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
  private Instant receivedAt;

  /** Name of the next expected event from this event's payload, or {@code null} if not specified. */
  @Column(name = "next_expected_event", length = 255)
  private String nextExpectedEvent;

  /** TTL in seconds for the next expected event, or {@code null} if not specified. */
  @Column(name = "next_event_ttl_seconds")
  private Integer nextEventTtlSeconds;

  /** Whether this event completes the flow. Defaults to {@code false}. */
  @Column(name = "final_event", nullable = false)
  @Builder.Default
  private Boolean finalEvent = false;

  /** Flexible key-value metadata stored as JSONB. Serialized from {@code Map<String, Object>} to JSON string before persistence. */
  @ColumnTransformer(write = "?::jsonb")
  @Column(name = "metadata", columnDefinition = "JSONB")
  private String metadata;

  /**
   * Returns the entity's primary key for {@link Persistable} contract.
   *
   * @return the {@code eventId}
   */
  @Override
  @Transient
  public String getId() {
    return eventId;
  }

  /**
   * Always returns {@code true} to force {@code persist()} semantics.
   *
   * <p>Without this, Spring Data JPA would use {@code merge()} for manually-assigned IDs,
   * potentially overwriting existing events instead of throwing a constraint violation.</p>
   *
   * @return always {@code true}
   */
  @Override
  @Transient
  public boolean isNew() {
    return true;
  }
}
