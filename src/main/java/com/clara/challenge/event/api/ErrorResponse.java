package com.clara.challenge.event.api;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
    ApiErrorCode code,
    String message,
    String traceId,
    Map<String, String> details,
    Instant timestamp) {}
