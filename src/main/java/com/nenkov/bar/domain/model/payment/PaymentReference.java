package com.nenkov.bar.domain.model.payment;

import java.util.Objects;

/**
 * Reference returned by an external payment provider.
 *
 * <p>This value object normalizes the reference to prevent meaningless values (blank strings) and
 * to keep the domain explicit.
 */
public record PaymentReference(String value) {

  private static final int MAX_LENGTH = 100;

  /**
   * Creates a {@code PaymentReference} from a provider reference.
   *
   * @param value provider reference; must not be null/blank; will be trimmed
   * @return a new {@code PaymentReference}
   */
  public static PaymentReference of(String value) {
    return new PaymentReference(value);
  }

  public PaymentReference {
    Objects.requireNonNull(value, "value must not be null");
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("value must not be blank");
    }
    if (trimmed.length() > MAX_LENGTH) {
      throw new IllegalArgumentException("value must be at most " + MAX_LENGTH + " characters");
    }
    value = trimmed;
  }
}
