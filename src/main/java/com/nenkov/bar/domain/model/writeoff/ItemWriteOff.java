package com.nenkov.bar.domain.model.writeoff;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.Objects;

/**
 * Immutable value object representing a strictly positive reduction scoped to a specific order item
 * and quantity.
 *
 * <p>This is used for item removals / comps / item-specific discounts. Item write-offs are applied
 * before session-level {@link WriteOff}s.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>{@code itemId} is non-null
 *   <li>{@code quantity} is strictly positive
 *   <li>{@code amount} is non-null and strictly greater than zero
 *   <li>{@code reason} is non-null
 *   <li>{@code note} is optional; if present it is trimmed, blank becomes {@code null}, and its
 *       length is limited
 * </ul>
 */
public record ItemWriteOff(
    OrderItemId itemId, int quantity, Money amount, WriteOffReason reason, String note) {

  private static final int NOTE_MAX_LENGTH = 200;

  public ItemWriteOff {
    Objects.requireNonNull(itemId, "itemId must not be null");
    Objects.requireNonNull(amount, "amount must not be null");
    Objects.requireNonNull(reason, "reason must not be null");

    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be > 0");
    }
    if (amount.isZero()) {
      throw new IllegalArgumentException("ItemWriteOff amount must be strictly greater than zero");
    }

    note = normalizeNote(note);
  }

  /**
   * Creates an {@code ItemWriteOff} with the given scope, amount, and reason.
   *
   * @param itemId non-null order item id
   * @param quantity strictly positive quantity this write-off applies to
   * @param amount strictly positive monetary amount to reduce
   * @param reason non-null write-off reason
   * @return a new {@code ItemWriteOff}
   */
  public static ItemWriteOff of(
      OrderItemId itemId, int quantity, Money amount, WriteOffReason reason) {
    return new ItemWriteOff(itemId, quantity, amount, reason, null);
  }

  /**
   * Creates an {@code ItemWriteOff} with the given scope, amount, reason, and note.
   *
   * @param itemId non-null order item id
   * @param quantity strictly positive quantity this write-off applies to
   * @param amount strictly positive monetary amount to reduce
   * @param reason non-null write-off reason
   * @param note optional human-readable note
   * @return a new {@code ItemWriteOff}
   */
  public static ItemWriteOff of(
      OrderItemId itemId, int quantity, Money amount, WriteOffReason reason, String note) {
    return new ItemWriteOff(itemId, quantity, amount, reason, note);
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
      throw new IllegalArgumentException(
          "ItemWriteOff note must be at most " + NOTE_MAX_LENGTH + " characters");
    }

    return trimmed;
  }
}
