package com.nenkov.bar.application.payment.exception;

import com.nenkov.bar.domain.model.session.TableSessionId;
import java.io.Serial;

/**
 * Thrown when a well-formed check creation request violates business rules (e.g., invalid item id
 * or over-selected quantity).
 *
 * <p>This is mapped by the web layer to 422 Unprocessable Entity with a stable {@code
 * ApiProblemCode}.
 */
public final class InvalidPaymentSelectionException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final transient TableSessionId sessionId;

  public InvalidPaymentSelectionException(TableSessionId sessionId) {
    super("Invalid payment selection.");
    this.sessionId = sessionId;
  }

  public TableSessionId sessionId() {
    return sessionId;
  }
}
