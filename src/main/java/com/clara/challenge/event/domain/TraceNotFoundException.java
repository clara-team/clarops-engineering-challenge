package com.clara.challenge.event.domain;

public class TraceNotFoundException extends RuntimeException {

  public TraceNotFoundException(String traceId) {
    super("Trace not found: " + traceId);
  }
}
