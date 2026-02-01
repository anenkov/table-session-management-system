package com.nenkov.bar.domain.model.payment;

import static com.nenkov.bar.testsupport.TestFixtures.itemId;
import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CheckQuoteTest {

  private static final String BGN = "BGN";

  private static final OrderItemId A = itemId("00000000-0000-0000-0000-000000000001");
  private static final OrderItemId B = itemId("00000000-0000-0000-0000-000000000002");

  @Test
  void of_success_copiesPaidItems_defensively() {
    Money checkAmount = money(BGN, "3.00");

    List<PaidItem> input = new ArrayList<>();
    input.add(paid(A, 1, "2.00", "2.00"));
    input.add(paid(B, 1, "1.00", "1.00"));

    CheckQuote quote = CheckQuote.of(checkAmount, input);

    assertEquals(checkAmount, quote.checkAmount());
    assertEquals(2, quote.paidItems().size());

    // Mutate original list - quote must not change
    input.clear();
    assertEquals(2, quote.paidItems().size());

    List<PaidItem> paidItems = quote.paidItems();
    PaidItem itemA = paid(A, 1, "1.00", "1.00");

    // And the list returned by the record must be unmodifiable (List.copyOf)
    assertThrows(UnsupportedOperationException.class, () -> paidItems.add(itemA));
  }

  @Test
  void constructor_checkAmountZero_rejected() {
    Money checkAmount = money(BGN, "0.00");
    List<PaidItem> paidItems = List.of(paid(A, 1, "1.00", "1.00"));

    assertThrows(IllegalArgumentException.class, () -> CheckQuote.of(checkAmount, paidItems));
  }

  @Test
  void constructor_paidItemsEmpty_rejected() {
    Money checkAmount = money(BGN, "1.00");
    List<PaidItem> paidItems = List.of();

    assertThrows(IllegalArgumentException.class, () -> CheckQuote.of(checkAmount, paidItems));
  }

  @Test
  void constructor_currencyMismatchBetweenCheckAmountAndPaidItems_rejected() {
    Money checkAmount = money(BGN, "2.00");
    List<PaidItem> paidItems =
        List.of(
            PaidItem.of(
                A, 1, money("EUR", "2.00"), money("EUR", "2.00"))); // paidAmount currency != BGN

    assertThrows(IllegalArgumentException.class, () -> CheckQuote.of(checkAmount, paidItems));
  }

  @Test
  void constructor_nullCheckAmount_rejected() {
    List<PaidItem> paidItems = List.of(paid(A, 1, "1.00", "1.00"));
    assertThrows(NullPointerException.class, () -> CheckQuote.of(null, paidItems));
  }

  @Test
  void constructor_nullPaidItems_rejected() {
    Money checkAmount = money(BGN, "1.00");
    assertThrows(NullPointerException.class, () -> CheckQuote.of(checkAmount, null));
  }

  private static PaidItem paid(OrderItemId id, int qty, String unitPrice, String paidAmount) {
    return PaidItem.of(id, qty, money(BGN, unitPrice), money(BGN, paidAmount));
  }
}
