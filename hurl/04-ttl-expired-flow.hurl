# Scenario: A trace expects a next event with a short TTL (1 second).
# After a 2-second delay, the status evaluates to TTL_EXPIRED_FOR_EVENT.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "evt-expired-001",
  "traceId": "trace-hurl-expired",
  "eventName": "FRAUD_CHECK_REQUESTED",
  "result": "SUCCESS",
  "occurredAt": "2026-07-20T10:00:00Z",
  "nextExpectedEvent": "FRAUD_CHECK_FINISHED",
  "nextEventTtlSeconds": 1,
  "finalEvent": false
}
HTTP 200

GET http://localhost:8080/api/traces/trace-hurl-expired/status
[Options]
delay: 2000
HTTP 200
[Asserts]
jsonpath "$.status" == "TTL_EXPIRED_FOR_EVENT"
jsonpath "$.nextExpectedEvent" == "FRAUD_CHECK_FINISHED"
jsonpath "$.eventsReceived" == 1
jsonpath "$.traceId" == "trace-hurl-expired"
