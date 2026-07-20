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


-- ------------------------------------------------------------
-- traces
-- Stores the consolidated, current state of each distributed flow.
-- Serves fast read operations for status checks without requiring
-- full event sequence recalculation on every request.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS traces (
    trace_id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    last_event_name VARCHAR(255) NOT NULL,
    last_event_result VARCHAR(50) NOT NULL,
    next_expected_event VARCHAR(255),
    next_expected_before TIMESTAMP WITH TIME ZONE,
    events_count INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ------------------------------------------------------------
-- events
-- Immutable event log containing all received payloads for audit,
-- history tracking, and idempotency control.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
    event_id VARCHAR(255) PRIMARY KEY,
    trace_id VARCHAR(255) NOT NULL REFERENCES traces(trace_id) ON DELETE CASCADE,
    event_name VARCHAR(255) NOT NULL,
    result VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_expected_event VARCHAR(255),
    next_event_ttl_seconds INTEGER,
    final_event BOOLEAN NOT NULL DEFAULT FALSE,
    metadata JSONB
);

-- ------------------------------------------------------------
-- Indexes
-- Optimize lookup efficiency for status updates and trace timeline audits.
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_events_trace_id ON events(trace_id);
CREATE INDEX IF NOT EXISTS idx_traces_status ON traces(status);