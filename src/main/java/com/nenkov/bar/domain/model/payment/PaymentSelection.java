package com.nenkov.bar.domain.model.payment;

import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.Objects;

/**
 * Represents a payer's intent to pay a specific quantity of a given order item.
 *
 * <p>This is an input value object used when quoting/creating a {@link Check}. Quantities are whole
 * units only.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>{@code itemId} is non-null
 *   <li>{@code quantity} is strictly positive
 * </ul>
 */
public record PaymentSelection(OrderItemId itemId, int quantity) {

  public PaymentSelection {
    Objects.requireNonNull(itemId, "itemId must not be null");
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be > 0");
    }
  }

  /**
   * Creates a {@code PaymentSelection}.
   *
   * @param itemId non-null item id
   * @param quantity strictly positive quantity
   * @return a new selection
   */
  public static PaymentSelection of(OrderItemId itemId, int quantity) {
    return new PaymentSelection(itemId, quantity);
  }
}
