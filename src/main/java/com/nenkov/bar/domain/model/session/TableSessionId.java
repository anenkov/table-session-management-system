package com.nenkov.bar.domain.model.session;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Strongly-typed identifier for {@link TableSession}.
 *
 * <p>Represents identity only. No persistence, parsing, or formatting concerns belong here.
 */
public final class TableSessionId implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  private final String value;

  private TableSessionId(String value) {
    this.value = value;
  }

  /**
   * Creates a {@link TableSessionId}.
   *
   * @param value id string (non-null, non-blank)
   * @return id instance
   */
  public static TableSessionId of(String value) {
    Objects.requireNonNull(value, "value");
    if (value.isBlank()) {
      throw new IllegalArgumentException("value must not be blank");
    }
    return new TableSessionId(value);
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TableSessionId other)) {
      return false;
    }
    return value.equals(other.value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value;
  }
}
