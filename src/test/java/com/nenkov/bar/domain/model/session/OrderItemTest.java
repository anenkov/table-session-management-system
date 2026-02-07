package com.nenkov.bar.domain.model.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderItemTest {

  @Test
  void constructor_nullId_throwsNpe() {
    Throwable thrown =
        assertThrows(
            NullPointerException.class,
            () -> new OrderItem(null, "P-1", 1, OrderItemStatus.ACCEPTED));

    assertThat(thrown.getMessage()).contains("id must not be null");
  }

  @Test
  void constructor_nullProductId_throwsNpe() {
    OrderItemId id = OrderItemId.of(UUID.randomUUID());

    Throwable thrown =
        assertThrows(
            NullPointerException.class, () -> new OrderItem(id, null, 1, OrderItemStatus.ACCEPTED));

    assertThat(thrown.getMessage()).contains("productId must not be null");
  }

  @Test
  void constructor_blankProductId_throwsIllegalArgumentException() {
    OrderItemId id = OrderItemId.of(UUID.randomUUID());
    String productId = "   ";

    Throwable thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new OrderItem(id, productId, 1, OrderItemStatus.ACCEPTED));

    assertThat(thrown.getMessage()).contains("productId must not be blank");
  }

  @Test
  void constructor_nonPositiveQuantity_throwsIllegalArgumentException() {
    OrderItemId id = OrderItemId.of(UUID.randomUUID());

    Throwable thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new OrderItem(id, "P-1", 0, OrderItemStatus.ACCEPTED));

    assertThat(thrown.getMessage()).contains("quantity must be positive");
  }

  @Test
  void constructor_nullStatus_throwsNpe() {
    OrderItemId id = OrderItemId.of(UUID.randomUUID());

    Throwable thrown =
        assertThrows(NullPointerException.class, () -> new OrderItem(id, "P-1", 1, null));

    assertThat(thrown.getMessage()).contains("status must not be null");
  }
}
