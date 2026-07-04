package com.clara.challenge.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventTransitionServiceTest {

  private static final Instant OCCURRED_AT = Instant.parse("2026-01-01T10:00:00Z");

  private final EventTransitionService service = new EventTransitionService();

  @Test
  void shouldCreateStartedTrace_WhenFirstEventHasNoNextExpectedEvent() {
    IncomingEvent event = event("event-1", "payment-created");

    TransitionResult result = service.applyFirstEvent(event);

    assertThat(result.reason()).isEqualTo(TransitionReason.TRACE_CREATED);
    assertThat(result.traceState())
        .extracting(
            TraceState::traceId,
            TraceState::status,
            TraceState::lastEventId,
            TraceState::lastEventName,
            TraceState::lastEventResult,
            TraceState::lastEventOccurredAt,
            TraceState::eventsReceived,
            TraceState::completedAt,
            TraceState::expiredAt)
        .containsExactly(
            "trace-1",
            TraceStatus.STARTED,
            "event-1",
            "payment-created",
            EventResult.SUCCESS,
            OCCURRED_AT,
            1,
            null,
            null);
    assertThat(result.traceState().nextExpectedEvent()).isNull();
    assertThat(result.traceState().nextExpectedBefore()).isNull();
  }

  @Test
  void shouldCreateWaitingTrace_WhenFirstEventDefinesNextExpectedEvent() {
    IncomingEvent event = eventWaitingFor("event-1", "payment-created", "payment-confirmed", 60);

    TransitionResult result = service.applyFirstEvent(event);

    assertThat(result.reason()).isEqualTo(TransitionReason.NEXT_EVENT_EXPECTED);
    assertThat(result.traceState().status()).isEqualTo(TraceStatus.WAITING_OTHER_EVENT);
    assertThat(result.traceState().nextExpectedEvent()).isEqualTo("payment-confirmed");
    assertThat(result.traceState().nextExpectedBefore()).isEqualTo(OCCURRED_AT.plusSeconds(60));
    assertThat(result.traceState().eventsReceived()).isEqualTo(1);
  }

  @Test
  void shouldCreateCompletedTrace_WhenFirstEventIsFinal() {
    IncomingEvent event = finalEvent("event-1", "payment-confirmed");

    TransitionResult result = service.applyFirstEvent(event);

    assertThat(result.reason()).isEqualTo(TransitionReason.FINAL_EVENT_RECEIVED);
    assertThat(result.traceState().status()).isEqualTo(TraceStatus.COMPLETED);
    assertThat(result.traceState().completedAt()).isEqualTo(OCCURRED_AT);
    assertThat(result.traceState().expiredAt()).isNull();
  }

  @Test
  void shouldCreateExpiredTrace_WhenWaitingTraceExpiresAfterTtlDeadline() {
    TraceState waitingState = waitingState();
    Instant expiredAt = OCCURRED_AT.plusSeconds(61);

    TransitionResult result = service.expireWaitingTrace(waitingState, expiredAt);

    assertThat(result.reason()).isEqualTo(TransitionReason.TTL_EXPIRED);
    assertThat(result.traceState().status()).isEqualTo(TraceStatus.TTL_EXPIRED_FOR_EVENT);
    assertThat(result.traceState().expiredAt()).isEqualTo(expiredAt);
    assertThat(result.traceState().completedAt()).isNull();
    assertThat(result.traceState().eventsReceived()).isEqualTo(waitingState.eventsReceived());
  }

  @Test
  void shouldRejectPrematureTtlExpiration_WhenExpiredAtIsAtTtlDeadline() {
    TraceState waitingState = waitingState();

    assertThatThrownBy(
            () -> service.expireWaitingTrace(waitingState, waitingState.nextExpectedBefore()))
        .isInstanceOf(EventConflictException.class)
        .hasMessage("Waiting trace cannot expire before the TTL deadline");
  }

  @Test
  void shouldAdvanceTrace_WhenExpectedEventArrivesBeforeTtlDeadline() {
    IncomingEvent event = eventAt("event-2", "payment-confirmed", OCCURRED_AT.plusSeconds(30));

    TransitionResult result = service.applyNextEvent(waitingState(), event);

    assertThat(result.reason()).isEqualTo(TransitionReason.EXPECTED_EVENT_RECEIVED);
    assertThat(result.traceState().status()).isEqualTo(TraceStatus.STARTED);
    assertThat(result.traceState().lastEventId()).isEqualTo("event-2");
    assertThat(result.traceState().eventsReceived()).isEqualTo(2);
    assertThat(result.traceState().nextExpectedEvent()).isNull();
    assertThat(result.traceState().nextExpectedBefore()).isNull();
  }

  @Test
  void shouldRejectExpectedEvent_WhenEventArrivesAfterTtlDeadline() {
    IncomingEvent event = eventAt("event-2", "payment-confirmed", OCCURRED_AT.plusSeconds(61));

    assertThatThrownBy(() -> service.applyNextEvent(waitingState(), event))
        .isInstanceOf(EventConflictException.class)
        .hasMessage("Expected event arrived after the TTL deadline");
  }

  @Test
  void shouldRejectUnexpectedEvent_WhenWaitingTraceReceivesDifferentEventName() {
    IncomingEvent event = eventAt("event-2", "payment-cancelled", OCCURRED_AT.plusSeconds(30));

    assertThatThrownBy(() -> service.applyNextEvent(waitingState(), event))
        .isInstanceOf(EventConflictException.class)
        .hasMessage("Unexpected event. Expected payment-confirmed");
  }

  @Test
  void shouldRejectNewEvent_WhenTraceIsCompleted() {
    TraceState completedState = completedState();

    assertThatThrownBy(
            () -> service.applyNextEvent(completedState, event("event-2", "payment-refund")))
        .isInstanceOf(EventConflictException.class)
        .hasMessage("Completed traces cannot accept new events");
  }

  @Test
  void shouldRejectNewEvent_WhenTraceIsExpired() {
    TraceState expiredState = expiredState();

    assertThatThrownBy(
            () -> service.applyNextEvent(expiredState, event("event-2", "payment-confirmed")))
        .isInstanceOf(EventConflictException.class)
        .hasMessage("Expired traces cannot accept new events");
  }

  @Test
  void shouldUseAuditSchemaReasons_WhenAcceptingEventsAndExpiringTrace() {
    assertThat(service.applyFirstEvent(event("event-1", "payment-created")).reason())
        .isEqualTo(TransitionReason.TRACE_CREATED);
    assertThat(
            service
                .applyFirstEvent(
                    eventWaitingFor("event-1", "payment-created", "payment-confirmed", 60))
                .reason())
        .isEqualTo(TransitionReason.NEXT_EVENT_EXPECTED);
    assertThat(
            service
                .applyNextEvent(
                    waitingState(),
                    eventAt("event-2", "payment-confirmed", OCCURRED_AT.plusSeconds(30)))
                .reason())
        .isEqualTo(TransitionReason.EXPECTED_EVENT_RECEIVED);
    assertThat(service.applyFirstEvent(finalEvent("event-1", "payment-confirmed")).reason())
        .isEqualTo(TransitionReason.FINAL_EVENT_RECEIVED);
    assertThat(service.expireWaitingTrace(waitingState(), OCCURRED_AT.plusSeconds(61)).reason())
        .isEqualTo(TransitionReason.TTL_EXPIRED);
  }

  @Test
  void shouldExposeAuditSchemaVocabulary_WhenListingTransitionReasons() {
    assertThat(Arrays.stream(TransitionReason.values()).map(Enum::name))
        .containsExactly(
            "TRACE_CREATED",
            "NEXT_EVENT_EXPECTED",
            "EXPECTED_EVENT_RECEIVED",
            "FINAL_EVENT_RECEIVED",
            "TTL_EXPIRED",
            "DUPLICATE_EVENT_IDEMPOTENT");
  }

  @Test
  void shouldPreserveMetadataWithJsonNullValues_WhenCreatingIncomingEvent() {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("nullable", null);
    metadata.put("source", "checkout");

    IncomingEvent event =
        new IncomingEvent(
            "event-1",
            "trace-1",
            "payment-created",
            EventResult.SUCCESS,
            OCCURRED_AT,
            null,
            null,
            false,
            metadata);

    assertThat(event.metadata())
        .containsEntry("nullable", null)
        .containsEntry("source", "checkout");
    assertThatThrownBy(() -> event.metadata().put("new-key", "new-value"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  private static IncomingEvent event(String eventId, String eventName) {
    return eventAt(eventId, eventName, OCCURRED_AT);
  }

  private static IncomingEvent eventAt(String eventId, String eventName, Instant occurredAt) {
    return new IncomingEvent(
        eventId,
        "trace-1",
        eventName,
        EventResult.SUCCESS,
        occurredAt,
        null,
        null,
        false,
        Map.of("source", "checkout"));
  }

  private static IncomingEvent eventWaitingFor(
      String eventId, String eventName, String nextExpectedEvent, int ttlSeconds) {
    return new IncomingEvent(
        eventId,
        "trace-1",
        eventName,
        EventResult.SUCCESS,
        OCCURRED_AT,
        nextExpectedEvent,
        ttlSeconds,
        false,
        Map.of("source", "checkout"));
  }

  private static IncomingEvent finalEvent(String eventId, String eventName) {
    return new IncomingEvent(
        eventId,
        "trace-1",
        eventName,
        EventResult.SUCCESS,
        OCCURRED_AT,
        null,
        null,
        true,
        Map.of("source", "checkout"));
  }

  private static TraceState waitingState() {
    return new TraceState(
        "trace-1",
        TraceStatus.WAITING_OTHER_EVENT,
        "event-1",
        "payment-created",
        EventResult.SUCCESS,
        OCCURRED_AT,
        "payment-confirmed",
        OCCURRED_AT.plusSeconds(60),
        1,
        null,
        null);
  }

  private static TraceState completedState() {
    return new TraceState(
        "trace-1",
        TraceStatus.COMPLETED,
        "event-1",
        "payment-confirmed",
        EventResult.SUCCESS,
        OCCURRED_AT,
        null,
        null,
        1,
        OCCURRED_AT,
        null);
  }

  private static TraceState expiredState() {
    return new TraceState(
        "trace-1",
        TraceStatus.TTL_EXPIRED_FOR_EVENT,
        "event-1",
        "payment-created",
        EventResult.SUCCESS,
        OCCURRED_AT,
        "payment-confirmed",
        OCCURRED_AT.plusSeconds(60),
        1,
        null,
        OCCURRED_AT.plusSeconds(61));
  }
}
