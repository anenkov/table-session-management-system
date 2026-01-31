package com.nenkov.bar.domain.model.payment;

import com.nenkov.bar.domain.model.money.Money;
import java.util.List;
import java.util.Objects;

/**
 * Result of quoting a payment check from session state and payer selection.
 *
 * <p>This is a pure calculation output: total check amount and the per-item allocation snapshot
 * ({@link PaidItem}) that will be embedded in a {@link Check}.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>{@code checkAmount} is non-null and strictly greater than zero
 *   <li>{@code paidItems} is non-null and non-empty
 *   <li>All paid items share the same currency as {@code checkAmount}
 * </ul>
 */
public record CheckQuote(Money checkAmount, List<PaidItem> paidItems) {

  public CheckQuote {
    Objects.requireNonNull(checkAmount, "checkAmount must not be null");
    Objects.requireNonNull(paidItems, "paidItems must not be null");
    paidItems = List.copyOf(paidItems);

    if (checkAmount.isZero()) {
      throw new IllegalArgumentException("checkAmount must be strictly greater than zero");
    }
    if (paidItems.isEmpty()) {
      throw new IllegalArgumentException("paidItems must not be empty");
    }

    String currency = checkAmount.currency();
    for (PaidItem item : paidItems) {
      if (!currency.equals(item.paidAmount().currency())) {
        throw new IllegalArgumentException("Currency mismatch between checkAmount and paid items");
      }
    }
  }

  /**
   * Creates a {@code CheckQuote}.
   *
   * @param checkAmount strictly positive total amount
   * @param paidItems non-empty allocation snapshot
   * @return a new quote
   */
  public static CheckQuote of(Money checkAmount, List<PaidItem> paidItems) {
    return new CheckQuote(checkAmount, paidItems);
  }
}
