package com.nenkov.bar.domain.model.payment;

import static org.junit.jupiter.api.Assertions.*;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaidItemTest {

  @Test
  void creates_successfully() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());

    PaidItem item =
        PaidItem.of(
            itemId,
            2,
            Money.of("USD", new BigDecimal("3.00")),
            Money.of("USD", new BigDecimal("5.00")));

    assertEquals(itemId, item.itemId());
    assertEquals(2, item.quantity());
    assertEquals(Money.of("USD", new BigDecimal("3.00")), item.unitPriceAtPayment());
    assertEquals(Money.of("USD", new BigDecimal("5.00")), item.paidAmount());
  }

  @Test
  void quantity_mustBePositive() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money money = Money.of("USD", new BigDecimal("1.00"));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> PaidItem.of(itemId, 0, money, money));

    assertTrue(ex.getMessage().contains("quantity"));
  }

  @Test
  void paidAmount_mustBeGreaterThanZero() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money money = Money.of("USD", new BigDecimal("1.00"));
    Money zero = Money.zero("USD");

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> PaidItem.of(itemId, 1, money, zero));

    assertTrue(ex.getMessage().toLowerCase().contains("greater than zero"));
  }

  @Test
  void currency_mismatch_rejected() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money usd = Money.of("USD", new BigDecimal("1.00"));
    Money eur = Money.of("EUR", new BigDecimal("1.00"));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> PaidItem.of(itemId, 1, usd, eur));

    assertTrue(ex.getMessage().toLowerCase().contains("currency mismatch"));
  }

  @Test
  void paidAmount_mustNotExceed_unitPriceTimesQuantity() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money price = Money.of("USD", new BigDecimal("5.00"));
    Money paidAmount = Money.of("USD", new BigDecimal("10.01"));
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> PaidItem.of(itemId, 2, price, paidAmount));

    assertTrue(ex.getMessage().toLowerCase().contains("must not exceed"));
  }
}
