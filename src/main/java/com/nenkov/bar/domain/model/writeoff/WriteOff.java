package com.nenkov.bar.domain.model.writeoff;

import com.nenkov.bar.domain.model.money.Money;
import java.util.Objects;

/**
 * Immutable value object representing a strictly positive adjustment that reduces a monetary total.
 *
 * <p>A {@code WriteOff} is <strong>not a payment</strong>. It represents an intentional reduction
 * such as a discount, compensation, or administrative correction.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>Amount is non-null and strictly greater than zero
 *   <li>Reason is non-null
 *   <li>Note is optional; if present it is trimmed, blank becomes {@code null}, and its length is
 *       limited
 * </ul>
 *
 * <p>Currency compatibility is enforced at the aggregate boundary where the write-off is applied.
 */
public record WriteOff(Money amount, WriteOffReason reason, String note) {

  private static final int NOTE_MAX_LENGTH = 200;

  public WriteOff {
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(reason, "reason must not be null");

    if (amount.isZero()) {
      throw new IllegalArgumentException("WriteOff amount must be strictly greater than zero");
    }

    note = normalizeNote(note);
  }

  /**
   * Creates a {@code WriteOff} with the given amount and reason.
   *
   * @param amount strictly positive monetary amount
   * @param reason non-null write-off reason
   * @return a new {@code WriteOff}
   */
  public static WriteOff of(Money amount, WriteOffReason reason) {
    return new WriteOff(amount, reason, null);
  }

  /**
   * Creates a {@code WriteOff} with the given amount, reason, and note.
   *
   * @param amount strictly positive monetary amount
   * @param reason non-null write-off reason
   * @param note optional human-readable note
   * @return a new {@code WriteOff}
   */
  public static WriteOff of(Money amount, WriteOffReason reason, String note) {
    return new WriteOff(amount, reason, note);
  }

  /**
   * Normalizes the optional note.
   *
   * <p>Trims whitespace, converts blank strings to {@code null}, and enforces a maximum length.
   */
  private static String normalizeNote(String note) {
    if (note == null) {
      return null;
    }

    String trimmed = note.trim();
    if (trimmed.isEmpty()) {
      return null;
    }

    if (trimmed.length() > NOTE_MAX_LENGTH) {
      throw new IllegalArgumentException(
          "WriteOff note must be at most " + NOTE_MAX_LENGTH + " characters");
    }

    return trimmed;
  }
}
