package com.nenkov.bar.domain.model.session;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable identifier for an order item within a {@code TableSession}.
 *
 * <p>This is a value object wrapping a UUID. It exists to keep the domain model explicit and to
 * avoid passing raw UUIDs throughout the codebase.
 */
public record OrderItemId(UUID value) {

  /**
   * Creates an {@code OrderItemId} from a UUID value.
   *
   * @param value non-null UUID
   * @return a new {@code OrderItemId}
   */
  public static OrderItemId of(UUID value) {
    return new OrderItemId(value);
  }

  /**
   * Generates a new random {@code OrderItemId}.
   *
   * @return a new {@code OrderItemId}
   */
  public static OrderItemId random() {
    return new OrderItemId(UUID.randomUUID());
  }

  public OrderItemId {
    Objects.requireNonNull(value, "value must not be null");
  }
}
