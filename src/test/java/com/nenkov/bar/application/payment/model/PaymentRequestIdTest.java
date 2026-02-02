package com.nenkov.bar.application.payment.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class PaymentRequestIdTest {

  @Test
  void happyPath() {
    PaymentRequestId id = PaymentRequestId.of("req-123");

    assertThat(id.value()).isEqualTo("req-123");
    assertThat(id).hasToString("req-123");
  }

  @Test
  void nullValue_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> PaymentRequestId.of(null));

    assertThat(thrown.getMessage()).contains("value");
  }

  @Test
  void blankValue_throwsIae() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> PaymentRequestId.of("   "));

    assertThat(thrown.getMessage()).contains("value must not be blank");
  }

  @Test
  void equalityAndHashCode() {
    PaymentRequestId a = PaymentRequestId.of("req-1");
    PaymentRequestId b = PaymentRequestId.of("req-1");

    assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
  }

  @Test
  void equals_returnsFalseForDifferentType() {
    PaymentRequestId id = PaymentRequestId.of("req-1");

    Object other = "req-1";

    assertThat(id.equals(other)).isFalse();
  }
}
