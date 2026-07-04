package com.clara.challenge.event.domain;

import java.util.Objects;

public record TransitionResult(TraceState traceState, TransitionReason reason) {

  public TransitionResult {
    Objects.requireNonNull(traceState, "traceState is required");
    Objects.requireNonNull(reason, "reason is required");
  }
}
