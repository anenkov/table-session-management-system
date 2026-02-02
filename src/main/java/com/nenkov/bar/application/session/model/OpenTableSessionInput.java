package com.nenkov.bar.application.session.model;

import java.util.Objects;

/**
 * Input model for opening a new table session.
 *
 * <p>Application-level model. Keeps orchestration stable and free of framework/UI concerns.
 */
public record OpenTableSessionInput(String tableId) {

  public OpenTableSessionInput {
    Objects.requireNonNull(tableId, "tableId must not be null");
    if (tableId.isBlank()) {
      throw new IllegalArgumentException("tableId must not be blank");
    }
  }
}
