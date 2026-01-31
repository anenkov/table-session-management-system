package com.nenkov.bar.domain.model.discount;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.writeoff.WriteOffReason;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable value object representing discount intent.
 *
 * <p>A {@code Discount} expresses <em>how</em> a reduction should be calculated, but it does not
 * perform calculations itself. Domain services convert a {@code Discount} into concrete {@code
 * Money} write-offs.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>Type is non-null
 *   <li>Reason is non-null
 *   <li>Exactly one of percent/amount is set according to the type
 *   <li>Percent is strictly positive and at most 100, normalized to scale = 2 (HALF_UP)
 *   <li>Flat amount is strictly positive ({@code Money} must be &gt; 0)
 *   <li>Note is optional; if present it is trimmed, blank becomes {@code null}, and its length is
 *       limited
 * </ul>
 */
public final class Discount {

  private static final int NOTE_MAX_LENGTH = 200;

  private static final int PERCENT_SCALE = 2;
  private static final RoundingMode PERCENT_ROUNDING = RoundingMode.HALF_UP;

  private final DiscountType type;
  private final BigDecimal percent; // normalized if type == PERCENT
  private final Money amount; // strictly positive if type == FLAT_AMOUNT
  private final WriteOffReason reason;
  private final String note;

  private Discount(
      DiscountType type, BigDecimal percent, Money amount, WriteOffReason reason, String note) {
    this.type = Objects.requireNonNull(type, "type must not be null");
    this.reason = Objects.requireNonNull(reason, "reason must not be null");
    this.note = normalizeNote(note);

    DiscountParts parts = partsFor(type, percent, amount);
    this.percent = parts.percent();
    this.amount = parts.amount();
  }

  private static DiscountParts partsFor(DiscountType type, BigDecimal percent, Money amount) {
    // Intentionally exhaustive enum switch: adding a new DiscountType must fail compilation.
    @SuppressWarnings("java:S1301") // Sonar: "switch could be if/else" — we want exhaustiveness
    DiscountParts parts =
        switch (type) {
          case PERCENT -> new DiscountParts(normalizePercent(percent), null);
          case FLAT_AMOUNT -> new DiscountParts(null, requirePositiveAmount(amount));
        };
    return parts;
  }

  /**
   * Creates a percentage discount.
   *
   * @param percent strictly positive percentage value in range (0, 100]
   * @param reason non-null reason
   * @return a new {@code Discount} of type {@link DiscountType#PERCENT}
   */
  public static Discount percent(BigDecimal percent, WriteOffReason reason) {
    return new Discount(DiscountType.PERCENT, percent, null, reason, null);
  }

  /**
   * Creates a percentage discount with an optional note.
   *
   * @param percent strictly positive percentage value in range (0, 100]
   * @param reason non-null reason
   * @param note optional human-readable note
   * @return a new {@code Discount} of type {@link DiscountType#PERCENT}
   */
  public static Discount percent(BigDecimal percent, WriteOffReason reason, String note) {
    return new Discount(DiscountType.PERCENT, percent, null, reason, note);
  }

  /**
   * Creates a flat-amount discount.
   *
   * @param amount strictly positive monetary amount to reduce
   * @param reason non-null reason
   * @return a new {@code Discount} of type {@link DiscountType#FLAT_AMOUNT}
   */
  public static Discount flatAmount(Money amount, WriteOffReason reason) {
    return new Discount(DiscountType.FLAT_AMOUNT, null, amount, reason, null);
  }

  /**
   * Creates a flat-amount discount with an optional note.
   *
   * @param amount strictly positive monetary amount to reduce
   * @param reason non-null reason
   * @param note optional human-readable note
   * @return a new {@code Discount} of type {@link DiscountType#FLAT_AMOUNT}
   */
  public static Discount flatAmount(Money amount, WriteOffReason reason, String note) {
    return new Discount(DiscountType.FLAT_AMOUNT, null, amount, reason, note);
  }

  /** Normalizes percent; enforces bounds and scale */
  private static BigDecimal normalizePercent(BigDecimal percent) {
    Objects.requireNonNull(percent, "percent must not be null");
    BigDecimal normalized = percent.setScale(PERCENT_SCALE, PERCENT_ROUNDING);

    if (normalized.signum() <= 0) {
      throw new IllegalArgumentException("percent must be > 0");
    }
    if (normalized.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new IllegalArgumentException("percent must be <= 100");
    }
    return normalized;
  }

  private static Money requirePositiveAmount(Money amount) {
    Objects.requireNonNull(amount, "amount must not be null");
    if (amount.isZero()) {
      throw new IllegalArgumentException("amount must be strictly greater than zero");
    }
    return amount;
  }

  private static String normalizeNote(String note) {
    if (note == null) {
      return null;
    }

    String trimmed = note.trim();
    if (trimmed.isEmpty()) {
      return null;
    }

    if (trimmed.length() > NOTE_MAX_LENGTH) {
      throw new IllegalArgumentException("note must be at most " + NOTE_MAX_LENGTH + " characters");
    }

    return trimmed;
  }

  /** Returns the discount type. */
  public DiscountType type() {
    return type;
  }

  /**
   * Returns the normalized percentage value (scale = 2) if this is a percent discount.
   *
   * @throws IllegalStateException if this discount is not of type {@link DiscountType#PERCENT}
   */
  public BigDecimal percent() {
    if (type != DiscountType.PERCENT) {
      throw new IllegalStateException("percent is only available for PERCENT discounts");
    }
    return percent;
  }

  /**
   * Returns the flat monetary amount if this is a flat-amount discount.
   *
   * @throws IllegalStateException if this discount is not of type {@link DiscountType#FLAT_AMOUNT}
   */
  public Money amount() {
    if (type != DiscountType.FLAT_AMOUNT) {
      throw new IllegalStateException("amount is only available for FLAT_AMOUNT discounts");
    }
    return amount;
  }

  /** Returns the discount reason. */
  public WriteOffReason reason() {
    return reason;
  }

  /** Returns the optional note (normalized). */
  public String note() {
    return note;
  }

  /** Returns {@code true} if this discount is percentage-based. */
  public boolean isPercent() {
    return type == DiscountType.PERCENT;
  }

  /** Returns {@code true} if this discount is flat-amount based. */
  public boolean isFlatAmount() {
    return type == DiscountType.FLAT_AMOUNT;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Discount other)) {
      return false;
    }
    return type == other.type
        && Objects.equals(percent, other.percent)
        && Objects.equals(amount, other.amount)
        && reason == other.reason
        && Objects.equals(note, other.note);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, percent, amount, reason, note);
  }

  @Override
  public String toString() {
    return "Discount{"
        + "type="
        + type
        + ", percent="
        + percent
        + ", amount="
        + amount
        + ", reason="
        + reason
        + ", note="
        + note
        + '}';
  }

  private record DiscountParts(BigDecimal percent, Money amount) {}
}
