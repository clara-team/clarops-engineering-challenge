package com.clara.challenge.events;

import com.clara.challenge.trace.TraceState;
import com.clara.challenge.trace.TraceStateRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventsController {

  private final ReceivedEventRepository events;
  private final TraceStateRepository traces;

  @PostMapping
  @Transactional
  public ResponseEntity<Void> receive(@Valid @RequestBody PostEventRequest request) {
    boolean isFinal = Boolean.TRUE.equals(request.finalEvent());

    events.save(
        new ReceivedEvent(
            request.eventId(),
            request.traceId(),
            request.eventName(),
            request.result(),
            request.occurredAt(),
            request.nextExpectedEvent(),
            request.nextEventTtlSeconds(),
            isFinal));

    TraceState trace = traces.findById(request.traceId()).orElseGet(TraceState::new);
    trace.setTraceId(request.traceId());
    trace.setLastEventName(request.eventName());
    trace.setLastEventResult(request.result());
    trace.setLastOccurredAt(request.occurredAt());
    trace.setNextExpectedEvent(request.nextExpectedEvent());
    trace.setNextExpectedBefore(deadlineOf(request));
    trace.setCompletedAt(isFinal ? request.occurredAt() : null);
    trace.setEventsReceived(trace.getEventsReceived() + 1);
    traces.save(trace);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  private Instant deadlineOf(PostEventRequest request) {
    if (request.nextExpectedEvent() == null || request.nextEventTtlSeconds() == null) {
      return null;
    }
    return request.occurredAt().plusSeconds(request.nextEventTtlSeconds());
  }
}
