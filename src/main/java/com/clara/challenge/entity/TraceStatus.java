package com.clara.challenge.entity;

/**
 * Possible states of a distributed flow trace.
 *
 * <p>Serialized as a string in JSON (e.g. {@code "STARTED"}, not an ordinal) via
 * {@code @Enumerated(EnumType.STRING)} on {@link TraceEntity#status}.</p>
 *
 * <h3>State definitions</h3>
 * <ul>
 *   <li>{@link #STARTED} — first event received, no next event expected, flow not completed</li>
 *   <li>{@link #WAITING_OTHER_EVENT} — latest event defined a next expected event with a valid TTL</li>
 *   <li>{@link #TTL_EXPIRED_FOR_EVENT} — expected event did not arrive before the TTL deadline</li>
 *   <li>{@link #COMPLETED} — a received event had {@code finalEvent = true}</li>
 * </ul>
 */
public enum TraceStatus {
  /** First event received, but there is no next expected event defined and the flow is not completed. */
  STARTED,
  /** The latest event defined a next expected event and its TTL has not expired yet. */
  WAITING_OTHER_EVENT,
  /** The expected next event did not arrive before the configured TTL expired. */
  TTL_EXPIRED_FOR_EVENT,
  /** A received event marked the flow as completed using {@code finalEvent = true}. */
  COMPLETED
}
