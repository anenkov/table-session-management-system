package com.nenkov.bar.domain.model.money;

import static org.assertj.core.api.Assertions.*;

import com.nenkov.bar.domain.exceptions.IllegalDomainStateException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

  private static final String EUR = "EUR";
  private static final String USD = "USD";

  @Test
  void createsMoneyWithNormalization() {
    BigDecimal rawAmount = new BigDecimal("10.235");

    Money money = Money.of(EUR, rawAmount);

    assertThat(money.amount()).isEqualByComparingTo("10.24");
    assertThat(money.currency()).isEqualTo(EUR);
  }

  @Test
  void zeroCreatesZeroMoney() {
    Money zero = Money.zero(EUR);

    assertThat(zero.amount()).isEqualByComparingTo("0.00");
    assertThat(zero.isZero()).isTrue();
  }

  @Test
  void rejectsNegativeAmount() {
    BigDecimal negativeAmount = new BigDecimal("-0.01");

    assertThatThrownBy(() -> Money.of(EUR, negativeAmount))
        .isInstanceOf(IllegalDomainStateException.class);
  }

  @Test
  void rejectsInvalidCurrency() {
    BigDecimal amount = BigDecimal.ONE;
    String invalidCurrency = "eur";

    assertThatThrownBy(() -> Money.of(invalidCurrency, amount))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void plusAddsAmounts() {
    Money first = Money.of(EUR, new BigDecimal("10.00"));
    Money second = Money.of(EUR, new BigDecimal("2.50"));

    Money result = first.plus(second);

    assertThat(result.amount()).isEqualByComparingTo("12.50");
  }

  @Test
  void minusSubtractsAmounts() {
    Money first = Money.of(EUR, new BigDecimal("10.00"));
    Money second = Money.of(EUR, new BigDecimal("3.25"));

    Money result = first.minus(second);

    assertThat(result.amount()).isEqualByComparingTo("6.75");
  }

  @Test
  void minusRejectsNegativeResult() {
    Money smaller = Money.of(EUR, new BigDecimal("5.00"));
    Money larger = Money.of(EUR, new BigDecimal("6.00"));

    assertThatThrownBy(() -> smaller.minus(larger)).isInstanceOf(IllegalDomainStateException.class);
  }

  @Test
  void timesMultipliesAmount() {
    Money money = Money.of(EUR, new BigDecimal("2.33"));
    int quantity = 3;

    Money result = money.times(quantity);

    assertThat(result.amount()).isEqualByComparingTo("6.99");
  }

  @Test
  void timesRejectsNegativeQuantity() {
    Money money = Money.of(EUR, BigDecimal.ONE);
    int negativeQuantity = -1;

    assertThatThrownBy(() -> money.times(negativeQuantity))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void operationsRequireSameCurrency() {
    Money eur = Money.of(EUR, BigDecimal.ONE);
    Money usd = Money.of(USD, BigDecimal.ONE);

    assertThatThrownBy(() -> eur.plus(usd)).isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> eur.minus(usd)).isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> eur.compareTo(usd)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void equalityIsBasedOnNormalizedAmountAndCurrency() {
    Money first = Money.of(EUR, new BigDecimal("10"));
    Money second = Money.of(EUR, new BigDecimal("10.00"));
    Money third = Money.of(EUR, new BigDecimal("10.000"));

    assertThat(first).isEqualTo(second);
    assertThat(second).isEqualTo(third);
    assertThat(first).hasSameHashCodeAs(second);
  }

  @Test
  void compareToOrdersByAmount() {
    Money smaller = Money.of(EUR, new BigDecimal("1.00"));
    Money larger = Money.of(EUR, new BigDecimal("2.00"));

    assertThat(smaller.compareTo(larger)).isLessThan(0);
    assertThat(larger.compareTo(smaller)).isGreaterThan(0);
  }
}
