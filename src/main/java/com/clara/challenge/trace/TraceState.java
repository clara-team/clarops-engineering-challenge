package com.clara.challenge.trace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "clarops_challenge_schema", name = "trace_state")
public class TraceState {

  @Id private String traceId;

  @Column(nullable = false)
  private String lastEventName;

  @Column(nullable = false)
  private String lastEventResult;

  @Column(nullable = false)
  private Instant lastOccurredAt;

  private String nextExpectedEvent;

  private Instant nextExpectedBefore;

  private Instant completedAt;

  @Column(nullable = false)
  private int eventsReceived;

  public TraceStatus statusAt(Instant now) {
    if (completedAt != null) {
      return TraceStatus.COMPLETED;
    }
    if (nextExpectedBefore == null) {
      return TraceStatus.STARTED;
    }
    return now.isAfter(nextExpectedBefore)
        ? TraceStatus.TTL_EXPIRED_FOR_EVENT
        : TraceStatus.WAITING_OTHER_EVENT;
  }
}
