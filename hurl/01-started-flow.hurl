# Scenario: A trace receives its first event without nextExpectedEvent and finalEvent = false.
# Expected result: trace.status == STARTED, eventsReceived == 1.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "evt-started-001",
  "traceId": "trace-hurl-started",
  "eventName": "SYSTEM_INITIALIZED",
  "result": "SUCCESS",
  "occurredAt": "2026-07-20T10:00:00Z",
  "finalEvent": false
}
HTTP 200

GET http://localhost:8080/api/traces/trace-hurl-started/status
HTTP 200
[Asserts]
jsonpath "$.status" == "STARTED"
jsonpath "$.lastEventName" == "SYSTEM_INITIALIZED"
jsonpath "$.eventsReceived" == 1
jsonpath "$.traceId" == "trace-hurl-started"
