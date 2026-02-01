package com.nenkov.bar.domain.service.payment.writeoff;

import static com.nenkov.bar.testsupport.TestFixtures.itemId;
import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.PaymentSelection;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOffReason;
import com.nenkov.bar.domain.service.payment.PaymentCalculationContext;
import com.nenkov.bar.domain.service.payment.SessionItemSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class ItemWriteOffAllocationTest {

  private static final String BGN = "BGN";

  private static final OrderItemId ITEM_A = itemId("00000000-0000-0000-0000-000000000001");
  private static final OrderItemId ITEM_B = itemId("00000000-0000-0000-0000-000000000002");

  @Test
  void allocateToSelected_allocatesPerUnitToSelectedQty_andCapsByGross() {
    // Item A: remaining 3, selected 2, unit price 10.00 -> gross selected cap = 20.00
    // Item write-off: total 3.00 over quantity 3 -> per-unit 1.00
    // For selected qty=2 => 2.00 (<= cap 20.00)
    PaymentCalculationContext ctx =
        PaymentCalculationContext.create(
            BGN,
            List.of(new SessionItemSnapshot(ITEM_A, money(BGN, "10.00"), 3)),
            List.of(selection(ITEM_A, 2)));

    ItemWriteOffAllocation allocation =
        ItemWriteOffAllocation.from(BGN, List.of(itemWriteOff(ITEM_A, 3, money(BGN, "3.00"))));

    Map<OrderItemId, Money> grossSelectedByItem = new LinkedHashMap<>();
    grossSelectedByItem.put(ITEM_A, money(BGN, "20.00"));

    Map<OrderItemId, Money> result = allocation.allocateToSelected(ctx, grossSelectedByItem);

    assertEquals(1, result.size());
    assertEquals(money(BGN, "2.00"), result.get(ITEM_A));
  }

  @Test
  void allocateToSelected_capsByGrossSelected() {
    // Item A: per-unit 10.00, selected qty=2 => computed 20.00
    // gross cap is 15.00 => allocation must be capped to 15.00
    PaymentCalculationContext ctx =
        PaymentCalculationContext.create(
            BGN,
            List.of(new SessionItemSnapshot(ITEM_A, money(BGN, "10.00"), 2)),
            List.of(selection(ITEM_A, 2)));

    ItemWriteOffAllocation allocation =
        ItemWriteOffAllocation.from(
            BGN, List.of(itemWriteOff(ITEM_A, 2, money(BGN, "20.00")))); // per-unit 10.00

    Map<OrderItemId, Money> grossSelectedByItem = new LinkedHashMap<>();
    grossSelectedByItem.put(ITEM_A, money(BGN, "15.00"));

    Map<OrderItemId, Money> result = allocation.allocateToSelected(ctx, grossSelectedByItem);

    assertEquals(money(BGN, "15.00"), result.get(ITEM_A));
  }

  @Test
  void allocateToSelected_returnsZeroWhenNoWriteOffForItem() {
    PaymentCalculationContext ctx =
        PaymentCalculationContext.create(
            BGN,
            List.of(new SessionItemSnapshot(ITEM_A, money(BGN, "10.00"), 2)),
            List.of(selection(ITEM_A, 2)));

    ItemWriteOffAllocation allocation = ItemWriteOffAllocation.from(BGN, List.of());

    Map<OrderItemId, Money> grossSelectedByItem = new LinkedHashMap<>();
    grossSelectedByItem.put(ITEM_A, money(BGN, "20.00"));

    Map<OrderItemId, Money> result = allocation.allocateToSelected(ctx, grossSelectedByItem);

    assertEquals(money(BGN, "0.00"), result.get(ITEM_A));
  }

  @Test
  void allocateToRemaining_includesAllSessionItems_inContextOrder() {
    // A remaining 2, B remaining 1
    // Write-off only for A: total 2.00 over qty 2 => per-unit 1.00 => remaining(2) => 2.00
    PaymentCalculationContext ctx =
        PaymentCalculationContext.create(
            BGN,
            List.of(
                new SessionItemSnapshot(ITEM_A, money(BGN, "10.00"), 2),
                new SessionItemSnapshot(ITEM_B, money(BGN, "5.00"), 1)),
            List.of(selection(ITEM_A, 1))); // selections irrelevant for remaining allocation

    ItemWriteOffAllocation allocation =
        ItemWriteOffAllocation.from(BGN, List.of(itemWriteOff(ITEM_A, 2, money(BGN, "2.00"))));

    Map<OrderItemId, Money> grossRemainingByItem = new LinkedHashMap<>();
    grossRemainingByItem.put(ITEM_A, money(BGN, "20.00"));
    grossRemainingByItem.put(ITEM_B, money(BGN, "5.00"));

    Map<OrderItemId, Money> result = allocation.allocateToRemaining(ctx, grossRemainingByItem);

    // Includes both items; B gets zero because no write-off exists for it.
    assertEquals(2, result.size());
    assertEquals(money(BGN, "2.00"), result.get(ITEM_A));
    assertEquals(money(BGN, "0.00"), result.get(ITEM_B));
  }

  @Test
  void allocateToSelected_throwsWhenGrossCapMissing() {
    PaymentCalculationContext ctx =
        PaymentCalculationContext.create(
            BGN,
            List.of(new SessionItemSnapshot(ITEM_A, money(BGN, "10.00"), 1)),
            List.of(selection(ITEM_A, 1)));

    ItemWriteOffAllocation allocation =
        ItemWriteOffAllocation.from(BGN, List.of(itemWriteOff(ITEM_A, 1, money(BGN, "1.00"))));

    // Missing cap for ITEM_A should fail fast.
    Map<OrderItemId, Money> grossSelectedByItem = new LinkedHashMap<>();

    assertThrows(
        IllegalArgumentException.class,
        () -> allocation.allocateToSelected(ctx, grossSelectedByItem));
  }

  @Test
  void allocateToSelected_aggregatesMultipleWriteOffsForSameItem() {
    // Two write-offs for the same item:
    // total amount = 3.00 + 1.00 = 4.00, total qty = 3 + 1 = 4 => per-unit 1.00
    // selected qty=2 => 2.00
    PaymentCalculationContext ctx =
        PaymentCalculationContext.create(
            BGN,
            List.of(new SessionItemSnapshot(ITEM_A, money(BGN, "10.00"), 10)),
            List.of(selection(ITEM_A, 2)));

    ItemWriteOffAllocation allocation =
        ItemWriteOffAllocation.from(
            BGN,
            List.of(
                itemWriteOff(ITEM_A, 3, money(BGN, "3.00")),
                itemWriteOff(ITEM_A, 1, money(BGN, "1.00"))));

    Map<OrderItemId, Money> grossSelectedByItem = new LinkedHashMap<>();
    grossSelectedByItem.put(ITEM_A, money(BGN, "20.00"));

    Map<OrderItemId, Money> result = allocation.allocateToSelected(ctx, grossSelectedByItem);

    assertEquals(money(BGN, "2.00"), result.get(ITEM_A));
  }

  @Test
  void allocateToSelected_limitsByAggregatedQuantity_whenScopeExceedsAggregatedQty() {
    // aggregated: amount 3.00 over qty 3 => per-unit 1.00
    // selected qty=5, but aggregatedQty=3 => allocQty=3 => 3.00
    PaymentCalculationContext ctx =
        PaymentCalculationContext.create(
            BGN,
            List.of(new SessionItemSnapshot(ITEM_A, money(BGN, "10.00"), 10)),
            List.of(selection(ITEM_A, 5)));

    ItemWriteOffAllocation allocation =
        ItemWriteOffAllocation.from(BGN, List.of(itemWriteOff(ITEM_A, 3, money(BGN, "3.00"))));

    Map<OrderItemId, Money> grossSelectedByItem = new LinkedHashMap<>();
    grossSelectedByItem.put(ITEM_A, money(BGN, "50.00"));

    Map<OrderItemId, Money> result = allocation.allocateToSelected(ctx, grossSelectedByItem);

    assertEquals(money(BGN, "3.00"), result.get(ITEM_A));
  }

  @Test
  void allocateToRemaining_whenRemainingQtyZero_returnsZero() {
    PaymentCalculationContext ctx =
        PaymentCalculationContext.create(
            BGN,
            List.of(
                new SessionItemSnapshot(ITEM_A, money(BGN, "10.00"), 0),
                new SessionItemSnapshot(ITEM_B, money(BGN, "5.00"), 1)),
            List.of(selection(ITEM_B, 1)));

    ItemWriteOffAllocation allocation =
        ItemWriteOffAllocation.from(
            BGN,
            List.of(
                itemWriteOff(ITEM_A, 1, money(BGN, "1.00")),
                itemWriteOff(ITEM_B, 1, money(BGN, "1.00"))));

    Map<OrderItemId, Money> grossRemainingByItem = new LinkedHashMap<>();
    grossRemainingByItem.put(ITEM_A, money(BGN, "0.00"));
    grossRemainingByItem.put(ITEM_B, money(BGN, "5.00"));

    Map<OrderItemId, Money> result = allocation.allocateToRemaining(ctx, grossRemainingByItem);

    assertEquals(money(BGN, "0.00"), result.get(ITEM_A));
    assertEquals(money(BGN, "1.00"), result.get(ITEM_B));
  }

  @Test
  void from_currencyMismatch_rejected() {
    List<ItemWriteOff> itemWriteOffs = List.of(itemWriteOff(ITEM_A, 1, money("EUR", "1.00")));
    assertThrows(
        IllegalArgumentException.class,
        () -> ItemWriteOffAllocation.from(BGN, itemWriteOffs)); // mismatch
  }

  @Test
  void allocateToSelected_preservesSelectedItemOrder() {
    // Selected order should be A then B (because ctx.selectedQtyByItem() preserves insertion
    // order).
    PaymentCalculationContext ctx =
        PaymentCalculationContext.create(
            BGN,
            List.of(
                new SessionItemSnapshot(ITEM_A, money(BGN, "10.00"), 2),
                new SessionItemSnapshot(ITEM_B, money(BGN, "10.00"), 2)),
            List.of(selection(ITEM_A, 1), selection(ITEM_B, 1)));

    ItemWriteOffAllocation allocation =
        ItemWriteOffAllocation.from(
            BGN,
            List.of(
                itemWriteOff(ITEM_A, 1, money(BGN, "1.00")),
                itemWriteOff(ITEM_B, 1, money(BGN, "1.00"))));

    Map<OrderItemId, Money> grossSelectedByItem = new LinkedHashMap<>();
    grossSelectedByItem.put(ITEM_A, money(BGN, "10.00"));
    grossSelectedByItem.put(ITEM_B, money(BGN, "10.00"));

    Map<OrderItemId, Money> result = allocation.allocateToSelected(ctx, grossSelectedByItem);

    // Deterministic order check (LinkedHashMap iteration)
    assertEquals(List.of(ITEM_A, ITEM_B), List.copyOf(result.keySet()));
  }

  private static PaymentSelection selection(OrderItemId itemId, int qty) {
    return PaymentSelection.of(itemId, qty);
  }

  private static ItemWriteOff itemWriteOff(OrderItemId itemId, int qty, Money amount) {
    return ItemWriteOff.of(itemId, qty, amount, WriteOffReason.DISCOUNT, null);
  }
}
