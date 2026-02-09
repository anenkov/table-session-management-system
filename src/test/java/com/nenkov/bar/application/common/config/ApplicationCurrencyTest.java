package com.nenkov.bar.application.common.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

final class ApplicationCurrencyTest {

  @ParameterizedTest(name = "input=\"{0}\" -> normalized=\"{1}\"")
  @CsvSource({"EUR, EUR", "eur, EUR", "'  eur  ', EUR"})
  void validCodes_areNormalized(String input, String expected) {
    ApplicationCurrency currency = new ApplicationCurrency(input);
    assertEquals(expected, currency.code());
  }

  @Test
  void blankCode_throwsIllegalArgumentException() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> new ApplicationCurrency("   "));
    assertEquals("code must not be blank", ex.getMessage());
  }

  @Test
  void invalidFormat_throwsIllegalArgumentException() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> new ApplicationCurrency("EURO"));
    assertEquals("code must be a valid ISO-4217 currency code (e.g., EUR)", ex.getMessage());
  }

  @Test
  void nullCode_throwsNullPointerException() {
    assertThrows(NullPointerException.class, () -> new ApplicationCurrency(null));
  }
}
