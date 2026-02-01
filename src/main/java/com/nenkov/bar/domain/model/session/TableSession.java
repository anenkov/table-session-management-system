package com.nenkov.bar.domain.model.session;

import java.util.Objects;

/**
 * Aggregate root representing a single table session (canonical tab for a table).
 *
 * <p>This is a minimal domain skeleton introduced to enable application-layer contracts to compile.
 *
 * <p>Behavior, invariants, and lifecycle rules will be introduced in later phases.
 */
public record TableSession(TableSessionId id) {

  public TableSession {
    Objects.requireNonNull(id, "id");
  }
}
