package com.clara.challenge.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void shouldReturn409WhenDuplicateEventException() {
    DuplicateEventException ex = new DuplicateEventException("Event already processed: evt-001");

    ResponseEntity<Map<String, Object>> response = handler.handleDuplicate(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).containsEntry("status", 409);
    assertThat(response.getBody()).containsEntry("error", "Conflict");
    assertThat(response.getBody().get("message")).toString().contains("evt-001");
  }

  @Test
  void shouldReturn404WhenTraceNotFoundException() {
    TraceNotFoundException ex = new TraceNotFoundException("trace-001");

    ResponseEntity<Map<String, Object>> response = handler.handleNotFound(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).containsEntry("status", 404);
    assertThat(response.getBody()).containsEntry("error", "Not Found");
    assertThat(response.getBody().get("message")).toString().contains("trace-001");
  }

  @Test
  void shouldReturn400WhenIllegalArgumentException() {
    IllegalArgumentException ex = new IllegalArgumentException("Invalid value");

    ResponseEntity<Map<String, Object>> response = handler.handleBadArgument(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).containsEntry("status", 400);
    assertThat(response.getBody()).containsEntry("error", "Invalid argument");
    assertThat(response.getBody()).containsEntry("message", "Invalid value");
  }

  @Test
  void shouldReturn500AndMaskMessageForUnhandledException() {
    Exception ex = new RuntimeException("Sensitive internal detail");

    ResponseEntity<Map<String, Object>> response = handler.handleGeneral(ex);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).containsEntry("status", 500);
    assertThat(response.getBody()).containsEntry("error", "Internal Server Error");
    assertThat(response.getBody().get("message").toString()).doesNotContain("Sensitive");
  }
}
