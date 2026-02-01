package com.nenkov.bar.domain.model.payment;

import static com.nenkov.bar.testsupport.TestFixtures.itemId;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nenkov.bar.domain.model.session.OrderItemId;
import org.junit.jupiter.api.Test;

final class PaymentSelectionTest {

  private static final OrderItemId A = itemId("00000000-0000-0000-0000-000000000001");

  @Test
  void of_quantityZero_rejected() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> PaymentSelection.of(A, 0));

    assertTrue(ex.getMessage().toLowerCase().contains("quantity"));
  }

  @Test
  void of_quantityNegative_rejected() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> PaymentSelection.of(A, -1));

    assertTrue(ex.getMessage().toLowerCase().contains("quantity"));
  }
}
