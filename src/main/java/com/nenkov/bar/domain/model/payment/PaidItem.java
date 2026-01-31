package com.nenkov.bar.domain.model.payment;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.Objects;

/**
 * Immutable snapshot describing what an individual {@link Check} paid for.
 *
 * <p>This is a value object used for allocation/receipt purposes. A {@code PaidItem} records both
 * the original unit price and the allocated paid amount after applying item write-offs and
 * proportional session-level write-offs.
 *
 * <p>Note: the domain still pays in whole units (integer quantity). {@code paidAmount} may differ
 * from {@code unitPriceAtPayment × quantity} due to discount allocation.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>{@code itemId} is non-null
 *   <li>{@code quantity} is strictly positive
 *   <li>{@code unitPriceAtPayment} is non-null
 *   <li>{@code paidAmount} is non-null and strictly greater than zero
 *   <li>{@code unitPriceAtPayment.currency == paidAmount.currency}
 *   <li>{@code paidAmount <= unitPriceAtPayment × quantity}
 * </ul>
 */
public record PaidItem(
    OrderItemId itemId, int quantity, Money unitPriceAtPayment, Money paidAmount) {

  public PaidItem {
    Objects.requireNonNull(itemId, "itemId must not be null");
    Objects.requireNonNull(unitPriceAtPayment, "unitPriceAtPayment must not be null");
    Objects.requireNonNull(paidAmount, "paidAmount must not be null");

    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be > 0");
    }
    if (paidAmount.isZero()) {
      throw new IllegalArgumentException("paidAmount must be strictly greater than zero");
    }
    if (!unitPriceAtPayment.currency().equals(paidAmount.currency())) {
      throw new IllegalArgumentException(
          "Currency mismatch between unitPriceAtPayment and paidAmount");
    }

    Money maxPayable = unitPriceAtPayment.times(quantity);
    if (paidAmount.compareTo(maxPayable) > 0) {
      throw new IllegalArgumentException(
          "paidAmount must not exceed unitPriceAtPayment × quantity");
    }
  }

  /**
   * Creates a {@code PaidItem}.
   *
   * @param itemId non-null order item id
   * @param quantity strictly positive quantity (whole units)
   * @param unitPriceAtPayment unit price at the time of payment
   * @param paidAmount strictly positive allocated amount for this item in this check
   * @return a new {@code PaidItem}
   */
  public static PaidItem of(
      OrderItemId itemId, int quantity, Money unitPriceAtPayment, Money paidAmount) {
    return new PaidItem(itemId, quantity, unitPriceAtPayment, paidAmount);
  }
}
