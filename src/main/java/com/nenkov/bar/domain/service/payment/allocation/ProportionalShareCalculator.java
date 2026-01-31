package com.nenkov.bar.domain.service.payment.allocation;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.money.MoneyPolicy;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Computes proportional shares of a total amount using high-precision intermediate math.
 *
 * <p>Intermediate division uses {@link MoneyPolicy#WORK_CONTEXT}. Final rounding is applied via
 * {@link Money#of(String, BigDecimal)}.
 *
 * <p>Example: share = total × part / whole.
 */
public final class ProportionalShareCalculator {

  /**
   * Returns the proportional share of {@code total} corresponding to {@code part} out of {@code
   * whole}.
   *
   * <p>If {@code total} or {@code part} is zero, the result is zero. {@code whole} must be &gt; 0.
   *
   * @throws IllegalArgumentException on currency mismatch or if {@code whole} is zero
   */
  public Money shareOfTotal(String currency, Money total, Money part, Money whole) {
    Objects.requireNonNull(currency, "currency must not be null");
    Objects.requireNonNull(total, "total must not be null");
    Objects.requireNonNull(part, "part must not be null");
    Objects.requireNonNull(whole, "whole must not be null");

    if (!currency.equals(total.currency())
        || !currency.equals(part.currency())
        || !currency.equals(whole.currency())) {
      throw new IllegalArgumentException("Currency mismatch in proportional share");
    }

    if (whole.isZero()) {
      throw new IllegalArgumentException("whole must be > 0");
    }
    if (total.isZero() || part.isZero()) {
      return Money.zero(currency);
    }

    BigDecimal raw =
        total.amount().multiply(part.amount()).divide(whole.amount(), MoneyPolicy.WORK_CONTEXT);

    return Money.of(currency, raw);
  }
}
