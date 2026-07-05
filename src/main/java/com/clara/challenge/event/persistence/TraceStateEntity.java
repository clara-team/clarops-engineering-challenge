package com.clara.challenge.event.persistence;

import com.clara.challenge.event.domain.EventResult;
import com.clara.challenge.event.domain.TraceState;
import com.clara.challenge.event.domain.TraceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "clarops_challenge_schema", name = "trace_state")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TraceStateEntity {

  @Id
  @Column(name = "trace_id", nullable = false, length = 120)
  private String traceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private TraceStatus status;

  @Column(name = "last_event_id", nullable = false, length = 120)
  private String lastEventId;

  @Column(name = "last_event_name", nullable = false, length = 120)
  private String lastEventName;

  @Enumerated(EnumType.STRING)
  @Column(name = "last_event_result", nullable = false, length = 20)
  private EventResult lastEventResult;

  @Column(name = "last_event_occurred_at", nullable = false)
  private Instant lastEventOccurredAt;

  @Column(name = "next_expected_event", length = 120)
  private String nextExpectedEvent;

  @Column(name = "next_expected_before")
  private Instant nextExpectedBefore;

  @Column(name = "events_received", nullable = false)
  private int eventsReceived;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "expired_at")
  private Instant expiredAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public static TraceStateEntity from(TraceState state) {
    TraceStateEntity entity = new TraceStateEntity();
    entity.apply(state);
    return entity;
  }

  public void apply(TraceState state) {
    traceId = state.traceId();
    status = state.status();
    lastEventId = state.lastEventId();
    lastEventName = state.lastEventName();
    lastEventResult = state.lastEventResult();
    lastEventOccurredAt = state.lastEventOccurredAt();
    nextExpectedEvent = state.nextExpectedEvent();
    nextExpectedBefore = state.nextExpectedBefore();
    eventsReceived = state.eventsReceived();
    completedAt = state.completedAt();
    expiredAt = state.expiredAt();
  }

  public TraceState toDomain() {
    return new TraceState(
        traceId,
        status,
        lastEventId,
        lastEventName,
        lastEventResult,
        lastEventOccurredAt,
        nextExpectedEvent,
        nextExpectedBefore,
        eventsReceived,
        completedAt,
        expiredAt);
  }

  @PrePersist
  void setCreatedAndUpdatedAt() {
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  void setUpdatedAt() {
    updatedAt = Instant.now();
  }
}
