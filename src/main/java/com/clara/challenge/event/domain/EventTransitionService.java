package com.clara.challenge.event.domain;

import java.time.Instant;
import java.util.Objects;

public class EventTransitionService {

  public TransitionResult applyFirstEvent(IncomingEvent event) {
    Objects.requireNonNull(event, "event is required");

    return transitionFromAcceptedEvent(event, 1, reasonForAcceptedEvent(event, true));
  }

  public TransitionResult applyNextEvent(TraceState currentState, IncomingEvent event) {
    Objects.requireNonNull(currentState, "currentState is required");
    Objects.requireNonNull(event, "event is required");

    if (!currentState.traceId().equals(event.traceId())) {
      throw new EventConflictException("Event traceId does not match current trace state");
    }

    if (currentState.status() == TraceStatus.COMPLETED) {
      throw new EventConflictException("Completed traces cannot accept new events");
    }

    if (currentState.status() == TraceStatus.TTL_EXPIRED_FOR_EVENT) {
      throw new EventConflictException("Expired traces cannot accept new events");
    }

    if (currentState.status() == TraceStatus.WAITING_OTHER_EVENT) {
      requireExpectedEvent(currentState, event);
      requireEventBeforeTtl(currentState, event);
    }

    return transitionFromAcceptedEvent(
        event, currentState.eventsReceived() + 1, reasonForAcceptedEvent(event, false));
  }

  public TransitionResult expireWaitingTrace(TraceState currentState, Instant expiredAt) {
    Objects.requireNonNull(currentState, "currentState is required");
    Objects.requireNonNull(expiredAt, "expiredAt is required");

    if (currentState.status() != TraceStatus.WAITING_OTHER_EVENT) {
      throw new EventConflictException("Only waiting traces can expire");
    }

    if (!expiredAt.isAfter(currentState.nextExpectedBefore())) {
      throw new EventConflictException("Waiting trace cannot expire before the TTL deadline");
    }

    TraceState expiredState =
        new TraceState(
            currentState.traceId(),
            TraceStatus.TTL_EXPIRED_FOR_EVENT,
            currentState.lastEventId(),
            currentState.lastEventName(),
            currentState.lastEventResult(),
            currentState.lastEventOccurredAt(),
            currentState.nextExpectedEvent(),
            currentState.nextExpectedBefore(),
            currentState.eventsReceived(),
            null,
            expiredAt);

    return new TransitionResult(expiredState, TransitionReason.TTL_EXPIRED);
  }

  private void requireExpectedEvent(TraceState currentState, IncomingEvent event) {
    if (!Objects.equals(currentState.nextExpectedEvent(), event.eventName())) {
      throw new EventConflictException(
          "Unexpected event. Expected " + currentState.nextExpectedEvent());
    }
  }

  private void requireEventBeforeTtl(TraceState currentState, IncomingEvent event) {
    if (event.occurredAt().isAfter(currentState.nextExpectedBefore())) {
      throw new EventConflictException("Expected event arrived after the TTL deadline");
    }
  }

  private TransitionResult transitionFromAcceptedEvent(
      IncomingEvent event, int eventsReceived, TransitionReason reason) {
    TraceStatus nextStatus = statusFor(event);
    Instant completedAt = nextStatus == TraceStatus.COMPLETED ? event.occurredAt() : null;

    TraceState nextState =
        new TraceState(
            event.traceId(),
            nextStatus,
            event.eventId(),
            event.eventName(),
            event.result(),
            event.occurredAt(),
            event.nextExpectedEvent(),
            event.nextExpectedBefore(),
            eventsReceived,
            completedAt,
            null);

    return new TransitionResult(nextState, reason);
  }

  private TraceStatus statusFor(IncomingEvent event) {
    if (event.finalEvent()) {
      return TraceStatus.COMPLETED;
    }

    if (event.definesNextExpectedEvent()) {
      return TraceStatus.WAITING_OTHER_EVENT;
    }

    return TraceStatus.STARTED;
  }

  private TransitionReason reasonForAcceptedEvent(IncomingEvent event, boolean firstEvent) {
    if (event.finalEvent()) {
      return TransitionReason.FINAL_EVENT_RECEIVED;
    }

    if (event.definesNextExpectedEvent()) {
      return TransitionReason.NEXT_EVENT_EXPECTED;
    }

    if (firstEvent) {
      return TransitionReason.TRACE_CREATED;
    }

    return TransitionReason.EXPECTED_EVENT_RECEIVED;
  }
}
