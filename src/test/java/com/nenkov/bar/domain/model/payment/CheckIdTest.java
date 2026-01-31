package com.nenkov.bar.domain.model.payment;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckIdTest {

  @Test
  void of_success() {
    UUID uuid = UUID.randomUUID();
    CheckId id = CheckId.of(uuid);
    assertEquals(uuid, id.value());
  }

  @Test
  void null_rejected() {
    NullPointerException ex = assertThrows(NullPointerException.class, () -> new CheckId(null));
    assertTrue(ex.getMessage().contains("value"));
  }

  @Test
  void random_generatesNonNullUuid() {
    CheckId id = CheckId.random();
    assertNotNull(id.value());
  }
}
