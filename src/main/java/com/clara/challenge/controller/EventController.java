package com.clara.challenge.controller;

import com.clara.challenge.dto.EventRequestDTO;
import com.clara.challenge.service.WatchdogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for ingesting distributed events.
 *
 * <p>Thin adapter over {@link WatchdogService}. Performs no business logic — delegates
 * everything to the service layer. Jakarta Bean Validation ({@code @Valid}) and error
 * responses are handled declaratively via {@code GlobalExceptionHandler}.</p>
 *
 * <h3>Endpoint</h3>
 * <pre>{@code POST /events}</pre>
 *
 * <h3>Possible responses</h3>
 * <ul>
 *   <li>{@code 200 OK} — event processed successfully (empty body)</li>
 *   <li>{@code 400 Bad Request} — validation failure or malformed JSON</li>
 *   <li>{@code 409 Conflict} — duplicate {@code eventId}</li>
 * </ul>
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

  private final WatchdogService watchdogService;

  /**
   * Receives an event associated with a distributed flow.
   *
   * <p>Triggers idempotency check, trace state creation or update, and event
   * persistence. All operations run within a single transaction in
   * {@link WatchdogService#processEvent}.</p>
   *
   * @param request the event payload, validated via Jakarta Bean Validation
   * @return {@code 200 OK} with empty body on success
   */
  @PostMapping
  public ResponseEntity<Void> receiveEvent(@Valid @RequestBody EventRequestDTO request) {
    watchdogService.processEvent(request);
    return ResponseEntity.ok().build();
  }
}
