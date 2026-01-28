package com.nenkov.bar.domain.exceptions;

/**
 * Base type for all domain-level exceptions.
 *
 * <p>Domain exceptions signal that a business rule or invariant was violated. They are not
 * technical failures (DB/network) and should be handled at the application boundary (e.g., mapping
 * to an appropriate API response).
 */
public abstract class DomainException extends RuntimeException {

  protected DomainException(String message) {
    super(message);
  }

  protected DomainException(String message, Throwable cause) {
    super(message, cause);
  }
}
