package com.nenkov.bar.domain.model.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class OrderItemIdTest {

  @Test
  void of_success() {
    UUID uuid = UUID.randomUUID();
    OrderItemId id = OrderItemId.of(uuid);

    assertEquals(uuid, id.value());
  }

  @Test
  void null_rejected() {
    NullPointerException ex = assertThrows(NullPointerException.class, () -> new OrderItemId(null));
    assertTrue(ex.getMessage().contains("value"));
  }

  @Test
  void random_generatesNonNullUuid() {
    OrderItemId id = OrderItemId.random();
    assertNotNull(id.value());
  }
}
