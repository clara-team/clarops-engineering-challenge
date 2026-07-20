# [H3] The event promised another one a long time ago and it never arrived.
# This test does NOT sleep: the occurredAt is far in the past, so the deadline
# was already gone before we asked. That works because we count the TTL from
# occurredAt and not from the moment the event reached us.

POST http://localhost:8080/api/events
Content-Type: application/json
{
  "eventId": "{{newUuid}}",
  "traceId": "hurl-expired",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2020-01-01T00:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 60
}
HTTP 201

GET http://localhost:8080/api/traces/hurl-expired/status
HTTP 200
[Asserts]
jsonpath "$.traceId" == "hurl-expired"
jsonpath "$.status" == "TTL_EXPIRED_FOR_EVENT"
jsonpath "$.nextExpectedEvent" == "RULES_EVALUATED"
