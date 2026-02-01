package com.nenkov.bar.application.payment.gateway;

import com.nenkov.bar.application.payment.exception.PaymentGatewayException;
import com.nenkov.bar.application.payment.model.PaymentAttemptResult;
import com.nenkov.bar.application.payment.model.PaymentRequestId;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.CheckId;
import com.nenkov.bar.domain.model.session.TableSessionId;

/**
 * External-system boundary for taking card payments.
 *
 * <p>This gateway abstracts the integration with a payment provider/portal. The application
 * controls the interaction and remains independent of HTTP/SDK concerns.
 *
 * <p>Contract principles:
 *
 * <ul>
 *   <li>Inputs/outputs use domain-friendly types only.
 *   <li>Business outcomes (approved/declined) are modeled as results.
 *   <li>Technical failures are surfaced as unchecked exceptions.
 * </ul>
 */
public interface PaymentGateway {

  /**
   * Initiates a payment attempt for a given session and check.
   *
   * <p>Idempotency: callers must provide a stable {@link PaymentRequestId} for retries. Repeating
   * the same request id should not produce duplicate charges.
   *
   * @param requestId idempotency key owned by the application (non-null)
   * @param sessionId owning session (non-null)
   * @param checkId check being paid (non-null)
   * @param amount amount to charge (non-null)
   * @return result describing provider-facing acceptance and current outcome
   * @throws PaymentGatewayException on technical/integration failures
   */
  PaymentAttemptResult initiatePayment(
      PaymentRequestId requestId, TableSessionId sessionId, CheckId checkId, Money amount);
}
