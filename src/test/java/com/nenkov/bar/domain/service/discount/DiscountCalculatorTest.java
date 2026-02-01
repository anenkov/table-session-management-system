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
import org.junit.jupiter.api.Test;

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
}
