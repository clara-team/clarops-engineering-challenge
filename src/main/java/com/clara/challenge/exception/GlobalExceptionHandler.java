package com.clara.challenge.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized exception-to-HTTP-response mapping for all REST controllers.
 *
 * <p>Every handler returns a consistent JSON error envelope with fields in a
 * predictable order: {@code timestamp}, {@code status}, {@code error},
 * {@code message}/{@code messages}. Uses {@link java.util.LinkedHashMap} to
 * preserve insertion order.</p>
 *
 * <p>Spring evaluates {@code @ExceptionHandler} methods in order of specificity —
 * the catch-all {@code handleGeneral(Exception)} fires only when no other
 * handler matches.</p>
 *
 * <h3>Error response envelope</h3>
 * <pre>{@code
 * {
 *   "timestamp": "ISO-8601",
 *   "status": 400,
 *   "error": "Validation failed",
 *   "messages": ["field: error"]
 * }
 * }</pre>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles malformed or unparseable request bodies (invalid JSON syntax, type mismatches).
   *
   * @param ex the exception thrown during HTTP message deserialization
   * @return {@code 400 Bad Request} with a descriptive error message
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> handleMalformedRequest(
      HttpMessageNotReadableException ex) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Malformed request body");
    body.put("message", "Request body is not valid JSON or contains invalid values");
    return ResponseEntity.badRequest().body(body);
  }

  /**
   * Handles Jakarta Bean Validation failures ({@code @Valid} violations on DTOs).
   *
   * <p>Collects all field-level errors into a {@code messages} list for the response.
   * Each entry follows the format {@code "fieldName: error message"}.</p>
   *
   * @param ex the exception containing binding result with field errors
   * @return {@code 400 Bad Request} with field-level validation messages
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      MethodArgumentNotValidException ex) {
    List<String> errors =
        ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList();
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Validation failed");
    body.put("messages", errors);
    return ResponseEntity.badRequest().body(body);
  }

  /**
   * Handles business-level argument errors ({@code IllegalArgumentException}).
   *
   * <p>The exception message is included in the response body. Useful for
   * invalid argument combinations that pass validation but fail business rules.</p>
   *
   * @param ex the exception with the descriptive message
   * @return {@code 400 Bad Request} with the exception message
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, Object>> handleBadArgument(IllegalArgumentException ex) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", "Invalid argument");
    body.put("message", ex.getMessage());
    return ResponseEntity.badRequest().body(body);
  }

  /**
   * Handles duplicate event submissions (idempotency violation).
   *
   * <p>Thrown by {@code WatchdogService.processEvent()} when
   * {@code eventRepository.existsById()} returns {@code true} before
   * any trace mutation occurs.</p>
   *
   * @param ex the exception with the event ID that was already processed
   * @return {@code 409 Conflict}
   */
  @ExceptionHandler(DuplicateEventException.class)
  public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateEventException ex) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now());
    body.put("status", HttpStatus.CONFLICT.value());
    body.put("error", "Conflict");
    body.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
  }

  /**
   * Handles queries for non-existent traces.
   *
   * <p>Thrown by {@code WatchdogService.getTraceStatus()} when
   * {@code traceRepository.findById()} returns empty.</p>
   *
   * @param ex the exception containing the unknown {@code traceId}
   * @return {@code 404 Not Found}
   */
  @ExceptionHandler(TraceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(TraceNotFoundException ex) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now());
    body.put("status", HttpStatus.NOT_FOUND.value());
    body.put("error", "Not Found");
    body.put("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  /**
   * Catch-all handler for unexpected exceptions.
   *
   * <p>Logs the full stack trace at ERROR level but returns a safe, generic message
   * to the client to avoid leaking internal implementation details.</p>
   *
   * @param ex the unexpected exception
   * @return {@code 500 Internal Server Error} with a masked message
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
    log.error("Unhandled exception", ex);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", Instant.now());
    body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
    body.put("error", "Internal Server Error");
    body.put("message", "An unexpected error occurred");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
