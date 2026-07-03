package com.clara.challenge.event.api;

import java.time.Instant;

public record TraceStatusResponse(
    String traceId,
    TraceStatus status,
    String lastEventName,
    EventResult lastEventResult,
    String nextExpectedEvent,
    Instant nextExpectedBefore,
    int eventsReceived) {}
