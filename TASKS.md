# Implementation Tasks

This task list tracks the implementation phases for the challenge solution.

## Phase 1 - Define Assumptions

- [x] Preserve the original challenge statement as `CHALLENGE_INSTRUCTIONS.md`.
- [x] Create a solution-focused `README.md`.
- [x] Document MVP scope.
- [x] Document assumptions, behavior, rationale, and trade-offs.
- [x] Document initial technical decisions.
- [x] Document the implementation task breakdown.

## Phase 2 - Define DDL

- [x] Extend `docker/init-scripts/db/01-init-schema.sql`.
- [x] Add the immutable `events` table.
- [x] Add the `trace_state` table.
- [x] Add the `trace_status_audit` table.
- [x] Add constraints and indexes for idempotency and lookup paths.

## Phase 3 - Define API DTOs

- [x] Add `EventRequest`.
- [x] Add `TraceStatusResponse`.
- [x] Add `ErrorResponse`.
- [x] Add validation annotations.
- [x] Define enum values used by the API.

## Phase 4 - Implement Domain Logic

- [x] Add trace status transition rules.
- [x] Add event result handling.
- [x] Add domain exceptions for conflicts and missing traces.
- [x] Add explicit duplicate payload comparison.
- [x] Keep business rules outside controllers.

## Phase 5 - Implement Persistence

- [ ] Add event entity and repository.
- [ ] Add trace state entity and repository.
- [ ] Add trace status audit entity and repository.
- [ ] Implement transactional event ingestion.
- [ ] Keep event history and current state consistent.

## Phase 6 - Implement Endpoints

- [ ] Add `POST /events`.
- [ ] Add `GET /traces/{traceId}/status`.
- [ ] Add global exception handling.
- [ ] Return standard error responses.
- [ ] Return correct HTTP status codes.

## Phase 7 - Implement Lazy Expiration

- [ ] Detect expired waiting traces on status reads.
- [ ] Persist `TTL_EXPIRED_FOR_EVENT`.
- [ ] Populate `expired_at`.
- [ ] Write `TTL_EXPIRED` audit rows.
- [ ] Keep repeated status reads idempotent.

## Phase 8 - Add Unit Tests

- [ ] Test first event creates `STARTED`.
- [ ] Test first event with next expected event creates `WAITING_OTHER_EVENT`.
- [ ] Test final event creates `COMPLETED`.
- [ ] Test TTL expiration creates `TTL_EXPIRED_FOR_EVENT`.
- [ ] Test expected event before TTL advances state.
- [ ] Test expected event after TTL is rejected.
- [ ] Test unexpected event is rejected.
- [ ] Test equivalent duplicate event is idempotent.
- [ ] Test different duplicate event returns conflict.
- [ ] Test completed trace rejects new events.

## Phase 9 - Add Hurl E2E Tests

- [ ] Add started flow scenario.
- [ ] Add waiting-other-event flow scenario.
- [ ] Add completed flow scenario.
- [ ] Add TTL-expired flow scenario.
- [ ] Add unexpected-event conflict scenario.
- [ ] Add duplicate-idempotent flow scenario.
- [ ] Add late-event conflict scenario.
- [ ] Add unknown-trace scenario.

## Phase 10 - Document Final Solution

- [ ] Update README with final API examples.
- [ ] Update README with final data model.
- [ ] Update README with final test commands.
- [x] Add `AI_USAGE.md`.
- [ ] Finalize accepted and rejected AI suggestions.

## Phase 11 - Verify

- [ ] Run Maven verification.
- [ ] Run formatting checks.
- [ ] Start the app with Docker/PostgreSQL.
- [ ] Run Hurl tests.
- [ ] Do a final README review.

