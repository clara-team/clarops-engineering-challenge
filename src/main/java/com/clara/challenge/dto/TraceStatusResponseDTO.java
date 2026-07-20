package com.clara.challenge.dto;

import com.clara.challenge.entity.TraceStatus;
import java.time.Instant;

/**
 * Immutable response body for {@code GET /traces/{traceId}/status}.
 *
 * <p>Java {@code record} serialized to JSON by Jackson 3.x. The {@code status} field
 * is serialized as its enum name (e.g. {@code "WAITING_OTHER_EVENT"}, not an ordinal).
 * Nullable fields ({@code nextExpectedEvent}, {@code nextExpectedBefore}) are omitted
 * from the JSON output when no expectation is set.</p>
 *
 * @param traceId            the identifier of the distributed flow
 * @param status             current trace state (STARTED, WAITING_OTHER_EVENT, TTL_EXPIRED_FOR_EVENT, or COMPLETED)
 * @param lastEventName      name of the most recently processed event
 * @param lastEventResult    result of the most recently processed event ({@code SUCCESS} or {@code ERROR})
 * @param nextExpectedEvent  name of the expected next event, or {@code null} if no expectation
 * @param nextExpectedBefore absolute deadline for the next expected event, or {@code null} if no expectation
 * @param eventsReceived     total count of events received for this trace
 */
public record TraceStatusResponseDTO(
    String traceId,
    TraceStatus status,
    String lastEventName,
    String lastEventResult,
    String nextExpectedEvent,
    Instant nextExpectedBefore,
    Integer eventsReceived) {}
