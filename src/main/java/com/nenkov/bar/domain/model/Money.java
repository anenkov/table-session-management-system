package com.nenkov.bar.domain.model;

import com.nenkov.bar.domain.exceptions.IllegalDomainStateException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Immutable value object representing a monetary amount in a single currency.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>Currency is a non-null, uppercase ISO-4217 code (length 3)
 *   <li>Amount is non-null, normalized to scale = 2 using HALF_UP rounding
 *   <li>Amount is never negative
 * </ul>
 *
 * <p>This class contains no business policies such as VAT, discounts, or currency conversion.
 */
public final class Money implements Comparable<Money> {

  private static final int SCALE = 2;
  private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

  private final BigDecimal amount;
  private final String currency;

  private Money(String currency, BigDecimal amount) {
    this.currency = validateCurrency(currency);
    this.amount = normalizeAmount(amount);
  }

  /**
   * Creates a {@code Money} instance for the given currency and amount. The amount is normalized
   * and must not be negative.
   */
  public static Money of(String currency, BigDecimal amount) {
    Objects.requireNonNull(amount, "amount must not be null");
    return new Money(currency, amount);
  }

  /** Creates a zero-valued {@code Money} instance for the given currency. */
  public static Money zero(String currency) {
    return new Money(currency, BigDecimal.ZERO);
  }

  /** Returns the normalized monetary amount (scale = 2). */
  public BigDecimal amount() {
    return amount;
  }

  /** Returns the ISO-4217 currency code. */
  public String currency() {
    return currency;
  }

  /** Adds another {@code Money} to this one. Both values must have the same currency. */
  public Money plus(Money other) {
    requireSameCurrency(other);
    return new Money(currency, amount.add(other.amount));
  }

  /** Subtracts another {@code Money} from this one. The result must not be negative. */
  public Money minus(Money other) {
    requireSameCurrency(other);
    BigDecimal result = amount.subtract(other.amount);
    if (result.signum() < 0) {
      throw new IllegalDomainStateException("Money subtraction would result in a negative amount");
    }
    return new Money(currency, result);
  }

  /** Multiplies this {@code Money} by the given quantity. Quantity must be zero or positive. */
  public Money times(int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("quantity must be >= 0");
    }
    return new Money(currency, amount.multiply(BigDecimal.valueOf(quantity)));
  }

  /**
   * Compares this {@code Money} with another one. Comparison is only valid for the same currency.
   */
  @Override
  public int compareTo(@NotNull Money other) {
    requireSameCurrency(other);
    return amount.compareTo(other.amount);
  }

  /** Returns {@code true} if this amount is zero. */
  public boolean isZero() {
    return amount.signum() == 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Money other)) return false;
    return amount.equals(other.amount) && currency.equals(other.currency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount, currency);
  }

  @Override
  public String toString() {
    return amount + " " + currency;
  }

  private static String validateCurrency(String currency) {
    Objects.requireNonNull(currency, "currency must not be null");
    if (currency.length() != 3 || !currency.equals(currency.toUpperCase())) {
      throw new IllegalArgumentException("Currency must be an uppercase ISO-4217 code");
    }
    return currency;
  }

  private static BigDecimal normalizeAmount(BigDecimal amount) {
    BigDecimal normalized = amount.setScale(SCALE, ROUNDING);
    if (normalized.signum() < 0) {
      throw new IllegalDomainStateException("Money amount cannot be negative");
    }
    return normalized;
  }

  private void requireSameCurrency(Money other) {
    Objects.requireNonNull(other, "other money must not be null");
    if (!currency.equals(other.currency)) {
      throw new IllegalArgumentException("Currency mismatch");
    }
  }
}
