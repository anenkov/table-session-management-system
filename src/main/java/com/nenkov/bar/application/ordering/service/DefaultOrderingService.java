package com.nenkov.bar.application.ordering.service;

import com.nenkov.bar.application.ordering.handler.AddOrderItemsHandler;
import com.nenkov.bar.application.ordering.model.AddOrderItemsInput;
import com.nenkov.bar.application.ordering.model.AddOrderItemsResult;
import java.util.Objects;

/**
 * Default implementation of {@link OrderingService}.
 *
 * <p>Thin feature façade: delegates workflows to handlers.
 */
public final class DefaultOrderingService implements OrderingService {

  private final AddOrderItemsHandler addOrderItemsHandler;

  public DefaultOrderingService(AddOrderItemsHandler addOrderItemsHandler) {
    this.addOrderItemsHandler =
        Objects.requireNonNull(addOrderItemsHandler, "addOrderItemsHandler must not be null");
  }

  @Override
  public AddOrderItemsResult addItems(AddOrderItemsInput input) {
    return addOrderItemsHandler.handle(input);
  }
}
