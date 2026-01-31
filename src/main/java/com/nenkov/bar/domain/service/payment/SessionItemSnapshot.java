package com.nenkov.bar.domain.service.payment;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.Objects;

/**
 * Immutable snapshot of an order item's payable state at the time a check is quoted.
 *
 * <p>This type exists to decouple payment calculation from the full session aggregate. It
 * represents only what the calculator needs: identity, unit price, and remaining quantity.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>{@code itemId} is non-null
 *   <li>{@code unitPrice} is non-null and strictly greater than zero
 *   <li>{@code remainingQuantity} is zero or positive
 * </ul>
 */
public record SessionItemSnapshot(OrderItemId itemId, Money unitPrice, int remainingQuantity) {

  public SessionItemSnapshot {
    Objects.requireNonNull(itemId, "itemId must not be null");
    Objects.requireNonNull(unitPrice, "unitPrice must not be null");

    if (unitPrice.isZero()) {
      throw new IllegalArgumentException("unitPrice must be strictly greater than zero");
    }
    if (remainingQuantity < 0) {
      throw new IllegalArgumentException("remainingQuantity must be >= 0");
    }
  }

  /**
   * Returns the gross payable amount for the given quantity at this unit price.
   *
   * @param quantity quantity of units
   * @return {@code unitPrice × quantity}
   */
  public Money grossAmountFor(int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("quantity must be >= 0");
    }
    return unitPrice.times(quantity);
  }
}
