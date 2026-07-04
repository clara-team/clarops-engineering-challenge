package com.clara.challenge.event.domain;

public enum TransitionReason {
  TRACE_STARTED,
  WAITING_FOR_NEXT_EVENT,
  TRACE_COMPLETED,
  EXPECTED_EVENT_RECEIVED
}
