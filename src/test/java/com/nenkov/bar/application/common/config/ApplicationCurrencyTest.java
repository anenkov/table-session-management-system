package com.nenkov.bar.application.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class ApplicationCurrencyTest {

  @Test
  void validCode_isAccepted() {
    ApplicationCurrency currency = new ApplicationCurrency("EUR");
    assertThat(currency.code()).isEqualTo("EUR");
  }

  @Test
  void nullCode_throwsNpe() {
    assertThrows(NullPointerException.class, () -> new ApplicationCurrency(null));
  }

  @Test
  void blankCode_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> new ApplicationCurrency(" "));
  }

  @Test
  void lowercaseCode_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> new ApplicationCurrency("eur"));
  }

  @Test
  void invalidLength_throwsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> new ApplicationCurrency("EURO"));
  }
}
