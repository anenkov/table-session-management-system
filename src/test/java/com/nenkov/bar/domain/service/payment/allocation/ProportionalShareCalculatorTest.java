// src/test/java/com/nenkov/bar/domain/service/payment/allocation/ProportionalShareCalculatorTest.java
package com.nenkov.bar.domain.service.payment.allocation;

import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nenkov.bar.domain.model.money.Money;
import org.junit.jupiter.api.Test;

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

  @Test
  void shareOfTotal_currencyMismatch_rejected() {
    Money total = money(EUR, "10.00"); // mismatch: expected BGN
    Money part = money(BGN, "1.00");
    Money whole = money(BGN, "2.00");

    assertThrows(IllegalArgumentException.class, () -> calc.shareOfTotal(BGN, total, part, whole));
  }
}
