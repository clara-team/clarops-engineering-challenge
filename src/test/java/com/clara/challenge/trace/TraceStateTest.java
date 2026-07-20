package com.clara.challenge.trace;

import static com.clara.challenge.trace.TraceStatus.WAITING_OTHER_EVENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TraceStateTest {

  private static final String FIRST_EVENT_OCCURRED_AT = "2026-06-15T10:00:00Z";
  private static final int TTL_SECONDS = 120;

  @LocalServerPort private int port;
  @Autowired private SettableClock clock;
  @Autowired private TraceStateRepository traces;

  private String traceId;
  private TraceStatusResponse response;

  @Test
  @DisplayName("reports WAITING_OTHER_EVENT while the deadline has not passed") // [T1]
  void shouldReportWaitingOtherEvent_whenTheDeadlineHasNotPassed() {
    givenTheDomainTypeExists("TraceState");
    traceId = givenATraceWhoseLastEventPromised("RULES_EVALUATED");
    givenTheDeadlineIs("2026-06-15T10:02:00Z");

    whenTheStatusIsAskedViaGetEndpoint(
        "GET", "/api/traces/" + traceId + "/status", "2026-06-15T10:01:00Z");

    thenTheStatusForTraceIdIs(traceId, WAITING_OTHER_EVENT);
  }

  // --- given ---

  private void givenTheDomainTypeExists(String typeName) {
    assertThat(TraceState.class.getSimpleName()).isEqualTo(typeName);
  }

  private String givenATraceWhoseLastEventPromised(String nextExpectedEvent) {
    String newTraceId = "trace-" + UUID.randomUUID();

    client()
        .post()
        .uri("/api/events")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            Map.of(
                "eventId", "evt-" + UUID.randomUUID(),
                "traceId", newTraceId,
                "eventName", "APPLICATION_RECEIVED",
                "result", "SUCCESS",
                "occurredAt", FIRST_EVENT_OCCURRED_AT,
                "nextExpectedEvent", nextExpectedEvent,
                "nextEventTtlSeconds", TTL_SECONDS))
        .exchange()
        .expectStatus()
        .isCreated();

    return newTraceId;
  }

  private void givenTheDeadlineIs(String expectedDeadline) {
    assertThat(traces.findById(traceId))
        .get()
        .extracting(TraceState::getNextExpectedBefore)
        .isEqualTo(Instant.parse(expectedDeadline));
  }

  // --- when ---

  private void whenTheStatusIsAskedViaGetEndpoint(String method, String url, String askedAt) {
    clock.setTo(Instant.parse(askedAt));
    response =
        client()
            .method(org.springframework.http.HttpMethod.valueOf(method))
            .uri(url)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(TraceStatusResponse.class)
            .returnResult()
            .getResponseBody();
  }

  // --- then ---

  private void thenTheStatusForTraceIdIs(String expectedTraceId, TraceStatus expectedStatus) {
    assertThat(response).isNotNull();
    assertThat(response.traceId()).isEqualTo(expectedTraceId);
    assertThat(response.status()).isEqualTo(expectedStatus);
  }

  // --- test wiring ---

  private RestTestClient client() {
    return RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @TestConfiguration
  static class FixedClockConfiguration {

    @Bean
    @Primary
    SettableClock settableClock() {
      return new SettableClock();
    }
  }

  static class SettableClock extends Clock {

    private Instant now = Instant.now();

    void setTo(Instant instant) {
      this.now = instant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }
}
