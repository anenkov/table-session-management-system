package com.nenkov.bar.web.api.ordering;

import java.util.List;

/**
 * Response returned after adding order items.
 *
 * <p>Returns the ids of created order items.
 */
public record AddOrderItemsResponse(String sessionId, List<String> createdItemIds) {}
