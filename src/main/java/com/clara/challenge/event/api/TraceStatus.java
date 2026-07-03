package com.clara.challenge.event.api;

public enum TraceStatus {
  STARTED,
  WAITING_OTHER_EVENT,
  TTL_EXPIRED_FOR_EVENT,
  COMPLETED
}
