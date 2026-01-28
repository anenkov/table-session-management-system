package com.nenkov.bar.domain.exceptions;

/**
 * Thrown when a domain object is in an illegal state for the attempted operation.
 *
 * <p>This exception is used to enforce domain invariants such as invalid state transitions or
 * disallowed operations given the current state.
 */
public final class IllegalDomainStateException extends DomainException {

  public IllegalDomainStateException(String message) {
    super(message);
  }
}
