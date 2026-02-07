package com.nenkov.bar.domain.model.session;

import java.util.Objects;

/**
 * Immutable order item belonging to a {@link TableSession}.
 *
 * <p>An OrderItem is created exclusively by the TableSession aggregate.
 */
public final class OrderItem {

  private final OrderItemId id;
  private final String productId;
  private final int quantity;
  private final OrderItemStatus status;

  public OrderItem(OrderItemId id, String productId, int quantity, OrderItemStatus status) {
    this.id = Objects.requireNonNull(id, "id must not be null");
    this.productId = Objects.requireNonNull(productId, "productId must not be null");
    this.status = Objects.requireNonNull(status, "status must not be null");

    if (productId.isBlank()) {
      throw new IllegalArgumentException("productId must not be blank");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }

    this.quantity = quantity;
  }

  public OrderItemId id() {
    return id;
  }

  public String productId() {
    return productId;
  }

  public int quantity() {
    return quantity;
  }

  public OrderItemStatus status() {
    return status;
  }
}
