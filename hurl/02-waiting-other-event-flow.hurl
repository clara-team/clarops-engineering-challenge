# Scenario: A trace receives an event defining nextExpectedEvent and nextEventTtlSeconds.
# Expected result: trace.status == WAITING_OTHER_EVENT, nextExpectedBefore is set.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "evt-waiting-001",
  "traceId": "trace-hurl-waiting",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2026-07-20T10:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 300,
  "finalEvent": false
}
HTTP 200

GET http://localhost:8080/api/traces/trace-hurl-waiting/status
HTTP 200
[Asserts]
jsonpath "$.status" == "WAITING_OTHER_EVENT"
jsonpath "$.nextExpectedEvent" == "RULES_EVALUATED"
jsonpath "$.nextExpectedBefore" exists
jsonpath "$.eventsReceived" == 1
jsonpath "$.lastEventName" == "APPLICATION_RECEIVED"
