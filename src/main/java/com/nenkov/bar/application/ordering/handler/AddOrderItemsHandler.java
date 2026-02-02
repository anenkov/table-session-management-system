package com.nenkov.bar.application.ordering.handler;

import com.nenkov.bar.application.ordering.model.AddOrderItemsInput;
import com.nenkov.bar.application.ordering.model.AddOrderItemsResult;
import java.util.List;
import java.util.Objects;

/**
 * Workflow handler: add items to an existing table session.
 *
 * <p>Orchestrates domain + repositories once the ordering aggregate model is introduced. For Phase
 * 3.2.2 we keep this as a compile-safe skeleton.
 */
public final class AddOrderItemsHandler {

  public AddOrderItemsHandler() {}

  public AddOrderItemsResult handle(AddOrderItemsInput input) {
    Objects.requireNonNull(input, "input must not be null");

    // Domain ordering model + persistence is not wired yet in 3.2.2.
    // This will be implemented once the session aggregate contains order items and can be saved.
    throw new UnsupportedOperationException(
        "AddOrderItemsHandler not implemented yet (future 3.2.x/3.3)");
  }

  static AddOrderItemsResult placeholderResult(AddOrderItemsInput input) {
    // Keep the method for temporary compile/testing convenience if needed later.
    return new AddOrderItemsResult(input.sessionId(), List.of());
  }
}
