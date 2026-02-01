package com.nenkov.bar.domain.model.writeoff;

import static com.nenkov.bar.testsupport.TestFixtures.money;
import static org.junit.jupiter.api.Assertions.*;

import com.nenkov.bar.domain.model.money.Money;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class WriteOffTest {

  @Test
  void of_minimal_success() {
    WriteOff wo = WriteOff.of(money("USD", "1.00"), WriteOffReason.DISCOUNT);

    assertEquals(money("USD", "1.00"), wo.amount());
    assertEquals(WriteOffReason.DISCOUNT, wo.reason());
    assertNull(wo.note());
  }

  @Test
  void of_withNote_success_andTrimmed() {
    WriteOff wo =
        WriteOff.of(money("USD", "1.00"), WriteOffReason.ADMIN_ADJUSTMENT, "  manual fix  ");
    assertEquals("manual fix", wo.note());
  }

  @Test
  void amount_null_rejected() {
    NullPointerException ex =
        assertThrows(NullPointerException.class, () -> WriteOff.of(null, WriteOffReason.OTHER));
    assertTrue(ex.getMessage().contains("amount"));
  }

  @Test
  void reason_null_rejected() {
    Money money = money("EUR", "2.00");
    NullPointerException ex =
        assertThrows(NullPointerException.class, () -> WriteOff.of(money, null));
    assertTrue(ex.getMessage().contains("reason"));
  }

  @Test
  void amount_zero_rejected() {
    Money money = Money.zero("USD");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> WriteOff.of(money, WriteOffReason.OTHER));

    assertTrue(ex.getMessage().toLowerCase().contains("greater than zero"));
  }

  @ParameterizedTest
  @MethodSource("blankNotes")
  void blank_note_normalizes_toNull(String note) {
    WriteOff wo = WriteOff.of(money("USD", "1.00"), WriteOffReason.ADMIN_ADJUSTMENT, note);
    assertNull(wo.note());
  }

  static Stream<String> blankNotes() {
    return Stream.of("", " ", "   \n\t  ");
  }

  @Test
  void note_length_overMax_rejected() {
    String tooLong = "a".repeat(201);
    Money money = money("EUR", "2.00");
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> WriteOff.of(money, WriteOffReason.OTHER, tooLong));

    assertTrue(ex.getMessage().contains("at most"));
  }

  @Test
  void value_semantics_equalByValue() {
    WriteOff a = WriteOff.of(money("USD", "1.00"), WriteOffReason.DISCOUNT, "x");
    WriteOff b = WriteOff.of(money("USD", "1.00"), WriteOffReason.DISCOUNT, " x ");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }
}
