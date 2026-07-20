package com.clara.challenge.events;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "clarops_challenge_schema", name = "events")
public class ReceivedEvent {

  @Id private String eventId;

  @Column(nullable = false)
  private String traceId;

  @Column(nullable = false)
  private String eventName;

  @Column(nullable = false)
  private String result;

  @Column(nullable = false)
  private Instant occurredAt;

  private String nextExpectedEvent;

  private Integer nextEventTtlSeconds;

  @Column(nullable = false)
  private boolean finalEvent;
}
