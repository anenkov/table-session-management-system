package com.nenkov.bar.domain.model.discount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.writeoff.WriteOffReason;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DiscountTest {

  @Test
  void percent_discount_normalizesScale() {
    Discount d = Discount.percent(new BigDecimal("10.129"), WriteOffReason.DISCOUNT);

    assertEquals(DiscountType.PERCENT, d.type());
    assertEquals(new BigDecimal("10.13"), d.percent());
    assertTrue(d.isPercent());
    assertFalse(d.isFlatAmount());
  }

  @Test
  void percent_discount_mustBeGreaterThanZero() {
    BigDecimal zero = new BigDecimal("0.00");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> Discount.percent(zero, WriteOffReason.DISCOUNT));

    assertTrue(ex.getMessage().contains("> 0"));
  }

  @Test
  void percent_discount_mustNotExceed100() {
    BigDecimal overHundred = new BigDecimal("100.01");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Discount.percent(overHundred, WriteOffReason.DISCOUNT));

    assertTrue(ex.getMessage().contains("<= 100"));
  }

  @Test
  void percent_null_rejected() {
    NullPointerException ex =
        assertThrows(
            NullPointerException.class, () -> Discount.percent(null, WriteOffReason.DISCOUNT));
    assertTrue(ex.getMessage().contains("percent"));
  }

  @Test
  void flat_amount_discount_requiresPositiveMoney() {
    Money zero = Money.zero("USD");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Discount.flatAmount(zero, WriteOffReason.DISCOUNT));

    assertTrue(ex.getMessage().toLowerCase().contains("greater than zero"));
  }

  @Test
  void flat_amount_discount_success() {
    Discount d =
        Discount.flatAmount(Money.of("USD", new BigDecimal("5.00")), WriteOffReason.PROMOTION);

    assertEquals(DiscountType.FLAT_AMOUNT, d.type());
    assertEquals(Money.of("USD", new BigDecimal("5.00")), d.amount());
    assertTrue(d.isFlatAmount());
    assertFalse(d.isPercent());
  }

  @Test
  void note_isTrimmed_andBlankBecomesNull() {
    Discount a = Discount.percent(new BigDecimal("10.00"), WriteOffReason.DISCOUNT, "  x  ");
    Discount b = Discount.percent(new BigDecimal("10.00"), WriteOffReason.DISCOUNT, "x");

    assertEquals("x", a.note());
    assertEquals(a, b);

    Discount c = Discount.percent(new BigDecimal("10.00"), WriteOffReason.DISCOUNT, "   ");
    assertNull(c.note());
  }

  @Test
  void note_overMax_rejected() {
    String tooLong = "a".repeat(201);
    BigDecimal percent = new BigDecimal("10.00");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Discount.percent(percent, WriteOffReason.DISCOUNT, tooLong));

    assertTrue(ex.getMessage().contains("at most"));
  }

  @Test
  void amount_accessor_throws_forPercentDiscount() {
    Discount d = Discount.percent(new BigDecimal("10.00"), WriteOffReason.DISCOUNT);

    IllegalStateException ex = assertThrows(IllegalStateException.class, d::amount);
    assertTrue(ex.getMessage().contains("FLAT_AMOUNT"));
  }

  @Test
  void percent_accessor_throws_forFlatAmountDiscount() {
    Discount d =
        Discount.flatAmount(Money.of("USD", new BigDecimal("5.00")), WriteOffReason.DISCOUNT);

    IllegalStateException ex = assertThrows(IllegalStateException.class, d::percent);
    assertTrue(ex.getMessage().contains("PERCENT"));
  }
}
