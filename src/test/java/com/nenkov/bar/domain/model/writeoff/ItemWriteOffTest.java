package com.nenkov.bar.domain.model.writeoff;

import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.*;

import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.model.session.OrderItemId;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ItemWriteOffTest {

  @Test
  void of_minimal_success() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());

    ItemWriteOff wo = ItemWriteOff.of(itemId, 2, money("USD", "3.00"), WriteOffReason.COMPENSATION);

    assertEquals(itemId, wo.itemId());
    assertEquals(2, wo.quantity());
    assertEquals(money("USD", "3.00"), wo.amount());
    assertEquals(WriteOffReason.COMPENSATION, wo.reason());
    assertNull(wo.note());
  }

  @Test
  void of_withNote_success_andTrimmed() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());

    ItemWriteOff wo =
        ItemWriteOff.of(itemId, 1, money("USD", "1.00"), WriteOffReason.ADMIN_ADJUSTMENT, "  x  ");

    assertEquals("x", wo.note());
  }

  @Test
  void itemId_null_rejected() {
    Money money = money("EUR", "1.00");
    NullPointerException ex =
        assertThrows(
            NullPointerException.class,
            () -> ItemWriteOff.of(null, 1, money, WriteOffReason.OTHER));

    assertTrue(ex.getMessage().contains("itemId"));
  }

  @Test
  void amount_null_rejected() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());

    NullPointerException ex =
        assertThrows(
            NullPointerException.class,
            () -> ItemWriteOff.of(itemId, 1, null, WriteOffReason.OTHER));

    assertTrue(ex.getMessage().contains("amount"));
  }

  @Test
  void reason_null_rejected() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money money = money("EUR", "1.00");

    NullPointerException ex =
        assertThrows(NullPointerException.class, () -> ItemWriteOff.of(itemId, 1, money, null));

    assertTrue(ex.getMessage().contains("reason"));
  }

  @Test
  void quantity_mustBePositive() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money money = money("EUR", "2.00");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> ItemWriteOff.of(itemId, 0, money, WriteOffReason.OTHER));

    assertTrue(ex.getMessage().contains("quantity"));
  }

  @Test
  void amount_zero_rejected() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money money = Money.zero("USD");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> ItemWriteOff.of(itemId, 1, money, WriteOffReason.OTHER));

    assertTrue(ex.getMessage().toLowerCase().contains("greater than zero"));
  }

  @ParameterizedTest
  @MethodSource("blankNotes")
  void blank_note_normalizes_toNull(String note) {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    Money money = money("USD", "1.00");

    ItemWriteOff wo = ItemWriteOff.of(itemId, 1, money, WriteOffReason.ADMIN_ADJUSTMENT, note);

    assertNull(wo.note());
  }

  static Stream<String> blankNotes() {
    return Stream.of("", " ", "   \n\t  ");
  }

  @Test
  void note_length_overMax_rejected() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    String tooLong = "a".repeat(201);
    Money money = money("EUR", "1.00");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> ItemWriteOff.of(itemId, 1, money, WriteOffReason.OTHER, tooLong));

    assertTrue(ex.getMessage().contains("at most"));
  }

  @Test
  void value_semantics_equalByValue() {
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());

    ItemWriteOff a = ItemWriteOff.of(itemId, 1, money("USD", "1.00"), WriteOffReason.DISCOUNT, "x");
    ItemWriteOff b =
        ItemWriteOff.of(itemId, 1, money("USD", "1.00"), WriteOffReason.DISCOUNT, " x ");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }
}
