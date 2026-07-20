# [H2] The event promises another one, with a TTL so long that it cannot
# expire while the test runs. The trace is waiting.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "{{newUuid}}",
  "traceId": "hurl-waiting",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2026-06-15T10:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 999999999
}
HTTP 201

GET http://localhost:8080/api/traces/hurl-waiting/status
HTTP 200
[Asserts]
jsonpath "$.traceId" == "hurl-waiting"
jsonpath "$.status" == "WAITING_OTHER_EVENT"
jsonpath "$.nextExpectedEvent" == "RULES_EVALUATED"
jsonpath "$.nextExpectedBefore" != null
