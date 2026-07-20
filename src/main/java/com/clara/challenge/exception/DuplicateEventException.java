package com.clara.challenge.exception;

import java.io.Serial;

/**
 * Thrown when an event with a previously processed {@code eventId} is received.
 *
 * <p>Mapped to {@code 409 Conflict} by {@link GlobalExceptionHandler#handleDuplicate}.
 * This exception is thrown at the start of {@code WatchdogService.processEvent()},
 * before any trace mutation or event persistence occurs, ensuring true idempotency.</p>
 */
public class DuplicateEventException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Creates a new exception for a duplicate event.
   *
   * @param message descriptive message, typically {@code "Event already processed: " + eventId}
   */
  public DuplicateEventException(String message) {
    super(message);
  }
}
