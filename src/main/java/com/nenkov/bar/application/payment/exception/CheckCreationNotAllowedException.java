package com.nenkov.bar.application.payment.exception;

import com.nenkov.bar.domain.model.session.TableSessionId;
import java.io.Serial;

/**
 * Thrown when creating a check is not allowed for the current session lifecycle/state.
 *
 * <p>This is mapped by the web layer to 409 Conflict with a stable {@code ApiProblemCode}.
 */
public final class CheckCreationNotAllowedException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  private final transient TableSessionId sessionId;

  public CheckCreationNotAllowedException(TableSessionId sessionId) {
    super("Check creation is not allowed for the current session state.");
    this.sessionId = sessionId;
  }

  public TableSessionId sessionId() {
    return sessionId;
  }
}
