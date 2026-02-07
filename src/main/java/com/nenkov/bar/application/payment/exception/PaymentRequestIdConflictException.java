package com.nenkov.bar.application.payment.exception;

import com.nenkov.bar.application.payment.model.PaymentRequestId;
import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.Objects;

/**
 * Thrown when the same {@link PaymentRequestId} is reused for a different target pair (sessionId,
 * checkId) than the one it was originally processed for.
 */
public final class PaymentRequestIdConflictException extends RuntimeException {

  public PaymentRequestIdConflictException(
      PaymentRequestId requestId,
      TableSessionId existingSessionId,
      CheckId existingCheckId,
      TableSessionId incomingSessionId,
      CheckId incomingCheckId) {
    super(
        "PaymentRequestId conflict: requestId="
            + Objects.requireNonNull(requestId, "requestId must not be null").value()
            + ", existing=("
            + Objects.requireNonNull(existingSessionId, "existingSessionId must not be null")
                .value()
            + ", "
            + Objects.requireNonNull(existingCheckId, "existingCheckId must not be null").value()
            + "), incoming=("
            + Objects.requireNonNull(incomingSessionId, "incomingSessionId must not be null")
                .value()
            + ", "
            + Objects.requireNonNull(incomingCheckId, "incomingCheckId must not be null").value()
            + ")");
  }
}
