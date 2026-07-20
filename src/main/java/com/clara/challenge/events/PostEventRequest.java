package com.clara.challenge.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.Map;

public record PostEventRequest(
    @NotBlank String eventId,
    @NotBlank String traceId,
    @NotBlank String eventName,
    @NotNull @Pattern(regexp = "SUCCESS|ERROR") String result,
    @NotNull Instant occurredAt,
    String nextExpectedEvent,
    Integer nextEventTtlSeconds,
    Boolean finalEvent,
    Map<String, Object> metadata) {}
