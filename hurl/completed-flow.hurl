# [H4] One event that says it is the last one. A flow with a single step is
# still a flow. Notice the result is ERROR and the status is still COMPLETED:
# result talks about the step, status talks about the flow.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "{{newUuid}}",
  "traceId": "hurl-completed",
  "eventName": "APPLICATION_REJECTED",
  "result": "ERROR",
  "occurredAt": "2026-06-15T10:00:00Z",
  "finalEvent": true
}
HTTP 201

GET http://localhost:8080/api/traces/hurl-completed/status
HTTP 200
[Asserts]
jsonpath "$.traceId" == "hurl-completed"
jsonpath "$.status" == "COMPLETED"
jsonpath "$.lastEventResult" == "ERROR"
