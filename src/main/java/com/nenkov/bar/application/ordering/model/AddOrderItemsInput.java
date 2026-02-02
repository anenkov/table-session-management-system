package com.nenkov.bar.application.ordering.model;

import java.util.List;
import java.util.Objects;

/**
 * Input model for adding order items to an existing table session.
 *
 * <p>Application-level model. Does not expose domain entities.
 */
public record AddOrderItemsInput(String sessionId, List<RequestedItem> items) {

  public AddOrderItemsInput {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(items, "items must not be null");
    if (sessionId.isBlank()) {
      throw new IllegalArgumentException("sessionId must not be blank");
    }
    if (items.isEmpty()) {
      throw new IllegalArgumentException("items must not be empty");
    }
  }

  /**
   * Requested item to be added.
   *
   * <p>Note: product/menu modeling is not finalized yet; this is a stable application DTO.
   */
  public record RequestedItem(String productId, int quantity) {

    public RequestedItem {
      Objects.requireNonNull(productId, "productId must not be null");
      if (productId.isBlank()) {
        throw new IllegalArgumentException("productId must not be blank");
      }
      if (quantity <= 0) {
        throw new IllegalArgumentException("quantity must be > 0");
      }
    }
  }
}
