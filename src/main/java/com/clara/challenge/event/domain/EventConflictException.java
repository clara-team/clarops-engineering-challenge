package com.clara.challenge.event.domain;

public class EventConflictException extends RuntimeException {

  public EventConflictException(String message) {
    super(message);
  }
}
