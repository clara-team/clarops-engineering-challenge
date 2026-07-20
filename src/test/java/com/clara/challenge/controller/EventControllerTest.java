package com.clara.challenge.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.challenge.dto.EventRequestDTO;
import com.clara.challenge.exception.DuplicateEventException;
import com.clara.challenge.service.WatchdogService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

class EventControllerTest {

  private WatchdogService watchdogService;
  private EventController controller;

  @BeforeEach
  void setUp() {
    watchdogService = mock(WatchdogService.class);
    controller = new EventController(watchdogService);
  }

  @Test
  void shouldReturn200WhenValidEventPosted() {
    EventRequestDTO dto =
        new EventRequestDTO(
            "evt-001", "trace-001", "APPLICATION_RECEIVED", "SUCCESS",
            Instant.now(), "RULES_EVALUATED", 120, false,
            Map.of("country", "MX"));

    ResponseEntity<Void> response = controller.receiveEvent(dto);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(watchdogService).processEvent(dto);
  }

  @Test
  void shouldPropagateDuplicateEventExceptionFromService() {
    EventRequestDTO dto =
        new EventRequestDTO(
            "evt-001", "trace-001", "EVENT", "SUCCESS",
            Instant.now(), null, null, false, null);
    doThrow(new DuplicateEventException("Event already processed: evt-001"))
        .when(watchdogService)
        .processEvent(any());

    try {
      controller.receiveEvent(dto);
    } catch (DuplicateEventException ex) {
      assertThat(ex.getMessage()).contains("evt-001");
    }
  }

  @Test
  void shouldAcceptRequestWithAllOptionalFieldsSet() {
    EventRequestDTO dto =
        new EventRequestDTO(
            "evt-001", "trace-001", "FINAL_EVENT", "ERROR",
            Instant.parse("2026-07-19T10:00:00Z"),
            "NEXT_EVENT", 60, true,
            Map.of("key", "value"));

    ResponseEntity<Void> response = controller.receiveEvent(dto);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(watchdogService).processEvent(dto);
  }

  @Test
  void shouldAcceptRequestWithNullOptionalFields() {
    EventRequestDTO dto =
        new EventRequestDTO(
            "evt-002", "trace-002", "SIMPLE_EVENT", "SUCCESS",
            Instant.now(), null, null, null, null);

    ResponseEntity<Void> response = controller.receiveEvent(dto);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    verify(watchdogService).processEvent(dto);
  }
}
