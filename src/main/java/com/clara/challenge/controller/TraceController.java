package com.clara.challenge.controller;

import com.clara.challenge.dto.TraceStatusResponseDTO;
import com.clara.challenge.service.WatchdogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for querying the status of a distributed flow trace.
 *
 * <p>Thin adapter over {@link WatchdogService}. TTL expiration is evaluated lazily
 * on read — there is no background scheduler. Unknown {@code traceId} values result in a
 * {@code 404 Not Found} response.</p>
 *
 * <h3>Endpoint</h3>
 * <pre>{@code GET /traces/{traceId}/status}</pre>
 *
 * <h3>Possible responses</h3>
 * <ul>
 *   <li>{@code 200 OK} — trace found, status returned in {@link TraceStatusResponseDTO}</li>
 *   <li>{@code 404 Not Found} — no trace exists with the given {@code traceId}</li>
 * </ul>
 */
@RestController
@RequestMapping("/traces")
@RequiredArgsConstructor
public class TraceController {

  private final WatchdogService watchdogService;

  /**
   * Returns the current state of the distributed flow identified by {@code traceId}.
   *
   * <p>The status is calculated from the received events and TTL configuration.
   * If the trace is in {@code WAITING_OTHER_EVENT} state and its TTL has expired,
   * the status is updated to {@code TTL_EXPIRED_FOR_EVENT} as a side effect.</p>
   *
   * @param traceId the unique identifier of the distributed flow
   * @return {@code 200 OK} with the trace status details
   */
  @GetMapping("/{traceId}/status")
  public ResponseEntity<TraceStatusResponseDTO> getTraceStatus(@PathVariable String traceId) {
    TraceStatusResponseDTO response = watchdogService.getTraceStatus(traceId);
    return ResponseEntity.ok(response);
  }
}
