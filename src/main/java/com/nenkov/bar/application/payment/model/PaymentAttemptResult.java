package com.nenkov.bar.application.payment.model;

import java.util.Optional;

/**
 * Outcome of a payment initiation attempt as observed by the application.
 *
 * <p>This type captures business-level outcomes without exposing provider APIs.
 */
public final class PaymentAttemptResult {

  private final PaymentAttemptStatus status;
  private final String providerReference; // opaque; for support/auditing
  private final String failureReason; // optional, human-readable

  private PaymentAttemptResult(
      PaymentAttemptStatus status, String providerReference, String failureReason) {
    this.status = status;
    this.providerReference = providerReference;
    this.failureReason = failureReason;
  }

  public static PaymentAttemptResult approved(String providerReference) {
    return new PaymentAttemptResult(PaymentAttemptStatus.APPROVED, providerReference, null);
  }

  public static PaymentAttemptResult declined(String providerReference, String failureReason) {
    return new PaymentAttemptResult(
        PaymentAttemptStatus.DECLINED, providerReference, failureReason);
  }

  public static PaymentAttemptResult pending(String providerReference) {
    return new PaymentAttemptResult(PaymentAttemptStatus.PENDING, providerReference, null);
  }

  public PaymentAttemptStatus status() {
    return status;
  }

  public String providerReference() {
    return providerReference;
  }

  public Optional<String> failureReason() {
    return Optional.ofNullable(failureReason);
  }
}
