package com.clara.challenge.event.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DuplicateEventComparatorTest {

  private static final Instant OCCURRED_AT = Instant.parse("2026-01-01T10:00:00Z");

  private final DuplicateEventComparator comparator = new DuplicateEventComparator();

  @Test
  void shouldReturnEquivalentDuplicate_WhenEventIdAndPayloadMatch() {
    IncomingEvent existingEvent =
        event("event-1", "trace-1", "payment-created", Map.of("source", "checkout"));
    IncomingEvent incomingEvent =
        event("event-1", "trace-1", "payment-created", Map.of("source", "checkout"));

    DuplicateEventComparison comparison = comparator.compare(existingEvent, incomingEvent);

    assertThat(comparison).isEqualTo(DuplicateEventComparison.EQUIVALENT_DUPLICATE);
  }

  @Test
  void shouldReturnConflictingDuplicate_WhenEventIdMatchesButPayloadDiffers() {
    IncomingEvent existingEvent =
        event("event-1", "trace-1", "payment-created", Map.of("amount", 100));
    IncomingEvent incomingEvent =
        event("event-1", "trace-1", "payment-created", Map.of("amount", 200));

    DuplicateEventComparison comparison = comparator.compare(existingEvent, incomingEvent);

    assertThat(comparison).isEqualTo(DuplicateEventComparison.CONFLICTING_DUPLICATE);
  }

  @Test
  void shouldReturnDifferentEvent_WhenEventIdDiffers() {
    IncomingEvent existingEvent =
        event("event-1", "trace-1", "payment-created", Map.of("source", "checkout"));
    IncomingEvent incomingEvent =
        event("event-2", "trace-1", "payment-created", Map.of("source", "checkout"));

    DuplicateEventComparison comparison = comparator.compare(existingEvent, incomingEvent);

    assertThat(comparison).isEqualTo(DuplicateEventComparison.DIFFERENT_EVENT);
  }

  private static IncomingEvent event(
      String eventId, String traceId, String eventName, Map<String, Object> metadata) {
    return new IncomingEvent(
        eventId, traceId, eventName, EventResult.SUCCESS, OCCURRED_AT, null, null, false, metadata);
  }
}
