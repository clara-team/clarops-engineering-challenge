package com.clara.challenge.event.domain;

import java.util.Objects;

public class DuplicateEventComparator {

  public DuplicateEventComparison compare(
      IncomingEvent existingEvent, IncomingEvent incomingEvent) {
    Objects.requireNonNull(existingEvent, "existingEvent is required");
    Objects.requireNonNull(incomingEvent, "incomingEvent is required");

    if (!existingEvent.eventId().equals(incomingEvent.eventId())) {
      return DuplicateEventComparison.DIFFERENT_EVENT;
    }

    if (hasEquivalentPayload(existingEvent, incomingEvent)) {
      return DuplicateEventComparison.EQUIVALENT_DUPLICATE;
    }

    return DuplicateEventComparison.CONFLICTING_DUPLICATE;
  }

  private boolean hasEquivalentPayload(IncomingEvent existingEvent, IncomingEvent incomingEvent) {
    return existingEvent.traceId().equals(incomingEvent.traceId())
        && existingEvent.eventName().equals(incomingEvent.eventName())
        && existingEvent.result() == incomingEvent.result()
        && existingEvent.occurredAt().equals(incomingEvent.occurredAt())
        && Objects.equals(existingEvent.nextExpectedEvent(), incomingEvent.nextExpectedEvent())
        && Objects.equals(existingEvent.nextEventTtlSeconds(), incomingEvent.nextEventTtlSeconds())
        && existingEvent.finalEvent() == incomingEvent.finalEvent()
        && existingEvent.metadata().equals(incomingEvent.metadata());
  }
}
