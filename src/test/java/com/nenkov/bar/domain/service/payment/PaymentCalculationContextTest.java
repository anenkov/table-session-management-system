package com.nenkov.bar.domain.service.payment;

import static com.nenkov.bar.testsupport.TestFixtures.itemId;
import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.*;

import com.nenkov.bar.domain.model.payment.PaymentSelection;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PaymentCalculationContextTest {

  private static final String BGN = "BGN";

  private static final OrderItemId A = itemId("00000000-0000-0000-0000-000000000001");
  private static final OrderItemId B = itemId("00000000-0000-0000-0000-000000000002");
  private static final OrderItemId C = itemId("00000000-0000-0000-0000-000000000003");

  @Test
  void create_success_indexesSessionItems_andConsolidatesSelections_preservingOrder() {
    List<SessionItemSnapshot> items =
        List.of(
            new SessionItemSnapshot(A, money(BGN, "10.00"), 5),
            new SessionItemSnapshot(B, money(BGN, "2.00"), 3),
            new SessionItemSnapshot(C, money(BGN, "1.00"), 7));

    // Selections contain duplicates; order of first encounter is: B, A, C
    List<PaymentSelection> selections =
        List.of(
            PaymentSelection.of(B, 1),
            PaymentSelection.of(A, 2),
            PaymentSelection.of(B, 2),
            PaymentSelection.of(C, 1));

    PaymentCalculationContext ctx = PaymentCalculationContext.create(BGN, items, selections);

    assertEquals(BGN, ctx.currency());

    // itemById preserves sessionItems order (A, B, C)
    assertEquals(List.of(A, B, C), new ArrayList<>(ctx.itemById().keySet()));

    // selectedQtyByItem preserves first-encounter order (B, A, C) and sums quantities
    assertEquals(List.of(B, A, C), new ArrayList<>(ctx.selectedQtyByItem().keySet()));
    assertEquals(3, ctx.selectedQtyByItem().get(B));
    assertEquals(2, ctx.selectedQtyByItem().get(A));
    assertEquals(1, ctx.selectedQtyByItem().get(C));

    // remainingQtyByItem preserves itemById order (A, B, C)
    assertEquals(List.of(A, B, C), new ArrayList<>(ctx.remainingQtyByItem().keySet()));
    assertEquals(5, ctx.remainingQtyByItem().get(A));
    assertEquals(3, ctx.remainingQtyByItem().get(B));
    assertEquals(7, ctx.remainingQtyByItem().get(C));
  }

  @Test
  void create_selectionsEmpty_rejected() {
    List<SessionItemSnapshot> items = List.of(new SessionItemSnapshot(A, money(BGN, "10.00"), 1));
    List<PaymentSelection> selections = List.of();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> PaymentCalculationContext.create(BGN, items, selections));

    assertTrue(ex.getMessage().toLowerCase().contains("selections"));
  }

  @Test
  void create_currencyMismatch_inSessionItems_rejected() {
    List<SessionItemSnapshot> items =
        List.of(
            new SessionItemSnapshot(A, money("EUR", "10.00"), 1),
            new SessionItemSnapshot(B, money(BGN, "1.00"), 1));

    List<PaymentSelection> selections = List.of(PaymentSelection.of(A, 1));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> PaymentCalculationContext.create(BGN, items, selections));

    assertTrue(ex.getMessage().toLowerCase().contains("currency mismatch"));
    assertTrue(ex.getMessage().contains(A.value().toString()));
  }

  @Test
  void create_duplicateSessionItemId_rejected() {
    SessionItemSnapshot a1 = new SessionItemSnapshot(A, money(BGN, "10.00"), 1);
    SessionItemSnapshot a2 = new SessionItemSnapshot(A, money(BGN, "10.00"), 1);

    List<PaymentSelection> selections = List.of(PaymentSelection.of(A, 1));
    List<SessionItemSnapshot> items = List.of(a1, a2);
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> PaymentCalculationContext.create(BGN, items, selections));

    assertTrue(ex.getMessage().toLowerCase().contains("duplicate"));
    assertTrue(ex.getMessage().contains(A.value().toString()));
  }

  @Test
  void create_selectedItemNotFound_rejected() {
    List<SessionItemSnapshot> items = List.of(new SessionItemSnapshot(A, money(BGN, "10.00"), 1));

    List<PaymentSelection> selections = List.of(PaymentSelection.of(B, 1));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> PaymentCalculationContext.create(BGN, items, selections));

    assertTrue(ex.getMessage().toLowerCase().contains("not found"));
    assertTrue(ex.getMessage().contains(B.value().toString()));
  }

  @Test
  void create_selectedQuantityExceedsRemaining_rejected_afterConsolidation() {
    List<SessionItemSnapshot> items = List.of(new SessionItemSnapshot(A, money(BGN, "10.00"), 2));

    // Consolidates to 3 which exceeds the remaining=2
    List<PaymentSelection> selections =
        List.of(PaymentSelection.of(A, 1), PaymentSelection.of(A, 2));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> PaymentCalculationContext.create(BGN, items, selections));

    assertTrue(ex.getMessage().toLowerCase().contains("exceeds"));
    assertTrue(ex.getMessage().contains(A.value().toString()));
  }

  @Test
  void exposedMaps_areImmutableSnapshots() {
    List<SessionItemSnapshot> items =
        List.of(
            new SessionItemSnapshot(A, money(BGN, "10.00"), 2),
            new SessionItemSnapshot(B, money(BGN, "1.00"), 1));

    List<PaymentSelection> selections = List.of(PaymentSelection.of(A, 1));

    PaymentCalculationContext ctx = PaymentCalculationContext.create(BGN, items, selections);

    Map<OrderItemId, SessionItemSnapshot> itemById = ctx.itemById();
    Map<OrderItemId, Integer> selected = ctx.selectedQtyByItem();
    Map<OrderItemId, Integer> remaining = ctx.remainingQtyByItem();

    SessionItemSnapshot item1 = items.getFirst();

    assertThrows(UnsupportedOperationException.class, () -> itemById.put(C, item1));
    assertThrows(UnsupportedOperationException.class, () -> selected.put(B, 1));
    assertThrows(UnsupportedOperationException.class, () -> remaining.put(B, 0));
  }

  @Test
  void create_duplicateSessionItemIds_rejected() {
    OrderItemId orderItemId = itemId("00000000-0000-0000-0000-000000000001");

    List<SessionItemSnapshot> sessionItems =
        List.of(
            new SessionItemSnapshot(orderItemId, money(BGN, "10.00"), 1),
            new SessionItemSnapshot(orderItemId, money(BGN, "10.00"), 2) // duplicate id
            );

    List<PaymentSelection> selections = List.of(PaymentSelection.of(orderItemId, 1));

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> PaymentCalculationContext.create(BGN, sessionItems, selections));

    assertTrue(ex.getMessage().toLowerCase().contains("duplicate"));
  }

  @Test
  void create_nullGuards_rejected() {
    List<SessionItemSnapshot> items = List.of(new SessionItemSnapshot(A, money(BGN, "10.00"), 1));
    List<PaymentSelection> selections = List.of(PaymentSelection.of(A, 1));

    NullPointerException ex1 =
        assertThrows(
            NullPointerException.class,
            () -> PaymentCalculationContext.create(null, items, selections));
    assertTrue(ex1.getMessage().contains("currency"));

    NullPointerException ex2 =
        assertThrows(
            NullPointerException.class,
            () -> PaymentCalculationContext.create(BGN, null, selections));
    assertTrue(ex2.getMessage().contains("sessionItems"));

    NullPointerException ex3 =
        assertThrows(
            NullPointerException.class, () -> PaymentCalculationContext.create(BGN, items, null));
    assertTrue(ex3.getMessage().contains("selections"));
  }
}
