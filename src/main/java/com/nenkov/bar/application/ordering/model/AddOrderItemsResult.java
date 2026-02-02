package com.nenkov.bar.application.ordering.model;

import java.util.List;
import java.util.Objects;

/**
 * Result model for adding order items.
 *
 * <p>For now returns only identifiers created/accepted by the system. Domain ordering identifiers
 * will be wired later once the domain model is finalized.
 */
public record AddOrderItemsResult(String sessionId, List<String> createdItemIds) {

  public AddOrderItemsResult {
    Objects.requireNonNull(sessionId, "sessionId must not be null");
    Objects.requireNonNull(createdItemIds, "createdItemIds must not be null");
    if (sessionId.isBlank()) {
      throw new IllegalArgumentException("sessionId must not be blank");
    }
  }
}
