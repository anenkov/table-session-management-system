package com.nenkov.bar.application.session.exception;

import com.nenkov.bar.domain.model.session.TableSessionId;

/** Thrown when a session workflow requires a session that does not exist. */
public final class TableSessionNotFoundException extends RuntimeException {

  public TableSessionNotFoundException(TableSessionId sessionId) {
    super("TableSession not found: " + sessionId.value());
  }
}
