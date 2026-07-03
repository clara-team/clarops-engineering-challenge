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
      'clarops event watchdog'
    WHERE
      NOT EXISTS(
        SELECT
          1
        FROM
          health
      );

-- -------------------------
-- events
-- Immutable history of accepted distributed events.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS events(
      id UUID PRIMARY KEY,
      event_id VARCHAR(120) NOT NULL,
      trace_id VARCHAR(120) NOT NULL,
      event_name VARCHAR(120) NOT NULL,
      RESULT VARCHAR(20) NOT NULL,
      occurred_at TIMESTAMPTZ NOT NULL,
      received_at TIMESTAMPTZ NOT NULL,
      next_expected_event VARCHAR(120),
      next_event_ttl_seconds INTEGER,
      final_event BOOLEAN NOT NULL DEFAULT FALSE,
      metadata JSONB,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      CONSTRAINT uk_events_event_id UNIQUE(event_id),
      CONSTRAINT chk_events_result CHECK(
        RESULT IN(
          'SUCCESS',
          'ERROR'
        )
      ),
      CONSTRAINT chk_events_next_event_ttl_seconds CHECK(
        next_event_ttl_seconds IS NULL
        OR next_event_ttl_seconds > 0
      ),
      CONSTRAINT chk_events_next_expected_pair CHECK(
        (
          next_expected_event IS NULL
          AND next_event_ttl_seconds IS NULL
        )
        OR(
          next_expected_event IS NOT NULL
          AND next_event_ttl_seconds IS NOT NULL
        )
      ),
      CONSTRAINT chk_events_final_event_has_no_next_expected CHECK(
        final_event = FALSE
        OR(
          next_expected_event IS NULL
          AND next_event_ttl_seconds IS NULL
        )
      )
    );

CREATE
  INDEX IF NOT EXISTS idx_events_trace_id ON
  events(trace_id);

CREATE
  INDEX IF NOT EXISTS idx_events_trace_id_occurred_at ON
  events(
    trace_id,
    occurred_at
  );

-- -------------------------
-- trace_state
-- Current state per trace for efficient status lookup.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS trace_state(
      trace_id VARCHAR(120) PRIMARY KEY,
      status VARCHAR(40) NOT NULL,
      last_event_id VARCHAR(120) NOT NULL,
      last_event_name VARCHAR(120) NOT NULL,
      last_event_result VARCHAR(20) NOT NULL,
      last_event_occurred_at TIMESTAMPTZ NOT NULL,
      next_expected_event VARCHAR(120),
      next_expected_before TIMESTAMPTZ,
      events_received INTEGER NOT NULL DEFAULT 1,
      completed_at TIMESTAMPTZ,
      expired_at TIMESTAMPTZ,
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      CONSTRAINT fk_trace_state_last_event FOREIGN KEY(last_event_id) REFERENCES events(event_id),
      CONSTRAINT chk_trace_state_status CHECK(
        status IN(
          'STARTED',
          'WAITING_OTHER_EVENT',
          'TTL_EXPIRED_FOR_EVENT',
          'COMPLETED'
        )
      ),
      CONSTRAINT chk_trace_state_last_event_result CHECK(
        last_event_result IN(
          'SUCCESS',
          'ERROR'
        )
      ),
      CONSTRAINT chk_trace_state_events_received CHECK(
        events_received > 0
      ),
      CONSTRAINT chk_trace_state_waiting_pair CHECK(
        (
          status = 'WAITING_OTHER_EVENT'
          AND next_expected_event IS NOT NULL
          AND next_expected_before IS NOT NULL
        )
        OR status <> 'WAITING_OTHER_EVENT'
      ),
      CONSTRAINT chk_trace_state_completed_at CHECK(
        (
          status = 'COMPLETED'
          AND completed_at IS NOT NULL
        )
        OR status <> 'COMPLETED'
      ),
      CONSTRAINT chk_trace_state_expired_at CHECK(
        (
          status = 'TTL_EXPIRED_FOR_EVENT'
          AND expired_at IS NOT NULL
        )
        OR status <> 'TTL_EXPIRED_FOR_EVENT'
      )
    );

CREATE
  INDEX IF NOT EXISTS idx_trace_state_status ON
  trace_state(status);

CREATE
  INDEX IF NOT EXISTS idx_trace_state_next_expected_before ON
  trace_state(next_expected_before);

-- -------------------------
-- trace_status_audit
-- Append-only state transition log and future alerting extension point.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS trace_status_audit(
      id UUID PRIMARY KEY,
      trace_id VARCHAR(120) NOT NULL,
      previous_status VARCHAR(40),
      new_status VARCHAR(40) NOT NULL,
      reason VARCHAR(80) NOT NULL,
      event_id VARCHAR(120),
      created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
      CONSTRAINT fk_trace_status_audit_trace FOREIGN KEY(trace_id) REFERENCES trace_state(trace_id),
      CONSTRAINT fk_trace_status_audit_event FOREIGN KEY(event_id) REFERENCES events(event_id),
      CONSTRAINT chk_trace_status_audit_previous_status CHECK(
        previous_status IS NULL
        OR previous_status IN(
          'STARTED',
          'WAITING_OTHER_EVENT',
          'TTL_EXPIRED_FOR_EVENT',
          'COMPLETED'
        )
      ),
      CONSTRAINT chk_trace_status_audit_new_status CHECK(
        new_status IN(
          'STARTED',
          'WAITING_OTHER_EVENT',
          'TTL_EXPIRED_FOR_EVENT',
          'COMPLETED'
        )
      ),
      CONSTRAINT chk_trace_status_audit_reason CHECK(
        reason IN(
          'TRACE_CREATED',
          'NEXT_EVENT_EXPECTED',
          'EXPECTED_EVENT_RECEIVED',
          'FINAL_EVENT_RECEIVED',
          'TTL_EXPIRED',
          'DUPLICATE_EVENT_IDEMPOTENT'
        )
      )
    );

CREATE
  INDEX IF NOT EXISTS idx_trace_status_audit_trace_id ON
  trace_status_audit(trace_id);

CREATE
  INDEX IF NOT EXISTS idx_trace_status_audit_created_at ON
  trace_status_audit(created_at);
