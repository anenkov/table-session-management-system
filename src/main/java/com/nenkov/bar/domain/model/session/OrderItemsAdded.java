package com.nenkov.bar.domain.model.session;

import java.util.List;
import java.util.Objects;

/** Domain result returned after successfully adding order items to a table session. */
public record OrderItemsAdded(TableSession session, List<OrderItemId> createdOrderItemIds) {
  public OrderItemsAdded {
    Objects.requireNonNull(session, "session must not be null");
    Objects.requireNonNull(createdOrderItemIds, "createdOrderItemIds must not be null");
  }
}
