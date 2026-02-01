package com.nenkov.bar.domain.service.discount;

import static com.nenkov.bar.testsupport.TestFixtures.itemId;
import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.*;

import com.nenkov.bar.domain.model.discount.Discount;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOffReason;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class DiscountCalculatorTest {

  private static final String BGN = "BGN";

  private final DiscountCalculator calc = new DiscountCalculator();

  @Test
  void calculateReduction_percent_simple() {
    // 10.00 * 25% = 2.50
    Discount discount = Discount.percent(new BigDecimal("25"), WriteOffReason.DISCOUNT, "promo");
    Money result = calc.calculateReduction(discount, money(BGN, "10.00"));
    assertEquals(money(BGN, "2.50"), result);
  }

  @Test
  void calculateReduction_percent_roundsViaMoneyOf() {
    // 10.00 * 33.33% = 3.333... -> Money rounds to 3.33
    Discount discount = Discount.percent(new BigDecimal("33.33"), WriteOffReason.PROMOTION, null);
    Money result = calc.calculateReduction(discount, money(BGN, "10.00"));
    assertEquals(money(BGN, "3.33"), result);
  }

  @Test
  void calculateReduction_percent_normalizationCanReach100() {
    // Discount normalizes first, then validates, so 99.999 -> 100.00 (valid).
    Discount discount = Discount.percent(new BigDecimal("99.999"), WriteOffReason.DISCOUNT, null);
    Money result = calc.calculateReduction(discount, money(BGN, "7.00"));
    assertEquals(money(BGN, "7.00"), result);
  }

  @Test
  void calculateReduction_flatAmount_returnsAmount() {
    Discount discount = Discount.flatAmount(money(BGN, "2.00"), WriteOffReason.DISCOUNT, "x");
    Money result = calc.calculateReduction(discount, money(BGN, "10.00"));
    assertEquals(money(BGN, "2.00"), result);
  }

  @Test
  void calculateReduction_baseAmountZero_rejected() {
    Discount discount = Discount.percent(new BigDecimal("10"), WriteOffReason.DISCOUNT, null);
    Money base = money(BGN, "0.00");
    assertThrows(IllegalArgumentException.class, () -> calc.calculateReduction(discount, base));
  }

  @Test
  void calculateReduction_nullGuards_rejected() {
    Discount discount = Discount.percent(new BigDecimal("10"), WriteOffReason.DISCOUNT, null);
    Money base = money(BGN, "10.00");

    NullPointerException ex1 =
        assertThrows(NullPointerException.class, () -> calc.calculateReduction(null, base));
    assertTrue(ex1.getMessage().contains("discount"));

    NullPointerException ex2 =
        assertThrows(NullPointerException.class, () -> calc.calculateReduction(discount, null));
    assertTrue(ex2.getMessage().contains("baseAmount"));
  }

  @Test
  void calculateReduction_percent100_returnsBaseAmount() {
    Discount discount = Discount.percent(new BigDecimal("100"), WriteOffReason.DISCOUNT, null);
    Money base = money(BGN, "7.35");

    Money result = calc.calculateReduction(discount, base);

    assertEquals(base, result);
  }

  @Test
  void calculateReduction_percent_roundsHalfUp_atHalfCentBoundary() {
    // 10.00 * 0.05% = 0.005 -> HALF_UP => 0.01
    Discount discount = Discount.percent(new BigDecimal("0.05"), WriteOffReason.DISCOUNT, null);
    Money base = money(BGN, "10.00");

    Money result = calc.calculateReduction(discount, base);

    assertEquals(money(BGN, "0.01"), result);
  }

  @Test
  void calculateReduction_flatAmount_currencyMismatch_rejected() {
    Discount discount = Discount.flatAmount(money("EUR", "1.00"), WriteOffReason.DISCOUNT, null);
    Money base = money(BGN, "10.00");
    assertThrows(IllegalArgumentException.class, () -> calc.calculateReduction(discount, base));
  }

  @Test
  void calculateReduction_reductionExceedsBase_rejected_flat() {
    Discount discount =
        Discount.flatAmount(money(BGN, "10.01"), WriteOffReason.ADMIN_ADJUSTMENT, null);
    Money base = money(BGN, "10.00");
    assertThrows(IllegalArgumentException.class, () -> calc.calculateReduction(discount, base));
  }

  @Test
  void toSessionWriteOff_preservesReasonAndNote() {
    Discount discount =
        Discount.percent(new BigDecimal("10"), WriteOffReason.PROMOTION, "  promo  ");
    WriteOff wo = calc.toSessionWriteOff(discount, money(BGN, "20.00"));

    assertEquals(money(BGN, "2.00"), wo.amount());
    assertEquals(WriteOffReason.PROMOTION, wo.reason());
    assertEquals("promo", wo.note());
  }

  @Test
  void toItemWriteOff_preservesScopeReasonAndNote() {
    OrderItemId item = itemId("00000000-0000-0000-0000-000000000001");
    Discount discount = Discount.flatAmount(money(BGN, "1.50"), WriteOffReason.COMPENSATION, "x");

    ItemWriteOff wo = calc.toItemWriteOff(discount, item, 2, money(BGN, "10.00"));

    assertEquals(item, wo.itemId());
    assertEquals(2, wo.quantity());
    assertEquals(money(BGN, "1.50"), wo.amount());
    assertEquals(WriteOffReason.COMPENSATION, wo.reason());
    assertEquals("x", wo.note());
  }

  @Test
  void toItemWriteOff_quantityMustBePositive() {
    OrderItemId item = OrderItemId.of(UUID.randomUUID());
    Discount discount = Discount.percent(new BigDecimal("10"), WriteOffReason.DISCOUNT, null);
    Money base = money(BGN, "10.00");
    assertThrows(
        IllegalArgumentException.class, () -> calc.toItemWriteOff(discount, item, 0, base));
  }

  @Test
  void toItemWriteOff_itemIdNull_rejected() {
    Discount discount = Discount.percent(new BigDecimal("10"), WriteOffReason.DISCOUNT, null);
    Money base = money(BGN, "10.00");
    assertThrows(NullPointerException.class, () -> calc.toItemWriteOff(discount, null, 1, base));
  }

  @Test
  void toItemWriteOff_percent_preservesItemReasonAndNote_andUsesCalculatedReduction() {
    // 10.00 * 12.34% = 1.234 -> Money rounds to 1.23
    OrderItemId item = itemId("00000000-0000-0000-0000-000000000001");
    Discount discount =
        Discount.percent(new BigDecimal("12.34"), WriteOffReason.PROMOTION, "  promo  ");
    Money base = money(BGN, "10.00");

    ItemWriteOff wo = calc.toItemWriteOff(discount, item, 2, base);

    assertEquals(item, wo.itemId());
    assertEquals(2, wo.quantity());
    assertEquals(WriteOffReason.PROMOTION, wo.reason());
    assertEquals("promo", wo.note());

    // Core of A11: amount matches calculator's reduction for the same (discount, base)
    Money expected = calc.calculateReduction(discount, base);
    assertEquals(expected, wo.amount());
    assertEquals(money(BGN, "1.23"), wo.amount()); // sanity check for rounding
  }

  @ParameterizedTest
  @MethodSource("percentRoundingToZeroInputs")
  void calculateReduction_percentRoundingToZero_rejected(String percent, String baseAmount) {
    Discount discount = Discount.percent(new BigDecimal(percent), WriteOffReason.DISCOUNT, null);
    Money base = money(BGN, baseAmount);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> calc.calculateReduction(discount, base));

    assertTrue(ex.getMessage().toLowerCase().contains("strictly greater than zero"));
  }

  private static Stream<Arguments> percentRoundingToZeroInputs() {
    return Stream.of(
        // base 0.01 * 1% = 0.0001 -> Money.of rounds to 0.00 -> rejected
        Arguments.of("1", "0.01"),
        // base 0.01 * 0.01% = 0.000001 -> Money.of rounds to 0.00 -> rejected
        Arguments.of("0.01", "0.01"));
  }
}
