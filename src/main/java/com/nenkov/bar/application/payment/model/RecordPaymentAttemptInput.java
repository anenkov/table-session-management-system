package com.nenkov.bar.application.payment.model;

import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.Objects;

/**
 * Input model for initiating a payment attempt for an existing check.
 *
 * <p>The handler will load the check to get the amount and keep the application orchestration safe.
 */
public record RecordPaymentAttemptInput(
    PaymentRequestId requestId, TableSessionId sessionId, CheckId checkId) {

  public RecordPaymentAttemptInput {
    Objects.requireNonNull(requestId, "requestId must not be null");
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(checkId, "checkId must not be null");
  }
}
