package com.clara.challenge.event.domain;

public enum TransitionReason {
  TRACE_CREATED,
  NEXT_EVENT_EXPECTED,
  EXPECTED_EVENT_RECEIVED,
  FINAL_EVENT_RECEIVED,
  TTL_EXPIRED,
  DUPLICATE_EVENT_IDEMPOTENT
}
