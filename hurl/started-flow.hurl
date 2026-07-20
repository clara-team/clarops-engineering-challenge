# [H1] The first event promises nothing and does not close the flow.
# The trace has started, and there is nothing pending.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "{{newUuid}}",
  "traceId": "hurl-started",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2026-06-15T10:00:00Z"
}
HTTP 201

GET http://localhost:8080/api/traces/hurl-started/status
HTTP 200
[Asserts]
jsonpath "$.traceId" == "hurl-started"
jsonpath "$.status" == "STARTED"
jsonpath "$.lastEventName" == "APPLICATION_RECEIVED"
jsonpath "$.nextExpectedEvent" == null
