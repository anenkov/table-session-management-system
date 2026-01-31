package com.nenkov.bar.domain.service.discount;

import com.nenkov.bar.domain.model.discount.Discount;
import com.nenkov.bar.domain.model.discount.DiscountType;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.money.MoneyPolicy;
import com.nenkov.bar.domain.model.session.OrderItemId;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOff;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Resolves discount intents (flat or percent) into concrete {@link Money} write-off amounts.
 *
 * <p>All intermediate math uses {@link MoneyPolicy#WORK_CONTEXT}. Final rounding/normalization is
 * performed only via {@link com.nenkov.bar.domain.model.money.Money#of(String,
 * java.math.BigDecimal)}.
 *
 * <p>This service intentionally contains no VAT/tax logic and does not mutate domain state.
 */
public final class DiscountCalculator {

  private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

  /**
   * Calculates the concrete monetary reduction for a discount over a given base amount.
   *
   * @param discount discount intent
   * @param baseAmount monetary base amount the discount applies to
   * @return strictly positive reduction amount
   * @throws IllegalArgumentException if the reduction is zero or exceeds the base amount
   */
  public Money calculateReduction(Discount discount, Money baseAmount) {
    Objects.requireNonNull(discount, "discount must not be null");
    Objects.requireNonNull(baseAmount, "baseAmount must not be null");

    if (baseAmount.isZero()) {
      throw new IllegalArgumentException("baseAmount must be greater than zero");
    }

    Money reduction =
        switch (discount.type()) {
          case PERCENT -> percentReduction(discount, baseAmount);
          case FLAT_AMOUNT -> flatReduction(discount, baseAmount);
        };

    // Strictly positive
    if (reduction.isZero()) {
      throw new IllegalArgumentException("Calculated reduction must be strictly greater than zero");
    }

    // Must not exceed base
    if (reduction.compareTo(baseAmount) > 0) {
      throw new IllegalArgumentException("Calculated reduction must not exceed baseAmount");
    }

    return reduction;
  }

  /**
   * Converts a discount into a session-level {@link WriteOff}.
   *
   * @param discount discount intent
   * @param baseTotal base session total the discount applies to
   * @return a session-level write-off
   */
  public WriteOff toSessionWriteOff(Discount discount, Money baseTotal) {
    Money reduction = calculateReduction(discount, baseTotal);
    return WriteOff.of(reduction, discount.reason(), discount.note());
  }

  /**
   * Converts a discount into an item-scoped {@link ItemWriteOff}.
   *
   * @param discount discount intent
   * @param itemId target item id
   * @param quantity strictly positive quantity in scope
   * @param baseItemTotal base total for the scoped quantity (unit × quantity)
   * @return an item-scoped write-off
   */
  public ItemWriteOff toItemWriteOff(
      Discount discount, OrderItemId itemId, int quantity, Money baseItemTotal) {
    Objects.requireNonNull(itemId, "itemId must not be null");
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be > 0");
    }

    Money reduction = calculateReduction(discount, baseItemTotal);
    return ItemWriteOff.of(itemId, quantity, reduction, discount.reason(), discount.note());
  }

  /**
   * Computes the monetary reduction for a percent-based discount.
   *
   * <p>Intermediate calculations use {@link MoneyPolicy#WORK_CONTEXT} to avoid premature rounding.
   * Final rounding/normalization is applied only via {@link Money#of(String, BigDecimal)}.
   *
   * <p>The returned value is a positive {@link Money} amount. Validity constraints (non-zero and
   * not exceeding {@code baseAmount}) are enforced by {@link #calculateReduction}.
   *
   * @throws IllegalArgumentException if {@code discount.type()} is not {@link DiscountType#PERCENT}
   */
  private static Money percentReduction(Discount discount, Money baseAmount) {
    if (discount.type() != DiscountType.PERCENT) {
      throw new IllegalArgumentException("Expected PERCENT discount");
    }

    BigDecimal base = baseAmount.amount();
    BigDecimal percent = discount.percent(); // scale=2, HALF_UP

    BigDecimal raw = base.multiply(percent).divide(ONE_HUNDRED, MoneyPolicy.WORK_CONTEXT);

    return Money.of(baseAmount.currency(), raw);
  }

  private static Money flatReduction(Discount discount, Money baseAmount) {
    if (discount.type() != DiscountType.FLAT_AMOUNT) {
      throw new IllegalArgumentException("Expected FLAT_AMOUNT discount");
    }

    Money reduction = discount.amount();
    // Ensure the same currency (Money.compareTo enforces this too, but keep error explicit)
    if (!reduction.currency().equals(baseAmount.currency())) {
      throw new IllegalArgumentException(
          "Currency mismatch between discount amount and baseAmount");
    }
    return reduction;
  }
}
