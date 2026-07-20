package com.clara.challenge.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TraceNotFoundExceptionTest {

  @Test
  void shouldContainTraceIdInMessage() {
    TraceNotFoundException ex = new TraceNotFoundException("trace-001");

    assertThat(ex.getMessage()).isEqualTo("Trace not found: trace-001");
    assertThat(ex).isInstanceOf(RuntimeException.class);
  }

  @Test
  void shouldBeRuntimeException() {
    TraceNotFoundException ex = new TraceNotFoundException("any");
    assertThat(ex).isInstanceOf(RuntimeException.class);
  }
}
