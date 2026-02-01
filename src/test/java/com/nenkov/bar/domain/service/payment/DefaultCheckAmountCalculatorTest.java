package com.nenkov.bar.domain.service.payment;

import static com.nenkov.bar.testsupport.TestFixtures.itemId;
import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.CheckQuote;
import com.nenkov.bar.domain.model.payment.PaidItem;
import com.nenkov.bar.domain.model.payment.PaymentSelection;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOffReason;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DefaultCheckAmountCalculatorTest {

  private static final String BGN = "BGN";

  private static final OrderItemId A = itemId("00000000-0000-0000-0000-000000000001");
  private static final OrderItemId B = itemId("00000000-0000-0000-0000-000000000002");
  private static final OrderItemId C = itemId("00000000-0000-0000-0000-000000000003");

  private final DefaultCheckAmountCalculator calc = new DefaultCheckAmountCalculator();

  private static PaymentSelection sel(OrderItemId id, int qty) {
    return PaymentSelection.of(id, qty);
  }

  private static ItemWriteOff itemWO(OrderItemId id, int qty, Money amount) {
    return ItemWriteOff.of(id, qty, amount, WriteOffReason.DISCOUNT, null);
  }

  private static WriteOff sessionWO(Money amount) {
    // If your API is different, change this single line.
    return WriteOff.of(amount, WriteOffReason.DISCOUNT, null);
  }

  @Test
  void quote_noWriteOffs_returnsGrossSelected() {
    List<SessionItemSnapshot> session =
        List.of(
            new SessionItemSnapshot(A, money(BGN, "10.00"), 5),
            new SessionItemSnapshot(B, money(BGN, "7.50"), 5));

    List<PaymentSelection> selections = List.of(sel(A, 2), sel(B, 3));

    CheckQuote quote =
        calc.quote(BGN, session, selections, List.of() /* item WOs */, List.of() /* session WOs */);

    assertEquals(money(BGN, "42.50"), quote.checkAmount()); // 10*2 + 7.5*3
    assertEquals(2, quote.paidItems().size());

    PaidItem a = quote.paidItems().get(0);
    PaidItem b = quote.paidItems().get(1);

    assertEquals(A, a.itemId());
    assertEquals(2, a.quantity());
    assertEquals(money(BGN, "10.00"), a.unitPriceAtPayment());
    assertEquals(money(BGN, "20.00"), a.paidAmount());

    assertEquals(B, b.itemId());
    assertEquals(3, b.quantity());
    assertEquals(money(BGN, "7.50"), b.unitPriceAtPayment());
    assertEquals(money(BGN, "22.50"), b.paidAmount());
  }

  @Test
  void quote_appliesItemWriteOffsFirst() {
    // A: unit 10, remaining 3, selected 2 => grossSelected=20
    // Item WO: total 3.00 over qty 3 => per-unit 1.00 => selected allocation 2.00
    // Paid for A: 18.00
    List<SessionItemSnapshot> session = List.of(new SessionItemSnapshot(A, money(BGN, "10.00"), 3));
    List<PaymentSelection> selections = List.of(sel(A, 2));

    List<ItemWriteOff> itemWriteOffs = List.of(itemWO(A, 3, money(BGN, "3.00")));
    CheckQuote quote = calc.quote(BGN, session, selections, itemWriteOffs, List.of());

    assertEquals(money(BGN, "18.00"), quote.checkAmount());
    assertEquals(1, quote.paidItems().size());
    assertEquals(money(BGN, "18.00"), quote.paidItems().getFirst().paidAmount());
  }

  @Test
  void quote_appliesSessionWriteOffsProportionally_onNetSelected() {
    // Two items, net selected is exactly their gross (no item WOs).
    // totalSessionWriteOff=3.00, totalNetSelected=30.00, totalNetRemaining=30.00 => share=3.00.
    // Proportional allocation over caps (10 and 20) => 1.00 and 2.00 => paid 9 and 18.
    List<SessionItemSnapshot> session =
        List.of(
            new SessionItemSnapshot(A, money(BGN, "10.00"), 1),
            new SessionItemSnapshot(B, money(BGN, "20.00"), 1));
    List<PaymentSelection> selections = List.of(sel(A, 1), sel(B, 1));

    List<WriteOff> sessionWriteOffs = List.of(sessionWO(money(BGN, "3.00")));

    CheckQuote quote = calc.quote(BGN, session, selections, List.of(), sessionWriteOffs);

    assertEquals(money(BGN, "27.00"), quote.checkAmount());
    assertEquals(2, quote.paidItems().size());

    PaidItem paidA = quote.paidItems().get(0);
    PaidItem paidB = quote.paidItems().get(1);

    assertEquals(money(BGN, "9.00"), paidA.paidAmount());
    assertEquals(money(BGN, "18.00"), paidB.paidAmount());
  }

  @Test
  void quote_distributesRoundingRemainder_deterministically_policyA() {
    // Make the session write-off share 0.01 and distribute it across 3 equal caps.
    // With equal rounding errors, the policy tie-break is by OrderItemId.value().toString()
    // ascending,
    // so A gets the cent. Paid amounts become: A=0.99, B=1.00, C=1.00.
    List<SessionItemSnapshot> session =
        List.of(
            new SessionItemSnapshot(A, money(BGN, "1.00"), 1),
            new SessionItemSnapshot(B, money(BGN, "1.00"), 1),
            new SessionItemSnapshot(C, money(BGN, "1.00"), 1));
    List<PaymentSelection> selections = List.of(sel(A, 1), sel(B, 1), sel(C, 1));

    List<WriteOff> sessionWriteOffs = List.of(sessionWO(money(BGN, "0.01")));

    CheckQuote quote = calc.quote(BGN, session, selections, List.of(), sessionWriteOffs);

    assertEquals(money(BGN, "2.99"), quote.checkAmount());
    assertEquals(3, quote.paidItems().size());

    // Selection order is A, B, C here (first encounter), so paidItems should match that.
    assertEquals(A, quote.paidItems().get(0).itemId());
    assertEquals(money(BGN, "0.99"), quote.paidItems().get(0).paidAmount());

    assertEquals(B, quote.paidItems().get(1).itemId());
    assertEquals(money(BGN, "1.00"), quote.paidItems().get(1).paidAmount());

    assertEquals(C, quote.paidItems().get(2).itemId());
    assertEquals(money(BGN, "1.00"), quote.paidItems().get(2).paidAmount());
  }

  @Test
  void quote_preservesSelectionEncounterOrder_inPaidItems() {
    // Selections encounter order is B then A, so paidItems should come out as B then A.
    List<SessionItemSnapshot> session =
        List.of(
            new SessionItemSnapshot(A, money(BGN, "10.00"), 1),
            new SessionItemSnapshot(B, money(BGN, "5.00"), 1));

    List<PaymentSelection> selections = List.of(sel(B, 1), sel(A, 1));

    CheckQuote quote = calc.quote(BGN, session, selections, List.of(), List.of());

    assertEquals(2, quote.paidItems().size());
    assertEquals(B, quote.paidItems().get(0).itemId());
    assertEquals(A, quote.paidItems().get(1).itemId());
  }

  @Test
  void quote_whenTotalNetSelectedZero_throws() {
    // Gross selected is 10.00. Item WO is capped to gross selected (10.00) => net selected is zero.
    List<SessionItemSnapshot> session = List.of(new SessionItemSnapshot(A, money(BGN, "10.00"), 1));
    List<PaymentSelection> selections = List.of(sel(A, 1));

    // total 10.00 over qty 1 => per-unit 10.00 => selected allocation 10.00 => net 0.00
    List<ItemWriteOff> itemWriteOffs = List.of(itemWO(A, 1, money(BGN, "10.00")));
    List<WriteOff> sessionWriteOffs = List.of();

    assertThrows(
        IllegalArgumentException.class,
        () -> calc.quote(BGN, session, selections, itemWriteOffs, sessionWriteOffs));
  }

  @Test
  void quote_currencyMismatch_inSessionWriteOffs_throws() {
    List<SessionItemSnapshot> session = List.of(new SessionItemSnapshot(A, money(BGN, "10.00"), 1));
    List<PaymentSelection> selections = List.of(sel(A, 1));

    List<WriteOff> sessionWriteOffs = List.of(sessionWO(money("EUR", "1.00")));
    List<ItemWriteOff> itemWriteOffs = List.of();
    assertThrows(
        IllegalArgumentException.class,
        () -> calc.quote(BGN, session, selections, itemWriteOffs, sessionWriteOffs));
  }

  @Test
  void quote_currencyMismatch_inItemWriteOffs_throws() {
    List<SessionItemSnapshot> session = List.of(new SessionItemSnapshot(A, money(BGN, "10.00"), 1));
    List<PaymentSelection> selections = List.of(sel(A, 1));

    List<ItemWriteOff> itemWriteOffs = List.of(itemWO(A, 1, money("EUR", "1.00")));
    List<WriteOff> sessionWriteOffs = List.of();
    assertThrows(
        IllegalArgumentException.class,
        () -> calc.quote(BGN, session, selections, itemWriteOffs, sessionWriteOffs));
  }

  @Test
  void quote_whenPaidAmountMissingForSelectedItem_throws() {
    // Make the net selected 0.00 for B (selected), by having an item write-off fully wipe it.
    // ItemWriteOffAllocation will include B in the selected allocation with 0.00,
    // subtractPerItem will produce netSelectedByItem[B]=0.00,
    // and requirePresentAndPositive will throw for B.
    List<SessionItemSnapshot> session =
        List.of(
            new SessionItemSnapshot(A, money(BGN, "5.00"), 1),
            new SessionItemSnapshot(B, money(BGN, "10.00"), 1));

    List<PaymentSelection> selections = List.of(sel(A, 1), sel(B, 1));

    // Write off B by its full gross (10.00) => net selected for B becomes 0.00
    List<ItemWriteOff> itemWriteOffs = List.of(itemWO(B, 1, money(BGN, "10.00")));

    List<WriteOff> sessionWriteOffs = List.of();

    // No session write-offs; A remains payable, so we don't fail earlier with "totalNetSelected is
    // zero".
    assertThrows(
        IllegalArgumentException.class,
        () -> calc.quote(BGN, session, selections, itemWriteOffs, sessionWriteOffs));
  }

  @Test
  void quote_whenPaidAmountZeroForSelectedItem_throws() {
    // Similar to the previous test but targets the explicit "zero payable amount" branch.
    // Select only B and wipe it fully => net selected total would be zero; to avoid failing
    // earlier,
    // we include another selected item A with a positive net.
    List<SessionItemSnapshot> session =
        List.of(
            new SessionItemSnapshot(A, money(BGN, "1.00"), 1),
            new SessionItemSnapshot(B, money(BGN, "10.00"), 1));

    List<PaymentSelection> selections = List.of(sel(A, 1), sel(B, 1));

    List<ItemWriteOff> itemWriteOffs = List.of(itemWO(B, 1, money(BGN, "10.00")));
    List<WriteOff> sessionWriteOffs = List.of();
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> calc.quote(BGN, session, selections, itemWriteOffs, sessionWriteOffs));

    // Message should include "zero payable amount" and the item id.
    assertTrue(ex.getMessage().toLowerCase().contains("zero payable amount"));
    assertTrue(ex.getMessage().contains(B.value().toString()));
  }

  @Test
  void quote_nullItemWriteOffs_rejected() {
    List<SessionItemSnapshot> session = List.of(new SessionItemSnapshot(A, money(BGN, "10.00"), 1));
    List<PaymentSelection> selections = List.of(sel(A, 1));
    List<WriteOff> sessionWriteOffs = List.of();
    assertThrows(
        NullPointerException.class,
        () -> calc.quote(BGN, session, selections, null, sessionWriteOffs));
  }

  // ---- helpers ----

  @Test
  void quote_nullSessionWriteOffs_rejected() {
    List<SessionItemSnapshot> session = List.of(new SessionItemSnapshot(A, money(BGN, "10.00"), 1));
    List<PaymentSelection> selections = List.of(sel(A, 1));
    List<ItemWriteOff> itemWriteOffs = List.of();

    assertThrows(
        NullPointerException.class,
        () -> calc.quote(BGN, session, selections, itemWriteOffs, null));
  }
}
