package com.nenkov.bar.domain.model.payment;

import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.*;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaidItemTest {

  @Test
  void creates_successfully() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());

    PaidItem item = PaidItem.of(itemId, 2, money("USD", "3.00"), money("USD", "5.00"));

    assertEquals(itemId, item.itemId());
    assertEquals(2, item.quantity());
    assertEquals(money("USD", "3.00"), item.unitPriceAtPayment());
    assertEquals(money("USD", "5.00"), item.paidAmount());
  }

  @Test
  void itemId_null_rejected() {
    Money unitPrice = money("USD", "3.00");
    Money paidAmount = money("USD", "5.00");

    NullPointerException ex =
        assertThrows(NullPointerException.class, () -> PaidItem.of(null, 1, unitPrice, paidAmount));

    assertTrue(ex.getMessage().contains("itemId"));
  }

  @Test
  void unitPriceAtPayment_null_rejected() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money paidAmount = money("USD", "1.00");

    NullPointerException ex =
        assertThrows(NullPointerException.class, () -> PaidItem.of(itemId, 1, null, paidAmount));

    assertTrue(ex.getMessage().contains("unitPriceAtPayment"));
  }

  @Test
  void paidAmount_null_rejected() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money unitPrice = money("USD", "1.00");

    NullPointerException ex =
        assertThrows(NullPointerException.class, () -> PaidItem.of(itemId, 1, unitPrice, null));

    assertTrue(ex.getMessage().contains("paidAmount"));
  }

  @Test
  void quantity_mustBePositive() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money money = money("USD", "1.00");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> PaidItem.of(itemId, 0, money, money));

    assertTrue(ex.getMessage().contains("quantity"));
  }

  @Test
  void paidAmount_mustBeGreaterThanZero() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money money = money("USD", "1.00");
    Money zero = Money.zero("USD");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> PaidItem.of(itemId, 1, money, zero));

    assertTrue(ex.getMessage().toLowerCase().contains("greater than zero"));
  }

  @Test
  void currency_mismatch_rejected() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money usd = money("USD", "1.00");
    Money eur = money("EUR", "1.00");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> PaidItem.of(itemId, 1, usd, eur));

    assertTrue(ex.getMessage().toLowerCase().contains("currency mismatch"));
  }

  @Test
  void paidAmount_mustNotExceed_unitPriceTimesQuantity() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money price = money("USD", "5.00");
    Money paidAmount = money("USD", "10.01");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> PaidItem.of(itemId, 2, price, paidAmount));

    assertTrue(ex.getMessage().toLowerCase().contains("must not exceed"));
  }

  @Test
  void value_semantics_equalByValue() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());

    PaidItem a = PaidItem.of(itemId, 2, money("USD", "3.00"), money("USD", "5.00"));
    PaidItem b = PaidItem.of(itemId, 2, money("USD", "3.00"), money("USD", "5.00"));

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }
}
