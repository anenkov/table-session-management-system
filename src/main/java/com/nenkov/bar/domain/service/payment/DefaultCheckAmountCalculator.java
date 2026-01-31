package com.nenkov.bar.domain.service.payment;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.CheckQuote;
import com.nenkov.bar.domain.model.payment.PaidItem;
import com.nenkov.bar.domain.model.payment.PaymentSelection;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOff;
import com.nenkov.bar.domain.service.payment.allocation.ProportionalAllocator;
import com.nenkov.bar.domain.service.payment.allocation.ProportionalShareCalculator;
import com.nenkov.bar.domain.service.payment.writeoff.ItemWriteOffAllocation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of {@link CheckAmountCalculator}.
 *
 * <p>Pure domain logic. Deterministic. No framework dependencies.
 *
 * <p>This class is an orchestrator: all allocation math is delegated to dedicated domain services.
 *
 * <p>Locked rules:
 *
 * <ol>
 *   <li>Selections must not exceed remaining quantities.
 *   <li>Apply item-scoped write-offs first.
 *   <li>Apply session-level write-offs proportionally on the net amounts.
 *   <li>Distribute a rounding remainder using policy A: largest fractional remainder first.
 * </ol>
 */
public final class DefaultCheckAmountCalculator implements CheckAmountCalculator {

  private final ProportionalAllocator proportionalAllocator;
  private final ProportionalShareCalculator shareCalculator;

  /** Creates a calculator using the locked remainder distribution policy (A). */
  public DefaultCheckAmountCalculator() {
    this(ProportionalAllocator.defaultAllocator(), new ProportionalShareCalculator());
  }

  /** Visible for testing / alternative policies. */
  public DefaultCheckAmountCalculator(ProportionalAllocator proportionalAllocator) {
    this(proportionalAllocator, new ProportionalShareCalculator());
  }

  /** Visible for testing / alternative policies. */
  public DefaultCheckAmountCalculator(
      ProportionalAllocator proportionalAllocator, ProportionalShareCalculator shareCalculator) {
    this.proportionalAllocator =
        Objects.requireNonNull(proportionalAllocator, "proportionalAllocator must not be null");
    this.shareCalculator =
        Objects.requireNonNull(shareCalculator, "shareCalculator must not be null");
  }

  @Override
  public CheckQuote quote(
      String currency,
      List<SessionItemSnapshot> sessionItems,
      List<PaymentSelection> selections,
      List<ItemWriteOff> itemWriteOffs,
      List<WriteOff> sessionWriteOffs) {

    PaymentCalculationContext ctx =
        PaymentCalculationContext.create(currency, sessionItems, selections);

    Money totalSessionWriteOff = sumSessionWriteOffs(ctx.currency(), sessionWriteOffs);

    Map<OrderItemId, Money> grossRemainingByItem = computeGrossRemaining(ctx);
    Map<OrderItemId, Money> grossSelectedByItem = computeGrossSelected(ctx);

    ItemWriteOffAllocation itemWriteOffAllocation =
        ItemWriteOffAllocation.from(ctx.currency(), itemWriteOffs);

    Map<OrderItemId, Money> itemWriteOffOnSelected =
        itemWriteOffAllocation.allocateToSelected(ctx, grossSelectedByItem);

    Map<OrderItemId, Money> netSelectedByItem =
        subtractPerItem(grossSelectedByItem, itemWriteOffOnSelected);

    Money totalNetSelected = sumMoney(ctx.currency(), netSelectedByItem.values());
    requirePositive(
        totalNetSelected, "Total net selected amount is zero; cannot create a check quote");

    Map<OrderItemId, Money> itemWriteOffOnRemaining =
        itemWriteOffAllocation.allocateToRemaining(ctx, grossRemainingByItem);

    Map<OrderItemId, Money> netRemainingByItem =
        subtractPerItem(grossRemainingByItem, itemWriteOffOnRemaining);

    Money totalNetRemaining = sumMoney(ctx.currency(), netRemainingByItem.values());
    requirePositive(
        totalNetRemaining, "Total net remaining amount is zero; cannot apply session write-offs");

    Money checkShareOfSessionWriteOff =
        shareCalculator.shareOfTotal(
            ctx.currency(), totalSessionWriteOff, totalNetSelected, totalNetRemaining);

    Map<OrderItemId, Money> sessionWriteOffAllocatedToSelected =
        proportionalAllocator.allocate(
            ctx.currency(), checkShareOfSessionWriteOff, netSelectedByItem);

    Map<OrderItemId, Money> paidAmountByItem =
        subtractPerItem(netSelectedByItem, sessionWriteOffAllocatedToSelected);

    return toCheckQuote(ctx, paidAmountByItem);
  }

  private static CheckQuote toCheckQuote(
      PaymentCalculationContext ctx, Map<OrderItemId, Money> paidAmountByItem) {
    List<PaidItem> paidItems = new ArrayList<>();

    for (Map.Entry<OrderItemId, Integer> entry : ctx.selectedQtyByItem().entrySet()) {
      OrderItemId itemId = entry.getKey();
      int qty = entry.getValue();

      SessionItemSnapshot snapshot = ctx.itemById().get(itemId);
      if (snapshot == null) {
        throw new IllegalArgumentException("Selected item not found: " + itemId.value());
      }

      Money paidAmount = requirePresentAndPositive(paidAmountByItem.get(itemId), itemId);

      paidItems.add(PaidItem.of(itemId, qty, snapshot.unitPrice(), paidAmount));
    }

    Money checkAmount = sumMoney(ctx.currency(), paidAmountByItem.values());
    requirePositive(checkAmount, "checkAmount is zero; cannot create a check quote");

    return CheckQuote.of(checkAmount, paidItems);
  }

  private static Money requirePresentAndPositive(Money value, OrderItemId itemId) {
    if (value == null) {
      throw new IllegalArgumentException("Missing paid amount for item: " + itemId.value());
    }
    if (value.isZero()) {
      throw new IllegalArgumentException(
          "Selected item results in zero payable amount: " + itemId.value());
    }
    return value;
  }

  private static void requirePositive(Money value, String message) {
    if (value.isZero()) {
      throw new IllegalArgumentException(message);
    }
  }

  private static Money sumSessionWriteOffs(String currency, List<WriteOff> writeOffs) {
    Money total = Money.zero(currency);
    for (WriteOff wo : writeOffs) {
      if (!currency.equals(wo.amount().currency())) {
        throw new IllegalArgumentException("Currency mismatch in session write-offs");
      }
      total = total.plus(wo.amount());
    }
    return total;
  }

  private static Money sumMoney(String currency, Iterable<Money> monies) {
    Money total = Money.zero(currency);
    for (Money m : monies) {
      if (!currency.equals(m.currency())) {
        throw new IllegalArgumentException("Currency mismatch while summing monies");
      }
      total = total.plus(m);
    }
    return total;
  }

  private static Map<OrderItemId, Money> computeGrossRemaining(PaymentCalculationContext ctx) {
    Map<OrderItemId, Money> result = new LinkedHashMap<>();
    for (SessionItemSnapshot item : ctx.itemById().values()) {
      result.put(item.itemId(), item.unitPrice().times(item.remainingQuantity()));
    }
    return result;
  }

  private static Map<OrderItemId, Money> computeGrossSelected(PaymentCalculationContext ctx) {
    Map<OrderItemId, Money> result = new LinkedHashMap<>();
    for (Map.Entry<OrderItemId, Integer> entry : ctx.selectedQtyByItem().entrySet()) {
      OrderItemId itemId = entry.getKey();
      SessionItemSnapshot item = ctx.itemById().get(itemId);
      if (item == null) {
        throw new IllegalArgumentException("Selected item not found: " + itemId.value());
      }
      result.put(itemId, item.unitPrice().times(entry.getValue()));
    }
    return result;
  }

  private static Map<OrderItemId, Money> subtractPerItem(
      Map<OrderItemId, Money> left, Map<OrderItemId, Money> right) {
    Map<OrderItemId, Money> result = new LinkedHashMap<>();
    for (Map.Entry<OrderItemId, Money> entry : left.entrySet()) {
      Money r = right.get(entry.getKey());
      if (r == null) {
        r = Money.zero(entry.getValue().currency());
      }
      result.put(entry.getKey(), entry.getValue().minus(r));
    }
    return result;
  }
}
