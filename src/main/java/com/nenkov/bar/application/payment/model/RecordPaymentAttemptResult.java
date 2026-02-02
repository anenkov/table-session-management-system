package com.nenkov.bar.application.payment.model;

import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.Objects;

/** Result model for a payment attempt. */
public record RecordPaymentAttemptResult(
    PaymentRequestId requestId,
    TableSessionId sessionId,
    CheckId checkId,
    PaymentAttemptResult attemptResult) {

  public RecordPaymentAttemptResult {
    Objects.requireNonNull(requestId, "requestId must not be null");
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(checkId, "checkId must not be null");
    Objects.requireNonNull(attemptResult, "attemptResult must not be null");
  }
}
