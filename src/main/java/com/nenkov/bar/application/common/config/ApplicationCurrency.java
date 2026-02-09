package com.nenkov.bar.application.common.config;

import java.util.Locale;
import java.util.Objects;

/**
 * Application-level configuration value representing the single supported currency.
 *
 * <p>The domain stays currency-agnostic and receives the configured currency from the application
 * layer.
 */
public record ApplicationCurrency(String code) {

  /**
   * Application-level configuration value representing the single supported currency.
   *
   * <p>The domain stays currency-agnostic and receives the configured currency from the application
   * layer.
   */
  public ApplicationCurrency {
    Objects.requireNonNull(code, "code must not be null");

    String normalized = code.trim().toUpperCase(Locale.ROOT);
    if (normalized.isBlank()) {
      throw new IllegalArgumentException("code must not be blank");
    }
    if (!normalized.matches("[A-Z]{3}")) {
      throw new IllegalArgumentException("code must be a valid ISO-4217 currency code (e.g., EUR)");
    }

    code = normalized;
  }
}
