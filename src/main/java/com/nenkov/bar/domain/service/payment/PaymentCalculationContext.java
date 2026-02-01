package com.nenkov.bar.domain.service.payment;

import com.nenkov.bar.domain.model.payment.PaymentSelection;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable calculation snapshot for payment-related domain services.
 *
 * <p>This context indexes the current session items by {@link OrderItemId} and consolidates payer
 * selections into a single selected quantity per item.
 *
 * <p>Determinism: internal maps preserve insertion order (via {@code LinkedHashMap}) so downstream
 * allocation and remainder distribution produce stable results across runs.
 *
 * <p>Creation invariants (validated eagerly):
 *
 * <ul>
 *   <li>{@code currency} is non-null
 *   <li>{@code selections} is non-empty
 *   <li>All session items use the provided {@code currency}
 *   <li>No duplicate session item ids
 *   <li>Every selected item exists in {@code sessionItems}
 *   <li>Each consolidated selected quantity is strictly positive
 *   <li>Selected quantity does not exceed the item's {@code remainingQuantity}
 * </ul>
 *
 * <p>All exposed maps are immutable snapshots ({@link Collections#unmodifiableMap(Map)}).
 */
public final class PaymentCalculationContext {

  private final String currency;
  private final Map<OrderItemId, SessionItemSnapshot> itemById;
  private final Map<OrderItemId, Integer> selectedQtyByItem;
  private final Map<OrderItemId, Integer> remainingQtyByItem;

  private PaymentCalculationContext(
      String currency,
      Map<OrderItemId, SessionItemSnapshot> itemById,
      Map<OrderItemId, Integer> selectedQtyByItem) {
    this.currency = currency;
    this.itemById = Collections.unmodifiableMap(new LinkedHashMap<>(itemById));
    this.selectedQtyByItem = Collections.unmodifiableMap(new LinkedHashMap<>(selectedQtyByItem));
    this.remainingQtyByItem =
        Collections.unmodifiableMap(new LinkedHashMap<>(buildRemainingQtyByItem(itemById)));
  }

  /** Returns the session currency used for all calculations. */
  public String currency() {
    return currency;
  }

  /** Returns an immutable map of session items by id. */
  public Map<OrderItemId, SessionItemSnapshot> itemById() {
    return itemById;
  }

  /** Returns an immutable map of consolidated selection quantities by item id. */
  public Map<OrderItemId, Integer> selectedQtyByItem() {
    return selectedQtyByItem;
  }

  /** Returns an immutable map of remaining quantities by item id. */
  public Map<OrderItemId, Integer> remainingQtyByItem() {
    return remainingQtyByItem;
  }

  /**
   * Creates a validated calculation context from the current session items and payment selections.
   *
   * <p>Selections may contain multiple entries for the same item id. They are consolidated by
   * summing quantities per item, preserving the order in which item ids are first encountered.
   *
   * @param currency session currency expected across all items
   * @param sessionItems current session item snapshots
   * @param selections payer selections (must be non-empty)
   * @return an immutable calculation context
   * @throws IllegalArgumentException if validation fails (unknown items, non-positive quantities,
   *     currency mismatches, duplicates, or selections exceeding remaining quantities)
   */
  public static PaymentCalculationContext create(
      String currency, List<SessionItemSnapshot> sessionItems, List<PaymentSelection> selections) {
    Objects.requireNonNull(currency, "currency must not be null");
    Objects.requireNonNull(sessionItems, "sessionItems must not be null");
    Objects.requireNonNull(selections, "selections must not be null");

    if (selections.isEmpty()) {
      throw new IllegalArgumentException("selections must not be empty");
    }

    Map<OrderItemId, SessionItemSnapshot> itemById = indexSessionItems(sessionItems, currency);
    Map<OrderItemId, Integer> selectedQtyByItem = consolidateSelections(selections);

    validateSelections(itemById, selectedQtyByItem);

    return new PaymentCalculationContext(currency, itemById, selectedQtyByItem);
  }

  /**
   * Derives remaining payable quantities for each item from the indexed session snapshot.
   *
   * <p>The resulting map preserves the same deterministic order as {@code itemById}.
   */
  private static Map<OrderItemId, Integer> buildRemainingQtyByItem(
      Map<OrderItemId, SessionItemSnapshot> itemById) {
    Map<OrderItemId, Integer> result = new LinkedHashMap<>();
    for (Map.Entry<OrderItemId, SessionItemSnapshot> e : itemById.entrySet()) {
      result.put(e.getKey(), e.getValue().remainingQuantity());
    }
    return result;
  }

  /**
   * Indexes session items by {@link OrderItemId} and validates currency consistency.
   *
   * <p>Rejects duplicate item ids. The resulting map preserves the input list to support
   * deterministic downstream processing.
   */
  private static Map<OrderItemId, SessionItemSnapshot> indexSessionItems(
      List<SessionItemSnapshot> items, String currency) {
    Map<OrderItemId, SessionItemSnapshot> map = new LinkedHashMap<>();
    for (SessionItemSnapshot item : items) {
      if (!currency.equals(item.unitPrice().currency())) {
        throw new IllegalArgumentException(
            "Currency mismatch in session items: itemId="
                + item.itemId().value()
                + ", expected="
                + currency
                + ", actual="
                + item.unitPrice().currency());
      }
      SessionItemSnapshot prev = map.put(item.itemId(), item);
      if (prev != null) {
        throw new IllegalArgumentException("Duplicate session item id: " + item.itemId().value());
      }
    }
    return map;
  }

  /**
   * Consolidates selections by summing quantities per {@link OrderItemId}.
   *
   * <p>Order is preserved: the first time an item id is encountered defines its position in the
   * resulting map. This supports deterministic downstream allocation logic.
   */
  private static Map<OrderItemId, Integer> consolidateSelections(
      List<PaymentSelection> selections) {
    Map<OrderItemId, Integer> qtyById = new LinkedHashMap<>();
    for (PaymentSelection sel : selections) {
      qtyById.merge(sel.itemId(), sel.quantity(), Integer::sum);
    }
    return qtyById;
  }

  /**
   * Validates consolidated selections against the indexed session snapshot.
   *
   * <p>Ensures all selected items exist, selected quantities are strictly positive, and selections
   * do not exceed each item's {@code remainingQuantity}.
   */
  private static void validateSelections(
      Map<OrderItemId, SessionItemSnapshot> itemById, Map<OrderItemId, Integer> selectedQtyByItem) {
    for (Map.Entry<OrderItemId, Integer> entry : selectedQtyByItem.entrySet()) {
      OrderItemId itemId = entry.getKey();
      int qty = entry.getValue();
      if (qty <= 0) {
        throw new IllegalArgumentException(
            "Selected quantity must be > 0 for item: " + itemId.value());
      }

      SessionItemSnapshot snapshot = itemById.get(itemId);
      if (snapshot == null) {
        throw new IllegalArgumentException("Selected item not found: " + itemId.value());
      }
      if (qty > snapshot.remainingQuantity()) {
        throw new IllegalArgumentException(
            "Selected quantity exceeds remaining quantity for item: " + itemId.value());
      }
    }
  }
}
