package com.clara.challenge.event.service;

import com.clara.challenge.event.domain.TraceState;
import java.util.Objects;

public record EventIngestionResult(TraceState traceState, boolean idempotentDuplicate) {

  public EventIngestionResult {
    Objects.requireNonNull(traceState, "traceState is required");
  }
}
