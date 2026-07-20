package com.clara.challenge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Map;

/**
 * Immutable request body for {@code POST /events}.
 *
 * <p>Java {@code record} with Jakarta Bean Validation annotations. Jackson 3.x
 * ({@code tools.jackson.databind}) deserializes the JSON payload into this record
 * automatically. Validation failures result in a {@code 400 Bad Request} with
 * field-level error messages.</p>
 *
 * <h3>Idempotency</h3>
 * <p>{@code eventId} serves as the idempotency key. Receiving the same
 * {@code eventId} twice returns {@code 409 Conflict} with no side effects.</p>
 *
 * <h3>State transitions</h3>
 * <p>The combination of {@code finalEvent}, {@code nextExpectedEvent}, and
 * {@code nextEventTtlSeconds} determines the resulting trace status:</p>
 * <ul>
 *   <li>{@code finalEvent = true} → {@code COMPLETED}</li>
 *   <li>Both expectation fields set → {@code WAITING_OTHER_EVENT}</li>
 *   <li>Neither → {@code STARTED}</li>
 * </ul>
 *
 * @param eventId            unique identifier of the event (idempotency key)
 * @param traceId            identifier of the distributed flow
 * @param eventName          name of the action that produced this event
 * @param result             outcome of the event ({@code SUCCESS} or {@code ERROR})
 * @param occurredAt         date and time when the event occurred (ISO-8601 with timezone)
 * @param nextExpectedEvent  name of the next expected event (optional)
 * @param nextEventTtlSeconds maximum seconds to wait for the next expected event (optional, must be &gt; 0)
 * @param finalEvent         whether this event completes the flow (optional, defaults to {@code false})
 * @param metadata           flexible key-value metadata (optional, stored as JSONB)
 */
public record EventRequestDTO(
    @NotBlank @Size(max = 255) String eventId,
    @NotBlank @Size(max = 255) String traceId,
    @NotBlank @Size(max = 255) String eventName,
    @NotBlank @Size(max = 50) String result,
    @NotNull Instant occurredAt,
    @Size(max = 255) String nextExpectedEvent,
    @Positive Integer nextEventTtlSeconds,
    Boolean finalEvent,
    Map<String, Object> metadata) {}
