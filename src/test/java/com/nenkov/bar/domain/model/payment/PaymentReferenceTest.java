package com.nenkov.bar.domain.model.payment;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PaymentReferenceTest {

  @Test
  void trims_value() {
    PaymentReference ref = PaymentReference.of("  abc  ");
    assertEquals("abc", ref.value());
  }

  @Test
  void blank_rejected() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> PaymentReference.of("   "));
    assertTrue(ex.getMessage().toLowerCase().contains("blank"));
  }

  @Test
  void null_rejected() {
    NullPointerException ex =
        assertThrows(NullPointerException.class, () -> PaymentReference.of(null));
    assertTrue(ex.getMessage().contains("value"));
  }

  @Test
  void tooLong_rejected() {
    String tooLong = "a".repeat(101);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> PaymentReference.of(tooLong));
    assertTrue(ex.getMessage().contains("at most"));
  }
}
