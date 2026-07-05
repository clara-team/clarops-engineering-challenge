package com.clara.challenge.event.persistence;

import com.clara.challenge.event.domain.TraceStatus;
import com.clara.challenge.event.domain.TransitionReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(schema = "clarops_challenge_schema", name = "trace_status_audit")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TraceStatusAuditEntity {

  @Id private UUID id;

  @Column(name = "trace_id", nullable = false, length = 120)
  private String traceId;

  @Enumerated(EnumType.STRING)
  @Column(name = "previous_status", length = 40)
  private TraceStatus previousStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "new_status", nullable = false, length = 40)
  private TraceStatus newStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "reason", nullable = false, length = 80)
  private TransitionReason reason;

  @Column(name = "event_id", length = 120)
  private String eventId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public static TraceStatusAuditEntity transition(
      String traceId,
      TraceStatus previousStatus,
      TraceStatus newStatus,
      TransitionReason reason,
      String eventId) {
    TraceStatusAuditEntity entity = new TraceStatusAuditEntity();
    entity.id = UUID.randomUUID();
    entity.traceId = traceId;
    entity.previousStatus = previousStatus;
    entity.newStatus = newStatus;
    entity.reason = reason;
    entity.eventId = eventId;
    return entity;
  }

  @PrePersist
  void setCreatedAt() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
