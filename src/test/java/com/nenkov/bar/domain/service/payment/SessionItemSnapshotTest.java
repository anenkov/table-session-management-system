// src/test/java/com/nenkov/bar/domain/service/payment/SessionItemSnapshotTest.java
package com.nenkov.bar.domain.service.payment;

import static com.nenkov.bar.testsupport.TestFixtures.itemId;
import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.*;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import org.junit.jupiter.api.Test;

final class SessionItemSnapshotTest {

  private static final String BGN = "BGN";

  @Test
  void creates_successfully() {
    OrderItemId id = itemId("00000000-0000-0000-0000-000000000001");

    SessionItemSnapshot s = new SessionItemSnapshot(id, money(BGN, "2.50"), 3);

    assertEquals(id, s.itemId());
    assertEquals(money(BGN, "2.50"), s.unitPrice());
    assertEquals(3, s.remainingQuantity());
  }

  @Test
  void itemId_null_rejected() {
    Money price = money(BGN, "1.00");

    NullPointerException ex =
        assertThrows(NullPointerException.class, () -> new SessionItemSnapshot(null, price, 1));

    assertTrue(ex.getMessage().contains("itemId"));
  }

  @Test
  void unitPrice_null_rejected() {
    OrderItemId id = itemId("00000000-0000-0000-0000-000000000001");

    NullPointerException ex =
        assertThrows(NullPointerException.class, () -> new SessionItemSnapshot(id, null, 1));

    assertTrue(ex.getMessage().contains("unitPrice"));
  }

  @Test
  void unitPrice_zero_rejected() {
    OrderItemId id = itemId("00000000-0000-0000-0000-000000000001");
    Money price = Money.zero(BGN);
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> new SessionItemSnapshot(id, price, 1));

    assertTrue(ex.getMessage().toLowerCase().contains("unitprice"));
  }

  @Test
  void remainingQuantity_negative_rejected() {
    OrderItemId id = itemId("00000000-0000-0000-0000-000000000001");
    Money price = money(BGN, "1.00");
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> new SessionItemSnapshot(id, price, -1));

    assertTrue(ex.getMessage().toLowerCase().contains("remainingquantity"));
  }

  @Test
  void grossAmountFor_multipliesUnitPrice() {
    OrderItemId id = itemId("00000000-0000-0000-0000-000000000001");
    SessionItemSnapshot s = new SessionItemSnapshot(id, money(BGN, "2.50"), 10);

    assertEquals(money(BGN, "0.00"), s.grossAmountFor(0));
    assertEquals(money(BGN, "2.50"), s.grossAmountFor(1));
    assertEquals(money(BGN, "7.50"), s.grossAmountFor(3));
  }

  @Test
  void grossAmountFor_negativeQuantity_rejected() {
    OrderItemId id = itemId("00000000-0000-0000-0000-000000000001");
    SessionItemSnapshot s = new SessionItemSnapshot(id, money(BGN, "1.00"), 10);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> s.grossAmountFor(-1));

    assertTrue(ex.getMessage().toLowerCase().contains("quantity"));
  }
}
