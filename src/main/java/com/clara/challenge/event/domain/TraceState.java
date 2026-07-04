package com.clara.challenge.event.domain;

import java.time.Instant;
import java.util.Objects;

public record TraceState(
    String traceId,
    TraceStatus status,
    String lastEventId,
    String lastEventName,
    EventResult lastEventResult,
    Instant lastEventOccurredAt,
    String nextExpectedEvent,
    Instant nextExpectedBefore,
    int eventsReceived,
    Instant completedAt,
    Instant expiredAt) {

  public TraceState {
    Objects.requireNonNull(traceId, "traceId is required");
    Objects.requireNonNull(status, "status is required");
    Objects.requireNonNull(lastEventId, "lastEventId is required");
    Objects.requireNonNull(lastEventName, "lastEventName is required");
    Objects.requireNonNull(lastEventResult, "lastEventResult is required");
    Objects.requireNonNull(lastEventOccurredAt, "lastEventOccurredAt is required");

    if (eventsReceived < 1) {
      throw new IllegalArgumentException("eventsReceived must be greater than zero");
    }

    if (status == TraceStatus.WAITING_OTHER_EVENT
        && (nextExpectedEvent == null || nextExpectedBefore == null)) {
      throw new IllegalArgumentException("waiting traces require next expected event details");
    }

    if (status == TraceStatus.COMPLETED && completedAt == null) {
      throw new IllegalArgumentException("completed traces require completedAt");
    }

    if (status == TraceStatus.TTL_EXPIRED_FOR_EVENT && expiredAt == null) {
      throw new IllegalArgumentException("expired traces require expiredAt");
    }
  }
}
