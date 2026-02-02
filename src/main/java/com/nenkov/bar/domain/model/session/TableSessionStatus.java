package com.nenkov.bar.domain.model.session;

/**
 * Lifecycle status of a {@link TableSession}.
 *
 * <p>Minimal status set for the application use-case layer to orchestrate lifecycle transitions.
 * More states (e.g. PAID) can be introduced later when domain rules are implemented.
 */
public enum TableSessionStatus {
  OPEN,
  CLOSED
}
