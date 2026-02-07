package com.nenkov.bar.application.session.exception;

/** Thrown when attempting to open a new session for a table that already has an OPEN session. */
public final class TableAlreadyHasOpenSessionException extends RuntimeException {

  public TableAlreadyHasOpenSessionException(String tableId) {
    super("Table already has an open session: " + tableId);
  }
}
