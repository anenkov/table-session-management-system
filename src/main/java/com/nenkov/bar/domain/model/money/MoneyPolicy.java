package com.nenkov.bar.domain.model.money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Shared numeric policies for {@link Money} calculations.
 *
 * <p>Domain rule: intermediate math uses {@link #WORK_CONTEXT} to avoid premature rounding. Final
 * rounding/normalization must happen only through {@link Money#of(String, BigDecimal)}.
 *
 * <p>This class intentionally contains no business logic (allocation, discounts, caps).
 */
public final class MoneyPolicy {

  /** Working precision for intermediate calculations across the domain. */
  public static final MathContext WORK_CONTEXT = new MathContext(10, RoundingMode.HALF_UP);

  /** One cent as a raw constant; wrap via {@link Money#of(String, BigDecimal)}. */
  public static final BigDecimal ONE_CENT = new BigDecimal("0.01");

  private MoneyPolicy() {}
}
