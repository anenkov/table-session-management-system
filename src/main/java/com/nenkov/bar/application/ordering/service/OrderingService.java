package com.nenkov.bar.application.ordering.service;

import com.nenkov.bar.application.ordering.model.AddOrderItemsInput;
import com.nenkov.bar.application.ordering.model.AddOrderItemsResult;

/**
 * Feature façade for ordering-related workflows.
 *
 * <p>This is the single public entry point for the ordering feature. It should remain thin and
 * delegate workflow logic to handlers.
 */
public interface OrderingService {

  AddOrderItemsResult addItems(AddOrderItemsInput input);
}
