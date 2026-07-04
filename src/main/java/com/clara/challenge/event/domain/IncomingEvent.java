package com.clara.challenge.event.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record IncomingEvent(
    String eventId,
    String traceId,
    String eventName,
    EventResult result,
    Instant occurredAt,
    String nextExpectedEvent,
    Integer nextEventTtlSeconds,
    boolean finalEvent,
    Map<String, Object> metadata) {

  public IncomingEvent {
    Objects.requireNonNull(eventId, "eventId is required");
    Objects.requireNonNull(traceId, "traceId is required");
    Objects.requireNonNull(eventName, "eventName is required");
    Objects.requireNonNull(result, "result is required");
    Objects.requireNonNull(occurredAt, "occurredAt is required");

    if ((nextExpectedEvent == null) != (nextEventTtlSeconds == null)) {
      throw new IllegalArgumentException(
          "nextExpectedEvent and nextEventTtlSeconds must be provided together");
    }

    if (nextExpectedEvent != null && nextExpectedEvent.isBlank()) {
      throw new IllegalArgumentException("nextExpectedEvent must not be blank");
    }

    if (nextEventTtlSeconds != null && nextEventTtlSeconds <= 0) {
      throw new IllegalArgumentException("nextEventTtlSeconds must be positive");
    }

    if (finalEvent && nextExpectedEvent != null) {
      throw new IllegalArgumentException("final events cannot define a next expected event");
    }

    metadata =
        metadata == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
  }

  public boolean definesNextExpectedEvent() {
    return nextExpectedEvent != null && nextEventTtlSeconds != null;
  }

  public Instant nextExpectedBefore() {
    if (!definesNextExpectedEvent()) {
      return null;
    }

    return occurredAt.plusSeconds(nextEventTtlSeconds);
  }
}
