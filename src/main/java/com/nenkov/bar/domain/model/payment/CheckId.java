package com.nenkov.bar.domain.model.payment;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable identifier for a {@link Check}.
 *
 * <p>This is a value object wrapping a UUID to keep the domain explicit and to avoid passing raw
 * UUIDs throughout the model.
 */
public record CheckId(UUID value) {

  /**
   * Creates a {@code CheckId} from a UUID value.
   *
   * @param value non-null UUID
   * @return a new {@code CheckId}
   */
  public static CheckId of(UUID value) {
    return new CheckId(value);
  }

  /**
   * Generates a new random {@code CheckId}.
   *
   * @return a new {@code CheckId}
   */
  public static CheckId random() {
    return new CheckId(UUID.randomUUID());
  }

  public CheckId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
