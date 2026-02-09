package com.nenkov.bar.web.api.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request to create a check for a session by selecting payable items.
 *
 * <p>Duplicate {@code itemId}s are allowed; the application/domain layer consolidates selections.
 */
public record CreateCheckRequest(@NotEmpty @Valid List<SelectionLine> selections) {

  public record SelectionLine(@NotBlank String itemId, @Min(1) int quantity) {}
}
