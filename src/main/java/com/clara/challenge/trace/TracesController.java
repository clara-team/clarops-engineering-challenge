package com.clara.challenge.trace;

import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/traces")
@RequiredArgsConstructor
public class TracesController {

  private final TraceStateRepository traces;
  private final Clock clock;

  @GetMapping("/{traceId}/status")
  public ResponseEntity<TraceStatusResponse> status(@PathVariable String traceId) {
    return traces
        .findById(traceId)
        .map(
            trace ->
                ResponseEntity.ok(
                    new TraceStatusResponse(
                        trace.getTraceId(),
                        trace.statusAt(clock.instant()),
                        trace.getLastEventName(),
                        trace.getLastEventResult(),
                        trace.getNextExpectedEvent(),
                        trace.getNextExpectedBefore(),
                        trace.getEventsReceived())))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
