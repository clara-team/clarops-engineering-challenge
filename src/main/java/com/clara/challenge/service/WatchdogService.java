package com.clara.challenge.service;

import com.clara.challenge.dto.EventRequestDTO;
import com.clara.challenge.dto.TraceStatusResponseDTO;
import com.clara.challenge.entity.EventEntity;
import com.clara.challenge.entity.TraceEntity;
import com.clara.challenge.entity.TraceStatus;
import com.clara.challenge.exception.DuplicateEventException;
import com.clara.challenge.exception.TraceNotFoundException;
import com.clara.challenge.repository.EventRepository;
import com.clara.challenge.repository.TraceRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Core business logic for the Distributed Event Watchdog.
 *
 * <p>Owns the state machine that governs trace lifecycle, enforces idempotency through
 * {@code eventId}, calculates TTL deadlines, and constructs JPA entities. All public
 * methods are {@code @Transactional} — database operations are atomic per request.</p>
 *
 * <p>Stateless — all state lives in PostgreSQL via {@link TraceRepository} and
 * {@link EventRepository}. No in-memory caches, no background schedulers, no locks.</p>
 *
 * <h3>State transition rules</h3>
 * <pre>
 * if (finalEvent)       → COMPLETED
 * else if (expectations) → WAITING_OTHER_EVENT, nextExpectedBefore = now + TTL
 * else                   → STARTED
 * </pre>
 *
 * <p>"Expectations" means both {@code nextExpectedEvent} and {@code nextEventTtlSeconds}
 * are non-null. The {@code result} field (SUCCESS/ERROR) does not affect state transitions.</p>
 *
 * <h3>TTL evaluation</h3>
 * <p>TTL is calculated from {@code received_at} (server clock), not {@code occurredAt}
 * (client clock), to avoid clock-skew. Expiration is evaluated lazily on
 * {@code GET /traces/{traceId}/status} — no background scheduler is needed.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchdogService {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final TraceRepository traceRepository;
  private final EventRepository eventRepository;

  /**
   * Processes an incoming event: idempotency check, trace creation or update, and event persistence.
   *
   * <p>All operations run within a single transaction. The trace is saved and flushed before
   * the event to satisfy the foreign key constraint from {@code events.trace_id} to
   * {@code traces.trace_id}.</p>
   *
   * <h3>Sequence</h3>
   * <ol>
   *   <li>Check idempotency via {@code eventRepository.existsById(eventId)}</li>
   *   <li>Find existing trace or create a new one with the appropriate initial state</li>
   *   <li>Save the trace (flushed to enforce FK constraint)</li>
   *   <li>Build and persist the immutable event entity</li>
   * </ol>
   *
   * @param dto the validated event payload
   * @throws DuplicateEventException if an event with the same {@code eventId} was already processed
   */
  @Transactional
  public void processEvent(EventRequestDTO dto) {
    log.info("Processing event: eventId={}, traceId={}, eventName={}", dto.eventId(), dto.traceId(),
        dto.eventName());

    if (eventRepository.existsById(dto.eventId())) {
      log.warn("Duplicate event detected: eventId={}", dto.eventId());
      throw new DuplicateEventException("Event already processed: " + dto.eventId());
    }

    TraceEntity trace =
        traceRepository
            .findById(dto.traceId())
            .map(existing -> updateExistingTrace(existing, dto))
            .orElseGet(() -> createNewTrace(dto));
    traceRepository.saveAndFlush(trace);

    Instant now = Instant.now();
    boolean finalEvent = dto.finalEvent() != null && dto.finalEvent();

    EventEntity event =
        EventEntity.builder()
            .eventId(dto.eventId())
            .traceId(dto.traceId())
            .eventName(dto.eventName())
            .result(dto.result())
            .occurredAt(dto.occurredAt())
            .receivedAt(now)
            .nextExpectedEvent(dto.nextExpectedEvent())
            .nextEventTtlSeconds(dto.nextEventTtlSeconds())
            .finalEvent(finalEvent)
            .metadata(serializeMetadata(dto.metadata()))
            .build();

    eventRepository.saveAndFlush(event);

    log.info("Event processed successfully: eventId={}, traceId={}, status={}", dto.eventId(),
        dto.traceId(), trace.getStatus());
  }

  /**
   * Serializes metadata from a {@code Map<String, Object>} to a JSON string using Jackson 3.x.
   *
   * <p>Returns {@code null} if the map is {@code null}, empty, or serialization fails.
   * Serialization failures are logged at WARN level but do not fail the request — metadata
   * is best-effort.</p>
   *
   * @param metadata the key-value map from the event request
   * @return JSON string representation, or {@code null}
   */
  private String serializeMetadata(java.util.Map<String, Object> metadata) {
    if (metadata == null || metadata.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(metadata);
    } catch (JacksonException e) {
      log.warn("Failed to serialize metadata, storing as null", e);
      return null;
    }
  }

  /**
   * Builds a new {@link TraceEntity} from the first event for a given {@code traceId}.
   *
   * <p>Applies state transition rules based on the event's fields. The trace is not
   * persisted here — the caller is responsible for saving it.</p>
   *
   * @param dto the event payload
   * @return a new, unsaved {@link TraceEntity} with the appropriate initial state
   */
  private TraceEntity createNewTrace(EventRequestDTO dto) {
    Instant now = Instant.now();
    boolean finalEvent = dto.finalEvent() != null && dto.finalEvent();

    TraceEntity.TraceEntityBuilder builder =
        TraceEntity.builder()
            .traceId(dto.traceId())
            .lastEventName(dto.eventName())
            .lastEventResult(dto.result())
            .eventsCount(1);

    if (finalEvent) {
      builder.status(TraceStatus.COMPLETED);
    } else if (dto.nextExpectedEvent() != null && dto.nextEventTtlSeconds() != null) {
      builder
          .status(TraceStatus.WAITING_OTHER_EVENT)
          .nextExpectedEvent(dto.nextExpectedEvent())
          .nextExpectedBefore(now.plusSeconds(dto.nextEventTtlSeconds()));
    } else {
      builder.status(TraceStatus.STARTED);
    }

    return builder.build();
  }

  /**
   * Mutates an existing {@link TraceEntity} in-place based on a new event.
   *
   * <p>Increments {@code eventsCount}, updates last event metadata, and advances the
   * state according to the transition rules. When entering {@code STARTED} or
   * {@code COMPLETED}, any stale expectations from the previous event are cleared.</p>
   *
   * @param trace the existing trace entity (mutated in-place)
   * @param dto   the new event payload
   * @return the mutated trace entity
   */
  private TraceEntity updateExistingTrace(TraceEntity trace, EventRequestDTO dto) {
    trace.setEventsCount(trace.getEventsCount() + 1);
    trace.setLastEventName(dto.eventName());
    trace.setLastEventResult(dto.result());

    boolean finalEvent = dto.finalEvent() != null && dto.finalEvent();

    if (finalEvent) {
      trace.setStatus(TraceStatus.COMPLETED);
      trace.setNextExpectedEvent(null);
      trace.setNextExpectedBefore(null);
    } else if (dto.nextExpectedEvent() != null && dto.nextEventTtlSeconds() != null) {
      trace.setStatus(TraceStatus.WAITING_OTHER_EVENT);
      trace.setNextExpectedEvent(dto.nextExpectedEvent());
      trace.setNextExpectedBefore(Instant.now().plusSeconds(dto.nextEventTtlSeconds()));
    } else {
      trace.setStatus(TraceStatus.STARTED);
      trace.setNextExpectedEvent(null);
      trace.setNextExpectedBefore(null);
    }

    return trace;
  }

  /**
   * Returns the current status of a trace, evaluating TTL expiration lazily.
   *
   * <p>If the trace is in {@code WAITING_OTHER_EVENT} state and the current server time
   * has passed {@code nextExpectedBefore}, the status is updated to
   * {@code TTL_EXPIRED_FOR_EVENT} and persisted. This is the only mechanism for TTL
   * expiration — there is no background scheduler.</p>
   *
   * <p>TTL expiration is idempotent: once expired, the trace stays expired until a new
   * event advances it to another state. Completed traces are never checked for TTL
   * expiration.</p>
   *
   * @param traceId the unique identifier of the distributed flow
   * @return the current trace status details
   * @throws TraceNotFoundException if no trace exists with the given {@code traceId}
   */
  @Transactional
  public TraceStatusResponseDTO getTraceStatus(String traceId) {
    log.info("Retrieving trace status: traceId={}", traceId);

    TraceEntity trace =
        traceRepository
            .findById(traceId)
            .orElseThrow(() -> new TraceNotFoundException(traceId));

    if (trace.getStatus() == TraceStatus.WAITING_OTHER_EVENT
        && trace.getNextExpectedBefore() != null
        && Instant.now().isAfter(trace.getNextExpectedBefore())) {
      log.info("TTL expired for trace: traceId={}", traceId);
      trace.setStatus(TraceStatus.TTL_EXPIRED_FOR_EVENT);
      traceRepository.saveAndFlush(trace);
    }

    return new TraceStatusResponseDTO(
        trace.getTraceId(),
        trace.getStatus(),
        trace.getLastEventName(),
        trace.getLastEventResult(),
        trace.getNextExpectedEvent(),
        trace.getNextExpectedBefore(),
        trace.getEventsCount());
  }
}
