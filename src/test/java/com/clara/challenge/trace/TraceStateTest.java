package com.clara.challenge.trace;

import static com.clara.challenge.trace.TraceStatus.COMPLETED;
import static com.clara.challenge.trace.TraceStatus.STARTED;
import static com.clara.challenge.trace.TraceStatus.TTL_EXPIRED_FOR_EVENT;
import static com.clara.challenge.trace.TraceStatus.WAITING_OTHER_EVENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Every test here goes through the public endpoints, the same way a consumer would. Nothing is
 * mocked except the clock, because otherwise the answer would depend on what time you run it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TraceStateTest {

  private static final String FIRST_EVENT_OCCURRED_AT = "2026-06-15T10:00:00Z";
  private static final int TTL_SECONDS = 120;

  @LocalServerPort private int port;
  @Autowired private SettableClock clock;

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

  @Test
  @DisplayName("reports TTL_EXPIRED_FOR_EVENT once the deadline passed, with nobody writing") // [T2]
  void shouldReportTtlExpired_whenTheDeadlinePassedAndNothingArrived() {
    traceId = givenATraceWhoseLastEventPromised("RULES_EVALUATED");
    givenTheDeadlineIs("2026-06-15T10:02:00Z");

    whenTheStatusIsAskedViaGetEndpoint(
        "GET", "/api/traces/" + traceId + "/status", "2026-06-15T10:03:00Z");

    thenTheStatusForTraceIdIs(traceId, TTL_EXPIRED_FOR_EVENT);
  }

  @Test
  @DisplayName("still reports WAITING_OTHER_EVENT exactly on the deadline") // [T3]
  void shouldStillReportWaiting_whenTheClockSitsExactlyOnTheDeadline() {
    traceId = givenATraceWhoseLastEventPromised("RULES_EVALUATED");
    givenTheDeadlineIs("2026-06-15T10:02:00Z");

    whenTheStatusIsAskedViaGetEndpoint(
        "GET", "/api/traces/" + traceId + "/status", "2026-06-15T10:02:00Z");

    thenTheStatusForTraceIdIs(traceId, WAITING_OTHER_EVENT);
  }

  @Test
  @DisplayName("reports COMPLETED when the very first event is already the last one") // [T4]
  void shouldReportCompleted_whenTheFirstEventIsAlsoTheFinalOne() {
    traceId = givenATraceWhoseFirstEventWasFinal();

    whenTheStatusIsAskedViaGetEndpoint(
        "GET", "/api/traces/" + traceId + "/status", "2026-06-15T10:01:00Z");

    thenTheStatusForTraceIdIs(traceId, COMPLETED);
  }

  @Test
  @DisplayName("keeps reporting COMPLETED however far the clock moves") // [T5]
  void shouldKeepReportingCompleted_whenTheClockMovesPastAnOldDeadline() {
    traceId = givenAClosedTraceThatAlsoCarriesAnOldDeadline();

    whenTheStatusIsAskedViaGetEndpoint(
        "GET", "/api/traces/" + traceId + "/status", "2027-01-01T00:00:00Z");

    thenTheStatusForTraceIdIs(traceId, COMPLETED);
  }

  @Test
  @DisplayName("keeps waiting when a failed step still promised another event") // [T7]
  void shouldKeepWaiting_whenAFailedStepPromisedAnotherEvent() {
    traceId = givenATraceWhoseLastEventFailedButPromised("PAYMENT_RETRIED");

    whenTheStatusIsAskedViaGetEndpoint(
        "GET", "/api/traces/" + traceId + "/status", "2026-06-15T10:01:00Z");

    thenTheStatusForTraceIdIs(traceId, WAITING_OTHER_EVENT);
  }

  @Test
  @DisplayName("reports STARTED when the promise came without a deadline") // [T8]
  void shouldReportStarted_whenThePromiseCameWithoutATtl() {
    traceId = givenATraceThatPromisedWithoutATtl("RULES_EVALUATED");

    whenTheStatusIsAskedViaGetEndpoint(
        "GET", "/api/traces/" + traceId + "/status", "2026-06-15T23:59:00Z");

    thenTheStatusForTraceIdIs(traceId, STARTED);
  }

  @Test
  @DisplayName("comes back to WAITING when the late event finally arrives") // [T9]
  void shouldComeBackToWaiting_whenTheLateEventArrivesPromisingAnotherOne() {
    traceId = givenATraceWhoseLastEventPromised("RULES_EVALUATED");

    whenTheExpectedEventArrivesPromising("RULES_EVALUATED", "SCORE_CALCULATED");
    whenTheStatusIsAskedViaGetEndpoint(
        "GET", "/api/traces/" + traceId + "/status", "2026-06-15T10:06:00Z");

    thenTheStatusForTraceIdIs(traceId, WAITING_OTHER_EVENT);
  }

  // --- given ---

  private void givenTheDomainTypeExists(String typeName) {
    assertThat(TraceState.class.getSimpleName()).isEqualTo(typeName);
  }

  private String givenATraceWhoseLastEventPromised(String nextExpectedEvent) {
    return postFirstEvent(
        event -> {
          event.put("nextExpectedEvent", nextExpectedEvent);
          event.put("nextEventTtlSeconds", TTL_SECONDS);
        });
  }

  private String givenATraceThatPromisedWithoutATtl(String nextExpectedEvent) {
    return postFirstEvent(event -> event.put("nextExpectedEvent", nextExpectedEvent));
  }

  private String givenATraceWhoseFirstEventWasFinal() {
    return postFirstEvent(
        event -> {
          event.put("eventName", "APPLICATION_REJECTED");
          event.put("result", "ERROR");
          event.put("finalEvent", true);
        });
  }

  private String givenAClosedTraceThatAlsoCarriesAnOldDeadline() {
    return postFirstEvent(
        event -> {
          event.put("nextExpectedEvent", "RULES_EVALUATED");
          event.put("nextEventTtlSeconds", TTL_SECONDS);
          event.put("finalEvent", true);
        });
  }

  private String givenATraceWhoseLastEventFailedButPromised(String nextExpectedEvent) {
    return postFirstEvent(
        event -> {
          event.put("result", "ERROR");
          event.put("nextExpectedEvent", nextExpectedEvent);
          event.put("nextEventTtlSeconds", TTL_SECONDS);
        });
  }

  private void givenTheDeadlineIs(String expectedDeadline) {
    assertThat(statusOf(traceId).nextExpectedBefore()).isEqualTo(Instant.parse(expectedDeadline));
  }

  // --- when ---

  private void whenTheExpectedEventArrivesPromising(String eventName, String nextExpectedEvent) {
    Map<String, Object> event = new HashMap<>(baseEvent(traceId));
    event.put("eventName", eventName);
    event.put("occurredAt", "2026-06-15T10:05:00Z");
    event.put("nextExpectedEvent", nextExpectedEvent);
    event.put("nextEventTtlSeconds", TTL_SECONDS);
    post(event);
  }

  private void whenTheStatusIsAskedViaGetEndpoint(String method, String url, String askedAt) {
    clock.setTo(Instant.parse(askedAt));
    response =
        client()
            .method(HttpMethod.valueOf(method))
            .uri(url)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(TraceStatusResponse.class)
            .returnResult()
            .getResponseBody();
  }

  /** Reads a trace through the endpoint. The deadline it returns does not depend on the clock. */
  private TraceStatusResponse statusOf(String someTraceId) {
    return client()
        .get()
        .uri("/api/traces/" + someTraceId + "/status")
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

  private String postFirstEvent(java.util.function.Consumer<Map<String, Object>> customize) {
    String newTraceId = "trace-" + UUID.randomUUID();
    Map<String, Object> event = new HashMap<>(baseEvent(newTraceId));
    customize.accept(event);
    post(event);
    return newTraceId;
  }

  private Map<String, Object> baseEvent(String forTraceId) {
    Map<String, Object> event = new HashMap<>();
    event.put("eventId", "evt-" + UUID.randomUUID());
    event.put("traceId", forTraceId);
    event.put("eventName", "APPLICATION_RECEIVED");
    event.put("result", "SUCCESS");
    event.put("occurredAt", FIRST_EVENT_OCCURRED_AT);
    return event;
  }

  private void post(Map<String, Object> event) {
    client()
        .post()
        .uri("/api/events")
        .contentType(MediaType.APPLICATION_JSON)
        .body(event)
        .exchange()
        .expectStatus()
        .isCreated();
  }

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
