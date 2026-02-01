// src/test/java/com/nenkov/bar/domain/service/payment/allocation/ProportionalShareCalculatorTest.java
package com.nenkov.bar.domain.service.payment.allocation;

import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nenkov.bar.domain.model.money.Money;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class ProportionalShareCalculatorTest {

  private static final String BGN = "BGN";
  private static final String EUR = "EUR";

  private final ProportionalShareCalculator calc = new ProportionalShareCalculator();

  @Test
  void shareOfTotal_totalZero_returnsZero() {
    Money result =
        calc.shareOfTotal(BGN, money(BGN, "0.00"), money(BGN, "1.00"), money(BGN, "2.00"));
    assertEquals(money(BGN, "0.00"), result);
  }

  @Test
  void shareOfTotal_partZero_returnsZero() {
    Money result =
        calc.shareOfTotal(BGN, money(BGN, "10.00"), money(BGN, "0.00"), money(BGN, "2.00"));
    assertEquals(money(BGN, "0.00"), result);
  }

  @Test
  void shareOfTotal_simpleRatio() {
    // 10.00 * 1.00 / 4.00 = 2.50
    Money result =
        calc.shareOfTotal(BGN, money(BGN, "10.00"), money(BGN, "1.00"), money(BGN, "4.00"));
    assertEquals(money(BGN, "2.50"), result);
  }

  @Test
  void shareOfTotal_roundsViaMoneyOf() {
    // 10.00 * 1.00 / 3.00 = 3.333... -> Money rounds to 3.33 (HALF_UP)
    Money result =
        calc.shareOfTotal(BGN, money(BGN, "10.00"), money(BGN, "1.00"), money(BGN, "3.00"));
    assertEquals(money(BGN, "3.33"), result);
  }

  @Test
  void shareOfTotal_wholeZero_rejected() {
    Money total = money(BGN, "10.00");
    Money part = money(BGN, "1.00");
    Money whole = money(BGN, "0.00");

    assertThrows(IllegalArgumentException.class, () -> calc.shareOfTotal(BGN, total, part, whole));
  }

  @ParameterizedTest
  @MethodSource("invalidShareInputs")
  void shareOfTotal_invalidInputs_rejected(Money total, Money part, Money whole) {
    assertThrows(IllegalArgumentException.class, () -> calc.shareOfTotal(BGN, total, part, whole));
  }

  @ParameterizedTest
  @MethodSource("currencyMismatchInputs")
  void shareOfTotal_currencyMismatch_rejected(Money total, Money part, Money whole) {
    assertThrows(IllegalArgumentException.class, () -> calc.shareOfTotal(BGN, total, part, whole));
  }

  @Test
  void shareOfTotal_nullGuards() {
    Money total = money(BGN, "10.00");
    Money part = money(BGN, "1.00");
    Money whole = money(BGN, "2.00");

    assertThrows(NullPointerException.class, () -> calc.shareOfTotal(null, total, part, whole));
    assertThrows(NullPointerException.class, () -> calc.shareOfTotal(BGN, null, part, whole));
    assertThrows(NullPointerException.class, () -> calc.shareOfTotal(BGN, total, null, whole));
    assertThrows(NullPointerException.class, () -> calc.shareOfTotal(BGN, total, part, null));
  }

  @Test
  void shareOfTotal_currencyMismatch_messageMentionsCurrencyMismatch() {
    Money total = money(EUR, "10.00"); // mismatch: expected BGN
    Money part = money(BGN, "1.00");
    Money whole = money(BGN, "2.00");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> calc.shareOfTotal(BGN, total, part, whole));

    assertTrue(ex.getMessage().toLowerCase().contains("currency"));
    assertTrue(ex.getMessage().toLowerCase().contains("mismatch"));
  }

  private static Stream<Arguments> currencyMismatchInputs() {
    return Stream.of(
        // mismatch in total
        Arguments.of(money(EUR, "10.00"), money(BGN, "1.00"), money(BGN, "2.00")),
        // mismatch in part
        Arguments.of(money(BGN, "10.00"), money(EUR, "1.00"), money(BGN, "2.00")),
        // mismatch in whole
        Arguments.of(money(BGN, "10.00"), money(BGN, "1.00"), money(EUR, "2.00")));
  }

  private static Stream<Arguments> invalidShareInputs() {
    return Stream.of(
        // the whole must not be zero (when total/part are non-zero)
        Arguments.of(money(BGN, "10.00"), money(BGN, "1.00"), money(BGN, "0.00")));
  }
}
