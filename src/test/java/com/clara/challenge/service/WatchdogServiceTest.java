package com.clara.challenge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.clara.challenge.dto.EventRequestDTO;
import com.clara.challenge.dto.TraceStatusResponseDTO;
import com.clara.challenge.entity.TraceEntity;
import com.clara.challenge.entity.TraceStatus;
import com.clara.challenge.exception.DuplicateEventException;
import com.clara.challenge.exception.TraceNotFoundException;
import com.clara.challenge.repository.EventRepository;
import com.clara.challenge.repository.TraceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchdogServiceTest {

  @Mock
  private TraceRepository traceRepository;

  @Mock
  private EventRepository eventRepository;

  @InjectMocks
  private WatchdogService watchdogService;

  private static EventRequestDTO defaultRequest() {
    return new EventRequestDTO(
        "evt-001", "trace-001", "APPLICATION_RECEIVED", "SUCCESS",
        Instant.parse("2026-07-19T10:00:00Z"),
        "RULES_EVALUATED", 120, false,
        Map.of("country", "MX"));
  }

  // ── processEvent ──────────────────────────────────────────────

  @Test
  void shouldRejectDuplicateEventWhenEventIdAlreadyExists() {
    EventRequestDTO dto = defaultRequest();
    when(eventRepository.existsById("evt-001")).thenReturn(true);

    assertThatThrownBy(() -> watchdogService.processEvent(dto))
        .isInstanceOf(DuplicateEventException.class)
        .hasMessageContaining("Event already processed: evt-001");

    verify(traceRepository, never()).findById(any());
    verify(eventRepository, never()).saveAndFlush(any());
  }

  @Test
  void shouldCreateTraceWithWaitingStatusWhenFirstEventDefinesNextExpectedEvent() {
    EventRequestDTO dto = defaultRequest();
    when(eventRepository.existsById("evt-001")).thenReturn(false);
    when(traceRepository.findById("trace-001")).thenReturn(Optional.empty());

    watchdogService.processEvent(dto);

    ArgumentCaptor<TraceEntity> captor = ArgumentCaptor.forClass(TraceEntity.class);
    verify(traceRepository).saveAndFlush(captor.capture());

    TraceEntity saved = captor.getValue();
    assertThat(saved.getTraceId()).isEqualTo("trace-001");
    assertThat(saved.getStatus()).isEqualTo(TraceStatus.WAITING_OTHER_EVENT);
    assertThat(saved.getLastEventName()).isEqualTo("APPLICATION_RECEIVED");
    assertThat(saved.getLastEventResult()).isEqualTo("SUCCESS");
    assertThat(saved.getNextExpectedEvent()).isEqualTo("RULES_EVALUATED");
    assertThat(saved.getNextExpectedBefore()).isNotNull();
    assertThat(saved.getEventsCount()).isEqualTo(1);
    verify(eventRepository).saveAndFlush(any());
  }

  @Test
  void shouldCreateTraceWithStartedStatusWhenFirstEventHasNoNextExpectedEvent() {
    EventRequestDTO dto = new EventRequestDTO(
        "evt-002", "trace-002", "EVENT_X", "SUCCESS",
        Instant.parse("2026-07-19T10:00:00Z"),
        null, null, false, null);
    when(eventRepository.existsById("evt-002")).thenReturn(false);
    when(traceRepository.findById("trace-002")).thenReturn(Optional.empty());

    watchdogService.processEvent(dto);

    ArgumentCaptor<TraceEntity> captor = ArgumentCaptor.forClass(TraceEntity.class);
    verify(traceRepository).saveAndFlush(captor.capture());

    TraceEntity saved = captor.getValue();
    assertThat(saved.getStatus()).isEqualTo(TraceStatus.STARTED);
    assertThat(saved.getNextExpectedEvent()).isNull();
    assertThat(saved.getNextExpectedBefore()).isNull();
    assertThat(saved.getEventsCount()).isEqualTo(1);
  }

  @Test
  void shouldCreateTraceWithCompletedStatusWhenFirstEventIsFinal() {
    EventRequestDTO dto = new EventRequestDTO(
        "evt-003", "trace-003", "FINAL_EVENT", "SUCCESS",
        Instant.parse("2026-07-19T10:00:00Z"),
        null, null, true, null);
    when(eventRepository.existsById("evt-003")).thenReturn(false);
    when(traceRepository.findById("trace-003")).thenReturn(Optional.empty());

    watchdogService.processEvent(dto);

    ArgumentCaptor<TraceEntity> captor = ArgumentCaptor.forClass(TraceEntity.class);
    verify(traceRepository).saveAndFlush(captor.capture());

    TraceEntity saved = captor.getValue();
    assertThat(saved.getStatus()).isEqualTo(TraceStatus.COMPLETED);
    assertThat(saved.getNextExpectedEvent()).isNull();
    assertThat(saved.getNextExpectedBefore()).isNull();
  }

  @Test
  void shouldUpdateExistingTraceWhenEventsArriveForSameTraceId() {
    EventRequestDTO first = defaultRequest();
    when(eventRepository.existsById("evt-001")).thenReturn(false);
    when(traceRepository.findById("trace-001")).thenReturn(Optional.empty());

    watchdogService.processEvent(first);

    ArgumentCaptor<TraceEntity> firstCaptor = ArgumentCaptor.forClass(TraceEntity.class);
    verify(traceRepository).saveAndFlush(firstCaptor.capture());
    assertThat(firstCaptor.getValue().getEventsCount()).isEqualTo(1);

    EventRequestDTO second = new EventRequestDTO(
        "evt-004", "trace-001", "RULES_EVALUATED", "APPROVED",
        Instant.parse("2026-07-19T10:02:00Z"),
        null, null, true, null);
    when(eventRepository.existsById("evt-004")).thenReturn(false);
    when(traceRepository.findById("trace-001")).thenReturn(Optional.of(firstCaptor.getValue()));

    watchdogService.processEvent(second);

    ArgumentCaptor<TraceEntity> secondCaptor = ArgumentCaptor.forClass(TraceEntity.class);
    verify(traceRepository, times(2)).saveAndFlush(secondCaptor.capture());
    List<TraceEntity> allSaves = secondCaptor.getAllValues();

    TraceEntity updated = allSaves.get(1);
    assertThat(updated.getEventsCount()).isEqualTo(2);
    assertThat(updated.getStatus()).isEqualTo(TraceStatus.COMPLETED);
    assertThat(updated.getLastEventName()).isEqualTo("RULES_EVALUATED");
    assertThat(updated.getLastEventResult()).isEqualTo("APPROVED");
    assertThat(updated.getNextExpectedEvent()).isNull();
    assertThat(updated.getNextExpectedBefore()).isNull();
  }

  // ── getTraceStatus ────────────────────────────────────────────

  @Test
  void shouldReturnTraceStatusWhenTraceIdExists() {
    TraceEntity trace = TraceEntity.builder()
        .traceId("trace-001")
        .status(TraceStatus.WAITING_OTHER_EVENT)
        .lastEventName("APPLICATION_RECEIVED")
        .lastEventResult("SUCCESS")
        .nextExpectedEvent("RULES_EVALUATED")
        .nextExpectedBefore(Instant.now().plusSeconds(60))
        .eventsCount(1)
        .build();
    when(traceRepository.findById("trace-001")).thenReturn(Optional.of(trace));

    TraceStatusResponseDTO response = watchdogService.getTraceStatus("trace-001");

    assertThat(response.traceId()).isEqualTo("trace-001");
    assertThat(response.status()).isEqualTo(TraceStatus.WAITING_OTHER_EVENT);
    assertThat(response.lastEventName()).isEqualTo("APPLICATION_RECEIVED");
    assertThat(response.lastEventResult()).isEqualTo("SUCCESS");
    assertThat(response.nextExpectedEvent()).isEqualTo("RULES_EVALUATED");
    assertThat(response.nextExpectedBefore()).isNotNull();
    assertThat(response.eventsReceived()).isEqualTo(1);
  }

  @Test
  void shouldThrowNotFoundExceptionWhenTraceIdDoesNotExist() {
    when(traceRepository.findById("unknown")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> watchdogService.getTraceStatus("unknown"))
        .isInstanceOf(TraceNotFoundException.class)
        .hasMessageContaining("Trace not found: unknown");
  }

  @Test
  void shouldTransitionToTtlExpiredWhenWaitingTtlHasExpiredOnStatusCheck() {
    TraceEntity trace = TraceEntity.builder()
        .traceId("trace-001")
        .status(TraceStatus.WAITING_OTHER_EVENT)
        .lastEventName("APPLICATION_RECEIVED")
        .lastEventResult("SUCCESS")
        .nextExpectedEvent("RULES_EVALUATED")
        .nextExpectedBefore(Instant.now().minusSeconds(10))
        .eventsCount(1)
        .build();
    when(traceRepository.findById("trace-001")).thenReturn(Optional.of(trace));

    TraceStatusResponseDTO response = watchdogService.getTraceStatus("trace-001");

    assertThat(response.status()).isEqualTo(TraceStatus.TTL_EXPIRED_FOR_EVENT);
    verify(traceRepository).saveAndFlush(any());
  }

  @Test
  void shouldNotExpireCompletedTraceOnStatusCheck() {
    TraceEntity trace = TraceEntity.builder()
        .traceId("trace-001")
        .status(TraceStatus.COMPLETED)
        .lastEventName("FINAL_EVENT")
        .lastEventResult("SUCCESS")
        .nextExpectedEvent(null)
        .nextExpectedBefore(null)
        .eventsCount(2)
        .build();
    when(traceRepository.findById("trace-001")).thenReturn(Optional.of(trace));

    TraceStatusResponseDTO response = watchdogService.getTraceStatus("trace-001");

    assertThat(response.status()).isEqualTo(TraceStatus.COMPLETED);
  }
}
