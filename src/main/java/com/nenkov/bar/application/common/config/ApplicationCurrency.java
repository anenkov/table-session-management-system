package com.nenkov.bar.application.common.config;

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
    if (code.isBlank()) {
      throw new IllegalArgumentException("code must not be blank");
    }
    if (code.length() != 3 || !code.equals(code.toUpperCase())) {
      throw new IllegalArgumentException("code must be an uppercase ISO-4217 code");
    }
  }
}
