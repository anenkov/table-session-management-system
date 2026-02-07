package com.nenkov.bar.domain.model.session;

import java.util.Objects;

/**
 * Draft input representing the intent to add an order item to a table session.
 *
 * <p>This is not an OrderItem yet. It becomes one only when accepted by the TableSession aggregate,
 * which assigns identity and initial status.
 */
public record OrderItemDraft(String productId, int quantity) {

  public OrderItemDraft {
    Objects.requireNonNull(productId, "productId must not be null");

    if (productId.isBlank()) {
      throw new IllegalArgumentException("productId must not be blank");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
  }
}
