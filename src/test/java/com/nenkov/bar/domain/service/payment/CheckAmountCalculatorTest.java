// src/test/java/com/nenkov/bar/domain/service/payment/CheckAmountCalculatorTest.java
package com.nenkov.bar.domain.service.payment;

import static com.nenkov.bar.testsupport.TestFixtures.itemId;
import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.*;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.payment.PaymentSelection;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CheckAmountCalculatorTest {

  private static final String BGN = "BGN";

  private static final OrderItemId A = itemId("00000000-0000-0000-0000-000000000001");
  private static final OrderItemId B = itemId("00000000-0000-0000-0000-000000000002");

  private final CheckAmountCalculator calc = new DefaultCheckAmountCalculator();

  @Test
  void grossSelectedAmount_singleItem() {
    List<SessionItemSnapshot> items =
        List.of(
            new SessionItemSnapshot(A, money(BGN, "2.50"), 10),
            new SessionItemSnapshot(B, money(BGN, "1.00"), 10));

    List<PaymentSelection> selections = List.of(PaymentSelection.of(A, 4));

    Money gross = calc.grossSelectedAmount(BGN, items, selections);

    assertEquals(money(BGN, "10.00"), gross);
  }

  @Test
  void grossSelectedAmount_multipleItems_sums() {
    List<SessionItemSnapshot> items =
        List.of(
            new SessionItemSnapshot(A, money(BGN, "2.50"), 10),
            new SessionItemSnapshot(B, money(BGN, "1.20"), 10));

    List<PaymentSelection> selections =
        List.of(PaymentSelection.of(A, 2), PaymentSelection.of(B, 3));

    Money gross = calc.grossSelectedAmount(BGN, items, selections);

    // A: 2 * 2.50 = 5.00
    // B: 3 * 1.20 = 3.60
    assertEquals(money(BGN, "8.60"), gross);
  }

  @Test
  void grossSelectedAmount_selectedItemMissing_rejected() {
    List<SessionItemSnapshot> items = List.of(new SessionItemSnapshot(A, money(BGN, "2.50"), 10));
    List<PaymentSelection> selections = List.of(PaymentSelection.of(B, 1));

    assertThrows(
        IllegalArgumentException.class, () -> calc.grossSelectedAmount(BGN, items, selections));
  }

  @Test
  void grossSelectedAmount_currencyMismatch_rejectedFromMoneyCompare() {
    // Money.plus requires same currency; if caller passes a different currency than the items use,
    // the sum will fail with a currency mismatch (IllegalArgumentException from Money).
    List<SessionItemSnapshot> items = List.of(new SessionItemSnapshot(A, money("EUR", "1.00"), 10));
    List<PaymentSelection> selections = List.of(PaymentSelection.of(A, 1));

    assertThrows(
        IllegalArgumentException.class, () -> calc.grossSelectedAmount(BGN, items, selections));
  }
}
