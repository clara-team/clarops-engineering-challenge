package com.clara.challenge.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DuplicateEventExceptionTest {

  @Test
  void shouldContainEventIdInMessage() {
    DuplicateEventException ex = new DuplicateEventException("Event already processed: evt-001");

    assertThat(ex.getMessage()).isEqualTo("Event already processed: evt-001");
  }

  @Test
  void shouldBeRuntimeException() {
    DuplicateEventException ex = new DuplicateEventException("test");
    assertThat(ex).isInstanceOf(RuntimeException.class);
  }
}
