package com.nenkov.bar.application.payment.model;

import java.util.Objects;

/**
 * Application-owned idempotency key for a payment initiation request.
 *
 * <p>Value is opaque to the domain and the payment provider.
 */
public final class PaymentRequestId {

  private final String value;

  private PaymentRequestId(String value) {
    this.value = value;
  }

  public static PaymentRequestId of(String value) {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("value must not be blank");
    }
    return new PaymentRequestId(value);
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PaymentRequestId other)) {
      return false;
    }
    return value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value;
  }
}
