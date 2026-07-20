-- ============================================================
-- Clarops Challenge — Initial Schema
-- Distributed event tracking, TTL expiration, and operational
-- flow analysis.
-- Idempotent — safe to re-execute.
-- UUIDs must be provided by the application layer.
-- ============================================================
CREATE
  SCHEMA IF NOT EXISTS clarops_challenge_schema;
SET
search_path TO clarops_challenge_schema;

-- -------------------------
-- health
-- Single-row table used by the health endpoint.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS health(
      id BIGSERIAL PRIMARY KEY,
      message VARCHAR(255) NOT NULL
    );

INSERT
  INTO
    health(message) SELECT
      'clarops sr engineer challenge'
    WHERE
      NOT EXISTS(
        SELECT
          1
        FROM
          health
      );

-- -------------------------
-- events
-- One row per event received. event_id is the primary key on purpose:
-- deduplication is a constraint, not application logic.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS events(
      event_id TEXT PRIMARY KEY,
      trace_id TEXT NOT NULL,
      event_name TEXT NOT NULL,
      result TEXT NOT NULL CHECK(result IN('SUCCESS','ERROR')),
      occurred_at TIMESTAMPTZ NOT NULL,
      next_expected_event TEXT,
      next_event_ttl_seconds INTEGER,
      final_event BOOLEAN NOT NULL DEFAULT FALSE,
      metadata JSONB,
      received_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_events_trace_id ON
events(trace_id);

-- -------------------------
-- trace_state
-- One row per trace. Facts only: there is NO status column.
-- The status is calculated when somebody asks.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS trace_state(
      trace_id TEXT PRIMARY KEY,
      last_event_name TEXT NOT NULL,
      last_event_result TEXT NOT NULL,
      last_occurred_at TIMESTAMPTZ NOT NULL,
      next_expected_event TEXT,
      next_expected_before TIMESTAMPTZ,
      completed_at TIMESTAMPTZ,
      events_received INTEGER NOT NULL DEFAULT 0
    );
