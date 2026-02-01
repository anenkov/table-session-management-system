package com.nenkov.bar.testsupport;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.math.BigDecimal;
import java.util.UUID;

public final class TestFixtures {

  private TestFixtures() {}

  public static Money money(String currency, String amount) {
    return Money.of(currency, new BigDecimal(amount));
  }

  public static OrderItemId itemId(String uuid) {
    return OrderItemId.of(UUID.fromString(uuid));
  }
}
