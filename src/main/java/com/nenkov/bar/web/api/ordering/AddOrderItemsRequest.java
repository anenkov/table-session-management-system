package com.nenkov.bar.web.api.ordering;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request to add order items to an existing session.
 *
 * <p>Duplicate productIds are allowed and represent separate ordered items.
 */
public record AddOrderItemsRequest(@NotEmpty @Valid List<AddOrderItemLine> items) {

  public record AddOrderItemLine(@NotBlank String productId, @Min(1) int quantity) {}
}
