package com.nenkov.bar.domain.model.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OrderItemDraftTest {

  @Test
  void constructor_nullProductId_throwsNpe() {
    Throwable thrown = assertThrows(NullPointerException.class, () -> new OrderItemDraft(null, 1));

    assertThat(thrown.getMessage()).contains("productId must not be null");
  }

  @Test
  void constructor_blankProductId_throwsIllegalArgumentException() {
    String productId = "   ";

    Throwable thrown =
        assertThrows(IllegalArgumentException.class, () -> new OrderItemDraft(productId, 1));

    assertThat(thrown.getMessage()).contains("productId must not be blank");
  }

  @Test
  void constructor_nonPositiveQuantity_throwsIllegalArgumentException() {
    Throwable thrown =
        assertThrows(IllegalArgumentException.class, () -> new OrderItemDraft("P-1", 0));

    assertThat(thrown.getMessage()).contains("quantity must be positive");
  }
}
