# Scenario: A trace transitions from waiting to completed when a final event arrives.
# Expected result: trace.status == COMPLETED, eventsReceived == 2.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "evt-completed-001",
  "traceId": "trace-hurl-completed",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2026-07-20T10:00:00Z",
  "nextExpectedEvent": "PAYMENT_CONFIRMED",
  "nextEventTtlSeconds": 300,
  "finalEvent": false
}
HTTP 200

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "evt-completed-002",
  "traceId": "trace-hurl-completed",
  "eventName": "PAYMENT_CONFIRMED",
  "result": "SUCCESS",
  "occurredAt": "2026-07-20T10:01:00Z",
  "finalEvent": true
}
HTTP 200

GET http://localhost:8080/api/traces/trace-hurl-completed/status
HTTP 200
[Asserts]
jsonpath "$.status" == "COMPLETED"
jsonpath "$.lastEventName" == "PAYMENT_CONFIRMED"
jsonpath "$.eventsReceived" == 2
jsonpath "$.traceId" == "trace-hurl-completed"
