# Implementation Tasks Breakdown

This document outlines the task decomposition used to guide the development process. Tasks were broken down into small, well-defined units to minimize LLM inference overhead and ensure deterministic implementation.

---

## Phase 1 — Database & Data Architecture
- [x] **Task 1.1:** Design the dual-storage data model (`traces` and `events`).
- [x] **Task 1.2:** Generate Mermaid ER diagrams and export SVG documentation under `docs/db/`.
- [x] **Task 1.3:** Draft the idempotent PostgreSQL DDL script in `docker/init-scripts/db/init.sql` with schema, tables, constraints, default values, and performance indexes.

## Phase 2 — Core API Contract & Data Layer
- [x] **Task 2.1:** Create JPA Entities (`TraceEntity`, `EventEntity`) mapped to `clarops_challenge_schema`.
- [x] **Task 2.2:** Define DTOs (`EventRequestDTO`, `TraceStatusResponseDTO`) with Jakarta validation annotations (`@NotBlank`, `@NotNull`, `@Positive`).
- [x] **Task 2.3:** Implement Spring Data JPA Repositories (`TraceRepository`, `EventRepository`) with custom query interfaces if necessary.

## Phase 3 — Business Logic & State Machine
- [x] **Task 3.1:** Implement `WatchdogService.processEvent()` handling trace creation, multi-event progression, idempotency checking, and state transitions.
- [x] **Task 3.2:** Implement `WatchdogService.getTraceStatus()` featuring lazy/on-demand TTL expiration evaluation.
- [x] **Task 3.3:** Implement `@RestControllerAdvice` (`GlobalExceptionHandler`) mapping custom domain exceptions (`DuplicateEventException`, `TraceNotFoundException`) to appropriate HTTP status codes (400, 404, 409).

## Phase 4 — Testing & Verification
- [x] **Task 4.1:** Define unit testing standards and write unit tests for domain logic, controllers, exception handlers, and DTOs using Mockito.
- [x] **Task 4.2:** Write Hurl E2E test scripts in `hurl/` covering `STARTED`, `WAITING_OTHER_EVENT`, `COMPLETED`, and `TTL_EXPIRED_FOR_EVENT` scenarios.
- [x] **Task 4.3:** Validate that 100% of unit tests and Hurl E2E tests pass.

## Phase 5 — Documentation
- [x] **Task 5.1:** Update `README.md` with trade-offs, design decisions, Open Questions resolutions, and execution steps.
- [x] **Task 5.2:** Create `AI_USAGE.md` documenting AI tools, prompts used, accepted/rejected suggestions, and manual corrections.
