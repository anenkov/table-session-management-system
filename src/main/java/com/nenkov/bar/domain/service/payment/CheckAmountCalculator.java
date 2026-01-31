package com.nenkov.bar.domain.service.payment;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.CheckQuote;
import com.nenkov.bar.domain.model.payment.PaymentSelection;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOff;
import java.util.List;

/**
 * Stateless domain service responsible for quoting a {@code Check} amount and allocation.
 *
 * <p>Rules:
 *
 * <ol>
 *   <li>Selections must not exceed remaining item quantities.
 *   <li>Apply item-scoped write-offs first (per item/quantity).
 *   <li>Apply session-level write-offs proportionally across remaining payable amounts.
 *   <li>Distribute a rounding remainder by the largest fractional remainder first (locked policy).
 * </ol>
 */
public interface CheckAmountCalculator {

  /**
   * Computes a quote for paying the selected items, given the current session payable state.
   *
   * @param currency session currency (used to construct intermediate zero values)
   * @param sessionItems snapshot of payable items (unit price plus remaining quantity)
   * @param selections payer selection (item + quantity)
   * @param itemWriteOffs item-scoped write-offs (applied first)
   * @param sessionWriteOffs session-level write-offs (applied proportionally after item write-offs)
   * @return a {@link CheckQuote} containing total and per-item paid allocation
   */
  CheckQuote quote(
      String currency,
      List<SessionItemSnapshot> sessionItems,
      List<PaymentSelection> selections,
      List<ItemWriteOff> itemWriteOffs,
      List<WriteOff> sessionWriteOffs);

  /**
   * Convenience method for computing the gross amount of a selection before any write-offs.
   *
   * <p>This is provided for testing and diagnostics.
   */
  default Money grossSelectedAmount(
      String currency, List<SessionItemSnapshot> sessionItems, List<PaymentSelection> selections) {
    Money total = Money.zero(currency);
    for (PaymentSelection sel : selections) {
      SessionItemSnapshot item =
          sessionItems.stream()
              .filter(i -> i.itemId().equals(sel.itemId()))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalArgumentException("Selected item not found in session items"));

      total = total.plus(item.grossAmountFor(sel.quantity()));
    }
    return total;
  }
}
