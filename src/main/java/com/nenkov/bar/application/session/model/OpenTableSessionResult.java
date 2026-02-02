package com.nenkov.bar.application.session.model;

import java.util.Objects;

/**
 * Result model for opening a new table session.
 *
 * <p>Returns minimal identifiers for continuing workflows without exposing domain internals.
 */
public record OpenTableSessionResult(String sessionId, String tableId) {

  public OpenTableSessionResult {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(tableId, "tableId must not be null");
    if (sessionId.isBlank()) {
      throw new IllegalArgumentException("sessionId must not be blank");
    }
    if (tableId.isBlank()) {
      throw new IllegalArgumentException("tableId must not be blank");
    }
  }
}
