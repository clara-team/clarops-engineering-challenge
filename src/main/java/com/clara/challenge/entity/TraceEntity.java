package com.clara.challenge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * JPA entity representing the read-optimized, denormalized snapshot of a trace's current state.
 *
 * <p>Mapped to {@code clarops_challenge_schema.traces}. This table provides fast lookups
 * for {@code GET /traces/{traceId}/status} without recalculating state from the full event
 * history. Every {@code POST /events} either creates or mutates one row in this table.</p>
 *
 * <h3>Design rationale</h3>
 * <p>Keeping a separate snapshot table (alongside the append-only {@code events} log)
 * trades write overhead for read efficiency — a single row fetch versus aggregating all
 * events for a trace.</p>
 *
 * <h3>Automatic timestamps</h3>
 * <p>{@code createdAt} and {@code updatedAt} are managed by Hibernate via
 * {@code @CreationTimestamp} and {@code @UpdateTimestamp} — no manual timestamp logic
 * is needed.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(schema = "clarops_challenge_schema", name = "traces")
public class TraceEntity {

  /** Caller-provided unique identifier of the distributed flow (primary key). */
  @Id
  @Column(name = "trace_id", nullable = false, length = 255)
  private String traceId;

  /** Current trace state. Defaults to {@code STARTED} via {@code @Builder.Default}. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  @Builder.Default
  private TraceStatus status = TraceStatus.STARTED;

  /** Name of the most recently processed event for this trace. */
  @Column(name = "last_event_name", nullable = false, length = 255)
  private String lastEventName;

  /** Result of the most recently processed event ({@code SUCCESS} or {@code ERROR}). */
  @Column(name = "last_event_result", nullable = false, length = 50)
  private String lastEventResult;

  /** Name of the next expected event, or {@code null} when no expectation is set. */
  @Column(name = "next_expected_event", length = 255)
  private String nextExpectedEvent;

  /** Absolute deadline (server time) for the next expected event. Computed as {@code received_at + nextEventTtlSeconds}. */
  @Column(name = "next_expected_before", columnDefinition = "TIMESTAMP WITH TIME ZONE")
  private Instant nextExpectedBefore;

  /**
   * Total count of events received for this trace. Starts at 1 and increments on each
   * subsequent event. Never decremented.
   */
  @Column(name = "events_count", nullable = false)
  @Builder.Default
  private Integer eventsCount = 1;

  /** Timestamp of trace creation. Set automatically by Hibernate on insert. Not updatable. */
  @CreationTimestamp
  @Column(
      name = "created_at",
      nullable = false,
      updatable = false,
      columnDefinition = "TIMESTAMP WITH TIME ZONE")
  private Instant createdAt;

  /** Timestamp of the last modification. Refreshed automatically by Hibernate on every update. */
  @UpdateTimestamp
  @Column(
      name = "updated_at",
      nullable = false,
      columnDefinition = "TIMESTAMP WITH TIME ZONE")
  private Instant updatedAt;
}
