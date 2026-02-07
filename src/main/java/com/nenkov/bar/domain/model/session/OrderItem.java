package com.nenkov.bar.domain.model.session;

import java.util.Objects;

/**
 * Immutable order item belonging to a {@link TableSession}.
 *
 * <p>An OrderItem is created exclusively by the TableSession aggregate.
 */
public record OrderItem(OrderItemId id, String productId, int quantity, OrderItemStatus status) {

  public OrderItem {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(productId, "productId must not be null");
    Objects.requireNonNull(status, "status must not be null");

    if (productId.isBlank()) {
      throw new IllegalArgumentException("productId must not be blank");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
  }
}
