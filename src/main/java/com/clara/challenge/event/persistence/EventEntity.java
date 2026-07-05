package com.clara.challenge.event.persistence;

import com.clara.challenge.event.domain.EventResult;
import com.clara.challenge.event.domain.IncomingEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(schema = "clarops_challenge_schema", name = "events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventEntity {

  @Id private UUID id;

  @Column(name = "event_id", nullable = false, unique = true, length = 120)
  private String eventId;

  @Column(name = "trace_id", nullable = false, length = 120)
  private String traceId;

  @Column(name = "event_name", nullable = false, length = 120)
  private String eventName;

  @Enumerated(EnumType.STRING)
  @Column(name = "result", nullable = false, length = 20)
  private EventResult result;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "next_expected_event", length = 120)
  private String nextExpectedEvent;

  @Column(name = "next_event_ttl_seconds")
  private Integer nextEventTtlSeconds;

  @Column(name = "final_event", nullable = false)
  private boolean finalEvent;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public static EventEntity from(IncomingEvent event, Instant receivedAt) {
    EventEntity entity = new EventEntity();
    entity.id = UUID.randomUUID();
    entity.eventId = event.eventId();
    entity.traceId = event.traceId();
    entity.eventName = event.eventName();
    entity.result = event.result();
    entity.occurredAt = event.occurredAt();
    entity.receivedAt = receivedAt;
    entity.nextExpectedEvent = event.nextExpectedEvent();
    entity.nextEventTtlSeconds = event.nextEventTtlSeconds();
    entity.finalEvent = event.finalEvent();
    entity.metadata = event.metadata();
    return entity;
  }

  public IncomingEvent toDomain() {
    return new IncomingEvent(
        eventId,
        traceId,
        eventName,
        result,
        occurredAt,
        nextExpectedEvent,
        nextEventTtlSeconds,
        finalEvent,
        metadata);
  }

  @PrePersist
  void setCreatedAt() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
