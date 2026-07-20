package com.clara.challenge.exception;

import java.io.Serial;

/**
 * Thrown when a {@code GET /traces/{traceId}/status} query targets an unknown trace.
 *
 * <p>Mapped to {@code 404 Not Found} by {@link GlobalExceptionHandler#handleNotFound}.
 * The message is formatted as {@code "Trace not found: " + traceId}.</p>
 */
public class TraceNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Creates a new exception for a non-existent trace.
   *
   * @param traceId the trace identifier that was not found in the database
   */
  public TraceNotFoundException(String traceId) {
    super("Trace not found: " + traceId);
  }
}
