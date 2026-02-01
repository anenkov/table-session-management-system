package com.nenkov.bar.application.common.persistence;

/**
 * Signals a technical failure when accessing persistence.
 *
 * <p>Not used for domain/business errors and not used for "not found" cases.
 */
public final class RepositoryAccessException extends RuntimeException {

  public RepositoryAccessException(String message, Throwable cause) {
    super(message, cause);
  }

  public RepositoryAccessException(String message) {
    super(message);
  }
}
