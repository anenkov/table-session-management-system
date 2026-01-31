package com.nenkov.bar.domain.service.payment.writeoff;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.money.MoneyPolicy;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.service.payment.PaymentCalculationContext;
import com.nenkov.bar.domain.service.payment.SessionItemSnapshot;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Allocates item-scoped write-offs to a quantity scope (selected quantities or remaining
 * quantities).
 *
 * <p>Item write-offs are quantity-based. This allocator:
 *
 * <ol>
 *   <li>Aggregates all {@link ItemWriteOff}s per {@link OrderItemId}
 *   <li>Computes a per-unit write-off amount (total / totalQuantity) using {@link
 *       MoneyPolicy#WORK_CONTEXT}
 *   <li>Applies per-unit amount to the requested scope quantity
 *   <li>Caps the result by the provided gross payable cap per item
 * </ol>
 *
 * <p>Determinism: this allocator preserves insertion order in its result maps so downstream
 * processing remains stable across runs.
 */
public final class ItemWriteOffAllocation {

  private final String currency;
  private final Map<OrderItemId, AggregatedItemWriteOff> aggregatedByItem;

  private ItemWriteOffAllocation(
      String currency, Map<OrderItemId, AggregatedItemWriteOff> aggregatedByItem) {
    this.currency = currency;
    this.aggregatedByItem = aggregatedByItem;
  }

  /**
   * Aggregates item write-offs by item id.
   *
   * <p>All write-offs must be in the provided {@code currency}. Aggregation sums amounts and
   * quantities.
   */
  public static ItemWriteOffAllocation from(String currency, List<ItemWriteOff> itemWriteOffs) {
    Objects.requireNonNull(currency, "currency must not be null");
    Objects.requireNonNull(itemWriteOffs, "itemWriteOffs must not be null");

    Map<OrderItemId, AggregatedItemWriteOff> aggregated = new LinkedHashMap<>();
    for (ItemWriteOff wo : itemWriteOffs) {
      if (!currency.equals(wo.amount().currency())) {
        throw new IllegalArgumentException("Currency mismatch in item write-offs");
      }
      aggregated.merge(
          wo.itemId(),
          new AggregatedItemWriteOff(wo.amount(), wo.quantity()),
          AggregatedItemWriteOff::merge);
    }
    return new ItemWriteOffAllocation(currency, aggregated);
  }

  /**
   * Allocates item write-offs to the payer-selected quantities.
   *
   * @param ctx calculation context containing consolidated selected quantities
   * @param grossSelectedByItem gross payable caps for the selected quantities per item
   * @return per-item allocated write-off amounts for the selected quantities
   */
  public Map<OrderItemId, Money> allocateToSelected(
      PaymentCalculationContext ctx, Map<OrderItemId, Money> grossSelectedByItem) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Objects.requireNonNull(grossSelectedByItem, "grossSelectedByItem must not be null");

    return allocateByQuantity(ctx.selectedQtyByItem(), grossSelectedByItem);
  }

  /**
   * Allocates item write-offs to the remaining quantities (not yet paid).
   *
   * @param ctx calculation context containing current session items
   * @param grossRemainingByItem gross payable caps for remaining quantities per item
   * @return per-item allocated write-off amounts for the remaining quantities
   */
  public Map<OrderItemId, Money> allocateToRemaining(
      PaymentCalculationContext ctx, Map<OrderItemId, Money> grossRemainingByItem) {
    Objects.requireNonNull(ctx, "ctx must not be null");
    Objects.requireNonNull(grossRemainingByItem, "grossRemainingByItem must not be null");

    Map<OrderItemId, Integer> qtyByItem = new LinkedHashMap<>();
    for (Map.Entry<OrderItemId, SessionItemSnapshot> entry : ctx.itemById().entrySet()) {
      qtyByItem.put(entry.getKey(), entry.getValue().remainingQuantity());
    }

    return allocateByQuantity(qtyByItem, grossRemainingByItem);
  }

  /**
   * Allocates aggregated item write-offs to a quantity scope.
   *
   * <p>For each item id in {@code qtyByItem}:
   *
   * <ul>
   *   <li>If scope quantity is &lt;= 0, allocation is zero
   *   <li>If no write-off exists for the item, allocation is zero
   *   <li>Otherwise allocate up to {@code min(scopeQty, aggregatedQty)}
   *   <li>Cap allocation by {@code grossCapByItem[itemId]}
   * </ul>
   *
   * @throws IllegalArgumentException if {@code grossCapByItem} is missing an entry for an item id
   */
  private Map<OrderItemId, Money> allocateByQuantity(
      Map<OrderItemId, Integer> qtyByItem, Map<OrderItemId, Money> grossCapByItem) {
    Map<OrderItemId, Money> result = new LinkedHashMap<>();

    for (Map.Entry<OrderItemId, Integer> entry : qtyByItem.entrySet()) {
      OrderItemId itemId = entry.getKey();
      int scopeQty = entry.getValue();

      Money cap = requireCap(itemId, grossCapByItem);
      Money allocation = allocationFor(itemId, scopeQty, cap);

      result.put(itemId, allocation);
    }

    return result;
  }

  private static Money requireCap(OrderItemId itemId, Map<OrderItemId, Money> grossCapByItem) {
    Money cap = grossCapByItem.get(itemId);
    if (cap == null) {
      throw new IllegalArgumentException("Missing gross cap amount for item: " + itemId.value());
    }
    return cap;
  }

  private Money allocationFor(OrderItemId itemId, int scopeQty, Money cap) {
    if (scopeQty <= 0) {
      return Money.zero(currency);
    }

    AggregatedItemWriteOff agg = aggregatedByItem.get(itemId);
    if (agg == null) {
      return Money.zero(currency);
    }

    int allocQty = Math.min(scopeQty, agg.totalQuantity());
    if (allocQty == 0) {
      return Money.zero(currency);
    }

    Money computed = computeAllocation(agg, allocQty);
    return computed.compareTo(cap) > 0 ? cap : computed;
  }

  /**
   * Computes the write-off amount for {@code allocQty} units based on an aggregated per-unit
   * amount.
   *
   * <p>Per-unit is computed with {@link MoneyPolicy#WORK_CONTEXT} precision; final rounding is
   * applied via {@link Money#of(String, java.math.BigDecimal)}.
   */
  private Money computeAllocation(AggregatedItemWriteOff agg, int allocQty) {
    Money perUnit = agg.perUnitAmount(currency);
    BigDecimal raw = perUnit.amount().multiply(BigDecimal.valueOf(allocQty));
    return Money.of(currency, raw);
  }

  private record AggregatedItemWriteOff(Money totalAmount, int totalQuantity) {

    AggregatedItemWriteOff {
      Objects.requireNonNull(totalAmount, "totalAmount must not be null");
      if (totalQuantity <= 0) {
        throw new IllegalArgumentException("totalQuantity must be > 0");
      }
    }

    static AggregatedItemWriteOff merge(AggregatedItemWriteOff a, AggregatedItemWriteOff b) {
      if (!a.totalAmount.currency().equals(b.totalAmount.currency())) {
        throw new IllegalArgumentException("Currency mismatch in aggregated item write-offs");
      }
      return new AggregatedItemWriteOff(
          a.totalAmount.plus(b.totalAmount), a.totalQuantity + b.totalQuantity);
    }

    /**
     * Computes a per-unit write-off amount for the aggregated write-off.
     *
     * <p>Intermediate division uses {@link MoneyPolicy#WORK_CONTEXT}. Final rounding is applied via
     * {@link Money#of(String, java.math.BigDecimal)}.
     */
    Money perUnitAmount(String currency) {
      if (!currency.equals(totalAmount.currency())) {
        throw new IllegalArgumentException("Currency mismatch in per-unit calculation");
      }

      BigDecimal raw =
          totalAmount.amount().divide(BigDecimal.valueOf(totalQuantity), MoneyPolicy.WORK_CONTEXT);

      return Money.of(currency, raw);
    }
  }
}
