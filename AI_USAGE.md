# AI Tools & Usage Documentation

## Executive Overview
This project was developed following a modern **AI-Assisted Technical Leadership** workflow. 

The **Lead Software Architect (Human)** was responsible for analyzing project ambiguities, defining system architecture, designing data models, enforcing engineering standards, reviewing generated code, and resolving complex edge cases. **AI tools were leveraged strictly as execution copilots** to accelerate boilerplates, draft code implementations, and automate test scaffolding under explicit prompt direction.

---

## Tools Used
- **DeepSeek V4 Pro Qwen3-235B-A22B (Local)** (via OpenCode / CLine) — Primary code execution copilot for Java refactoring, DTO creation, JPA mapping, and test scaffolding.
- **Gemini** — Strategic collaboration partner for prompt engineering, architectural trade-off analysis, and documentation formatting.

---

## Technical Leadership & Human Direction

### 1. Architecture & Decision Making (Human-Led)
- **Lazy TTL Strategy:** Made the executive decision to evaluate TTL expiration on-demand during status checks (`GET /traces/{traceId}/status`), eliminating unnecessary background schedulers,Quartz jobs, or external Redis/Kafka infrastructure.
- **Clock-Skew Prevention:** Mandated that TTL expectations be calculated relative to the **server receive time (`received_at`)** rather than client-supplied timestamps (`occurredAt`).
- **Data Model Architecture:** Designed the dual-table pattern (`traces` for fast state lookups and `events` for immutable auditing).

### 2. Prompts Engineered (Human-Designed)
All AI prompts were authored with strict technical context to minimize LLM inference overhead:
- **Requirements & Task Breakdown:** [`TASKS.md`](TASKS.md)
- **Database Schema DDL:** [`docker/init-scripts/db/init.sql`](docker/init-scripts/db/init.sql)
- **Implementation Prompt:** Saved in [`docs/prompts/deepseek_implementation_prompt.md`](docs/prompts/deepseek_implementation_prompt.md)
- **Testing Standard Prompt:** Instructed AI to generate unit tests using `shouldExpectedBehavior_WhenCondition` naming conventions with pure Mockito isolated tests.

---

## Manual Review, Debugging & Critical Adjustments (Human-Executed)

AI-generated drafts required active human intervention to fix subtle framework and persistence issues:

1. **Jackson 3.x Namespace Migration:** Resolved package import conflicts between legacy `com.fasterxml.jackson.*` and Spring Boot's modern `tools.jackson.*` namespace.
2. **PostgreSQL JSONB Mapping:** Fixed failing `@JdbcTypeCode(SqlTypes.JSON)` mappings by introducing `@ColumnTransformer(write = "?::jsonb")` combined with manual Jackson string serialization.
3. **Idempotency Control via `Persistable<String>`:** Identified a flaw where Spring Data JPA attempted silent `merge()` calls on duplicate events instead of rejecting them. Manually updated `EventEntity` to implement `Persistable<String>` with explicit `isNew() = true` semantics to enforce strict `persist()` constraint behavior.
4. **Validation & Exception Handling Improvements:** Enhanced global exception handlers to capture `HttpMessageNotReadableException` and handle multi-field validation error payloads gracefully.

---

## Accepted vs. Rejected AI Suggestions

| Suggestion | Origin | Action | Justification |
| :--- | :--- | :---: | :--- |
| **Lazy TTL Evaluation** | AI / Human Alignment | **Accepted** | Keeps the microservice stateless and avoids multi-node scheduler lock complexity. |
| **Dual-Table Materialized Pattern** | AI / Human Alignment | **Accepted** | Separates real-time state reads from audit logs efficiently. |
| **Kafka / RabbitMQ Queues** | AI Suggestion | **Rejected** | Over-engineered for challenge constraints; DB-first strategy is fully sufficient and durable. |
| **Spring `@Scheduled` Polling** | AI Suggestion | **Rejected** | Unnecessary resource consumption; lazy evaluation handles all requirements cleanly. |

---

## Summary
The combination of **Human Architectural Leadership** and **AI Code Execution** resulted in a production-grade MVP featuring **30/30 passing unit tests**, **100% passing Hurl E2E scenarios**, and a fully documented PostgreSQL persistence layer within the target 3–4 hour timeframe.