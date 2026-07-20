package com.clara.challenge.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EventRequestDTOTest {

  private static Validator validator;

  @BeforeAll
  static void setUp() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @Test
  void shouldFailValidationWhenEventIdIsBlank() {
    EventRequestDTO dto =
        new EventRequestDTO("", "trace-001", "EVENT", "SUCCESS", Instant.now(), null, null, false, null);

    Set<ConstraintViolation<EventRequestDTO>> violations = validator.validate(dto);

    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(
        v -> v.getMessageTemplate().equals("{jakarta.validation.constraints.NotBlank.message}"));
  }

  @Test
  void shouldFailValidationWhenTraceIdIsBlank() {
    EventRequestDTO dto =
        new EventRequestDTO("evt-001", "", "EVENT", "SUCCESS", Instant.now(), null, null, false, null);

    Set<ConstraintViolation<EventRequestDTO>> violations = validator.validate(dto);

    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("traceId"));
  }

  @Test
  void shouldFailValidationWhenOccurredAtIsNull() {
    EventRequestDTO dto =
        new EventRequestDTO("evt-001", "trace-001", "EVENT", "SUCCESS", null, null, null, false, null);

    Set<ConstraintViolation<EventRequestDTO>> violations = validator.validate(dto);

    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("occurredAt"));
  }

  @Test
  void shouldPassValidationWhenAllRequiredFieldsArePresent() {
    EventRequestDTO dto =
        new EventRequestDTO(
            "evt-001", "trace-001", "EVENT", "SUCCESS",
            Instant.now(), null, null, false, null);

    Set<ConstraintViolation<EventRequestDTO>> violations = validator.validate(dto);

    assertThat(violations).isEmpty();
  }

  @Test
  void shouldFailValidationWhenNextEventTtlSecondsIsNegative() {
    EventRequestDTO dto =
        new EventRequestDTO(
            "evt-001", "trace-001", "EVENT", "SUCCESS",
            Instant.now(), "NEXT_EVENT", -1, false, null);

    Set<ConstraintViolation<EventRequestDTO>> violations = validator.validate(dto);

    assertThat(violations).isNotEmpty();
    assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("nextEventTtlSeconds"));
  }
}
