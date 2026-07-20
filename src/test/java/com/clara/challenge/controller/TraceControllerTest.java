package com.clara.challenge.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.clara.challenge.dto.TraceStatusResponseDTO;
import com.clara.challenge.entity.TraceStatus;
import com.clara.challenge.exception.TraceNotFoundException;
import com.clara.challenge.service.WatchdogService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class TraceControllerTest {

  private WatchdogService watchdogService;
  private TraceController controller;

  @BeforeEach
  void setUp() {
    watchdogService = mock(WatchdogService.class);
    controller = new TraceController(watchdogService);
  }

  @Test
  void shouldReturn200AndTraceStatusWhenTraceIdExists() {
    TraceStatusResponseDTO dto =
        new TraceStatusResponseDTO(
            "trace-001", TraceStatus.WAITING_OTHER_EVENT,
            "APPLICATION_RECEIVED", "SUCCESS",
            "RULES_EVALUATED", Instant.parse("2026-07-19T10:02:00Z"), 1);
    when(watchdogService.getTraceStatus("trace-001")).thenReturn(dto);

    ResponseEntity<TraceStatusResponseDTO> response = controller.getTraceStatus("trace-001");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().traceId()).isEqualTo("trace-001");
    assertThat(response.getBody().status()).isEqualTo(TraceStatus.WAITING_OTHER_EVENT);
    assertThat(response.getBody().lastEventName()).isEqualTo("APPLICATION_RECEIVED");
    assertThat(response.getBody().lastEventResult()).isEqualTo("SUCCESS");
    assertThat(response.getBody().nextExpectedEvent()).isEqualTo("RULES_EVALUATED");
    assertThat(response.getBody().eventsReceived()).isEqualTo(1);
  }

  @Test
  void shouldPropagateTraceNotFoundExceptionFromService() {
    when(watchdogService.getTraceStatus("unknown"))
        .thenThrow(new TraceNotFoundException("unknown"));

    try {
      controller.getTraceStatus("unknown");
    } catch (TraceNotFoundException ex) {
      assertThat(ex.getMessage()).contains("unknown");
    }
  }

  @Test
  void shouldReturnTraceStatusWithCompletedWhenFlowIsFinished() {
    TraceStatusResponseDTO dto =
        new TraceStatusResponseDTO(
            "trace-002", TraceStatus.COMPLETED,
            "FINAL_EVENT", "SUCCESS",
            null, null, 3);
    when(watchdogService.getTraceStatus("trace-002")).thenReturn(dto);

    ResponseEntity<TraceStatusResponseDTO> response = controller.getTraceStatus("trace-002");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().status()).isEqualTo(TraceStatus.COMPLETED);
    assertThat(response.getBody().nextExpectedEvent()).isNull();
    assertThat(response.getBody().nextExpectedBefore()).isNull();
    assertThat(response.getBody().eventsReceived()).isEqualTo(3);
  }
}
