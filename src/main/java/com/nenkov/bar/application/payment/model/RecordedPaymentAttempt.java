package com.nenkov.bar.application.payment.model;

import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.session.TableSessionId;
import java.util.Objects;

/**
 * Application-level idempotency record for payment initiation requests.
 *
 * <p>Stores the first processed outcome for a given {@link PaymentRequestId}. Replays with the same
 * request id can be served deterministically without gateway calls or additional writes.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>requestId, sessionId, checkId, attemptResult are non-null
 * </ul>
 */
public record RecordedPaymentAttempt(
    PaymentRequestId requestId,
    TableSessionId sessionId,
    CheckId checkId,
    PaymentAttemptResult attemptResult) {

  public RecordedPaymentAttempt {
    Objects.requireNonNull(requestId, "requestId must not be null");
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(checkId, "checkId must not be null");
    Objects.requireNonNull(attemptResult, "attemptResult must not be null");
  }
}
