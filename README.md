# Distributed Event Watchdog

Spring Boot implementation of the Clarops Distributed Event Watchdog challenge.

The service receives distributed events, tracks the state of a flow by `traceId`, and reports whether the flow is started, waiting for another event, completed, or expired because an expected event did not arrive within the configured TTL.

The original challenge statement is preserved in [CHALLENGE_INSTRUCTIONS.md](CHALLENGE_INSTRUCTIONS.md).

## Current Phase

Phase 2: database DDL.

This README captures the product and technical assumptions that guide the implementation, plus the current database design decisions.

## Scope

The MVP will expose two public endpoints under the existing `/api` context path:

```http
POST /events
GET /traces/{traceId}/status
```

The solution will stay intentionally small, but it will include enough production-aware behavior to make the flow reliable and explainable:

- immutable event history;
- current trace state for efficient status lookup;
- audit trail for state transitions;
- idempotent duplicate handling;
- conflict responses for invalid flow transitions;
- lazy TTL expiration when trace status is queried.

## Assumptions

|                           Assumption                            |                                                               Behavior                                                               |                                                Rationale                                                 |                                                Trade-off                                                 |
|-----------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| TTL is calculated from `occurredAt`.                            | `nextExpectedBefore = occurredAt + nextEventTtlSeconds`.                                                                             | The event timestamp represents when the upstream service completed the action.                           | If producers send delayed or incorrect timestamps, expiration can be earlier or later than receive time. |
| TTL expiration is evaluated lazily.                             | Expiration is checked when `GET /traces/{traceId}/status` is called.                                                                 | The challenge explicitly does not require a scheduler or background job.                                 | Expired traces are only detected when they are queried.                                                  |
| Lazy expiration is persisted.                                   | When expiration is detected, `trace_state` moves to `TTL_EXPIRED_FOR_EVENT`, `expired_at` is populated, and an audit row is written. | Persisting the result makes repeated reads consistent and creates an extension point for future alerts.  | A read endpoint can mutate state, so this must be documented and tested.                                 |
| Event history is immutable.                                     | Accepted events are stored in an `events` table and not updated.                                                                     | This supports auditability and debugging of distributed flows.                                           | Requires a separate `trace_state` table for current status lookup.                                       |
| Current state is stored separately.                             | `trace_state` stores one row per `traceId`.                                                                                          | Status reads should not need to recompute the full event stream.                                         | State transitions must keep `events` and `trace_state` consistent.                                       |
| Duplicate `eventId` is idempotent only for equivalent payloads. | If all relevant fields match, the request returns `200 OK` with current trace status.                                                | Safe retries should not create duplicate effects.                                                        | The implementation must compare fields explicitly.                                                       |
| Duplicate `eventId` with different payload is a conflict.       | The request returns `409 CONFLICT`.                                                                                                  | Same ID with different content is ambiguous and unsafe to accept.                                        | Clients must correct the event instead of relying on overwrite behavior.                                 |
| Unexpected event while waiting is rejected.                     | If a trace waits for `nextExpectedEvent`, any different event returns `409 CONFLICT` and does not mutate state.                      | The service models a strict expected-event flow.                                                         | More flexible branching workflows are out of scope.                                                      |
| Expected event after TTL is rejected.                           | The request returns `409 CONFLICT` and the trace remains `TTL_EXPIRED_FOR_EVENT`.                                                    | Once the SLA window expires, the flow should remain visibly expired.                                     | Late recovery would need an explicit remediation model, which is out of scope.                           |
| Completed traces are terminal.                                  | New events for a completed trace return `409 CONFLICT`.                                                                              | A final event marks the flow as closed.                                                                  | Reopening flows is not supported in this MVP.                                                            |
| `ERROR` is an event result, not a trace status.                 | Events with `result = ERROR` can still define a next expected event.                                                                 | The challenge only defines four trace statuses and allows event result to describe the upstream outcome. | Business-specific failure semantics are not modeled as separate trace statuses.                          |
| Metadata is stored as JSONB.                                    | The service stores `metadata` in PostgreSQL `JSONB`.                                                                                 | Metadata is flexible and not part of core transition rules.                                              | Querying metadata is out of scope for the MVP.                                                           |
| Unknown traces return `404 NOT FOUND`.                          | `GET /traces/{traceId}/status` returns 404 when no trace exists.                                                                     | Missing data is different from an invalid flow transition.                                               | Clients must distinguish not found from conflict responses.                                              |
| `trace_status_audit` records state transitions.                 | Important changes are appended with previous status, new status, reason, and event ID when available.                                | This supports debugging and future notification/alerting integrations.                                   | It adds write overhead and another table to maintain.                                                    |

## Technical Decisions

|       Topic       |                                    Decision                                    |
|-------------------|--------------------------------------------------------------------------------|
| Application stack | Spring Boot 4, Java 21, Maven, PostgreSQL                                      |
| API style         | REST JSON API                                                                  |
| Persistence       | JPA repositories backed by PostgreSQL tables                                   |
| Schema management | Extend the existing Docker init SQL; no Flyway or Liquibase                    |
| Status model      | `STARTED`, `WAITING_OTHER_EVENT`, `TTL_EXPIRED_FOR_EVENT`, `COMPLETED`         |
| Error format      | Standard JSON error response with code, message, optional traceId, and details |
| Validation        | Bean Validation annotations on request DTOs                                    |
| Testing focus     | Unit tests for business rules and Hurl tests for public HTTP behavior          |

## Data Model

The PostgreSQL DDL is defined in `docker/init-scripts/db/01-init-schema.sql`. The schema uses one
immutable history table, one current-state table, and one append-only audit table.

|        Table         |                                            Purpose                                            |                                                                                  Key columns                                                                                  |
|----------------------|-----------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `events`             | Stores accepted distributed events as immutable history.                                      | `id`, `event_id`, `trace_id`, `event_name`, `result`, `occurred_at`, `received_at`, `next_expected_event`, `next_event_ttl_seconds`, `metadata`                               |
| `trace_state`        | Stores the current status of each trace for efficient `GET /traces/{traceId}/status` lookups. | `trace_id`, `status`, `last_event_id`, `last_event_name`, `last_event_result`, `next_expected_event`, `next_expected_before`, `events_received`, `completed_at`, `expired_at` |
| `trace_status_audit` | Records state transitions and supports future alerting or debugging use cases.                | `id`, `trace_id`, `previous_status`, `new_status`, `reason`, `event_id`, `created_at`                                                                                         |

Important constraints and indexes:

- `events.event_id` is unique to support idempotency checks.
- `events.result` is restricted to `SUCCESS` or `ERROR`.
- `events.next_expected_event` and `events.next_event_ttl_seconds` must be provided together.
- `events.final_event` cannot be combined with `next_expected_event` or `next_event_ttl_seconds`.
- `trace_state.status` is restricted to `STARTED`, `WAITING_OTHER_EVENT`, `TTL_EXPIRED_FOR_EVENT`, or `COMPLETED`.
- `trace_state.events_received` defaults to `1` and must stay greater than `0` because a trace state exists only after the first accepted event.
- `trace_state` requires waiting traces to have both `next_expected_event` and `next_expected_before`.
- `trace_state` requires terminal timestamps for completed and expired traces.
- Indexes support event lookup by trace, status lookup, pending-expiration lookup, and audit lookup by trace or creation time.

## Initial Task Breakdown

1. Document assumptions, decisions, and trade-offs in this README.
2. Define PostgreSQL DDL for event history, trace state, and audit trail.
3. Define API request, response, and error DTOs.
4. Implement state transition logic outside controllers.
5. Implement persistence entities, repositories, and transactional service flow.
6. Expose `POST /events` and `GET /traces/{traceId}/status`.
7. Implement lazy TTL expiration on status reads.
8. Add unit tests for core business rules.
9. Add Hurl end-to-end tests for public API scenarios.
10. Complete final documentation and AI usage notes.
11. Run final verification.

## Running the Project

For local setup and run instructions, see [SETUP.md](SETUP.md).

At the beginning of Phase 1, the repository still contains only the baseline health endpoint:

```http
GET /api/health
```

