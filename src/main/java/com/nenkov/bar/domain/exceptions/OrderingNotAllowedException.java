package com.nenkov.bar.domain.exceptions;

/**
 * Thrown when ordering is not allowed for the current session state.
 *
 * <p>This is a domain-level invariant violation and should be mapped at the API boundary to a
 * stable RFC7807 response (e.g., 409 Conflict).
 */
public final class OrderingNotAllowedException extends DomainException {

  public OrderingNotAllowedException(String message) {
    super(message);
  }
}
