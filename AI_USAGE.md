# AI Usage

This document records how AI assistance was used during the challenge implementation.

The file is updated incrementally at the end of each implementation phase so the final submission shows which suggestions were accepted, rejected, or manually adjusted.

## Tools Used

| Tool                            | Usage                                                                                                 |
|---------------------------------|-------------------------------------------------------------------------------------------------------|
| OpenCode                        | Repository exploration, implementation assistance, documentation drafting, and verification commands. |
| Engram                          | Session memory for preserving planning decisions and implementation context across work sessions.     |
| PR description writer subagent  | Drafting, creating, and editing GitHub PR descriptions from the repository PR template.               |
| OpenCode `/review` command      | Built-in review command for branch and diff inspection before accepting or documenting changes.       |
| Codex cloud code reviewer       | Automated GitHub PR review comments used as an additional correctness and regression signal.          |

## Models Used

| Model                  | Role                                                    |
|------------------------|---------------------------------------------------------|
| GPT-5.5                | Main coding agent used through OpenCode.                |
| GPT-5.4 Mini Fast      | PR description writer subagent used for GitHub PR body. |

## Phase 1 - Assumptions and Documentation

### Prompts and Requests

- Asked the assistant to analyze the repository and explain the challenge.
- Asked for a manual implementation plan before writing code.
- Asked to normalize implementation phases and prompts.
- Asked to start Phase 1 by preserving the original challenge instructions and creating a new solution README.

### Accepted Suggestions

- Preserve the original challenge statement as `CHALLENGE_INSTRUCTIONS.md`.
- Create a solution-focused `README.md` with assumptions, trade-offs, technical decisions, and task breakdown.
- Track implementation work with a phase-based `TASKS.md`.

### Rejected or Adjusted Suggestions

- Internal planning notes were kept outside the final challenge documentation.

### Manual Corrections

- Pull requests must be created explicitly inside the fork repository, not against the original upstream repository.

## Phase 2 - Database DDL

### Prompts and Requests

Main DDL prompt:

> Start Phase 2 and implement the PostgreSQL DDL for the Event Watchdog MVP.
>
> Use the challenge requirements and the decisions already documented in `README.md`:
>
> - keep immutable event history;
> - keep current trace state for efficient status lookup;
> - add an audit trail for state transitions;
> - support idempotency checks with a unique `eventId`;
> - support strict statuses: `STARTED`, `WAITING_OTHER_EVENT`, `TTL_EXPIRED_FOR_EVENT`, and `COMPLETED`;
> - support event results `SUCCESS` and `ERROR`;
> - calculate TTL from `occurredAt` plus `nextEventTtlSeconds`;
> - persist lazy TTL expiration;
> - store flexible event metadata as PostgreSQL `JSONB`;
> - add constraints and indexes that make these assumptions explicit;
> - extend the existing Docker init SQL instead of adding Flyway or Liquibase.
>
> Use this table structure as the target design:
>
> - `events`: `id UUID PRIMARY KEY`, `event_id VARCHAR(120) UNIQUE NOT NULL`, `trace_id VARCHAR(120) NOT NULL`, `event_name VARCHAR(120) NOT NULL`, `result VARCHAR(20) NOT NULL`, `occurred_at TIMESTAMPTZ NOT NULL`, `received_at TIMESTAMPTZ NOT NULL`, `next_expected_event VARCHAR(120)`, `next_event_ttl_seconds INTEGER`, `final_event BOOLEAN NOT NULL DEFAULT FALSE`, `metadata JSONB`, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`;
> - `trace_state`: `trace_id VARCHAR(120) PRIMARY KEY`, `status VARCHAR(40) NOT NULL`, `last_event_id VARCHAR(120) NOT NULL`, `last_event_name VARCHAR(120) NOT NULL`, `last_event_result VARCHAR(20) NOT NULL`, `last_event_occurred_at TIMESTAMPTZ NOT NULL`, `next_expected_event VARCHAR(120)`, `next_expected_before TIMESTAMPTZ`, `events_received INTEGER NOT NULL DEFAULT 1`, `completed_at TIMESTAMPTZ`, `expired_at TIMESTAMPTZ`, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`, `updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`;
> - `trace_status_audit`: `id UUID PRIMARY KEY`, `trace_id VARCHAR(120) NOT NULL`, `previous_status VARCHAR(40)`, `new_status VARCHAR(40) NOT NULL`, `reason VARCHAR(80) NOT NULL`, `event_id VARCHAR(120)`, `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()`.
>
> Add these indexes for expected lookup paths:
>
> - `events(trace_id)` for event history lookup by trace;
> - `events(trace_id, occurred_at)` for ordered event history inspection;
> - `trace_state(status)` for status-based lookups;
> - `trace_state(next_expected_before)` for pending TTL expiration lookup;
> - `trace_status_audit(trace_id)` for audit lookup by trace;
> - `trace_status_audit(created_at)` for chronological audit inspection.
>
> Add a constraint so `final_event = true` cannot be combined with `next_expected_event` or `next_event_ttl_seconds`, because a terminal event must not also declare another expected event.
>
> Keep the implementation minimal and explain any trade-offs or alternatives that should be documented.

Additional requests:

- "Should `AI_USAGE.md` be updated incrementally by phase so the final submission documents the AI usage clearly?"
- "Create the local Docker environment file and make sure the Compose volume name does not conflict with other projects."
- "Use local defaults that avoid port conflicts: Spring Boot on `8081` and PostgreSQL exposed on `5433`."
- "Help me configure the local machine to use Java 21 with asdf instead of the Java version installed by Homebrew."
- "Re-run `./mvnw spotless:check` now that Java 21 is configured, and document the Java 21 `.tool-versions` setup."
- "Verify whether everything is in order before committing the Phase 2 changes."

### Accepted Suggestions

- Create `AI_USAGE.md` early and update it incrementally after each phase.
- Extend the existing Docker init SQL instead of adding Flyway or Liquibase.
- Add an immutable `events` table for event history.
- Add a `trace_state` table for efficient current status lookup.
- Add a `trace_status_audit` table as a state transition audit log and future alerting extension point.
- Add constraints for event result, trace status, positive TTL values, required waiting-state fields, terminal events, completed timestamps, and expired timestamps.
- Add indexes for event trace lookup, trace status lookup, pending expiration lookup, and audit lookup.

### Rejected or Adjusted Suggestions

- No separate migration framework was added because the challenge repository already uses Docker initialization SQL and explicitly does not require extra infrastructure.
- Payload hashing was not added to the DDL; explicit field comparison remains the MVP approach, with hashing left as a future improvement.

### Manual Corrections

- The local Java runtime was corrected to Java 21 with asdf before using `./mvnw spotless:check` as a validation signal.
