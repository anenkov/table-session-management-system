package com.nenkov.bar.application.ordering.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

final class AddOrderItemsInputTest {

  @Test
  void happyPath() {
    AddOrderItemsInput.RequestedItem item = new AddOrderItemsInput.RequestedItem("P-1", 2);
    List<AddOrderItemsInput.RequestedItem> items = List.of(item);

    AddOrderItemsInput input = new AddOrderItemsInput("S-1", items);

    assertThat(input.sessionId()).isEqualTo("S-1");
    assertThat(input.items()).hasSize(1);
    assertThat(input.items().getFirst().productId()).isEqualTo("P-1");
    assertThat(input.items().getFirst().quantity()).isEqualTo(2);
  }

  @Test
  void nullSessionId_throwsNpe() {
    AddOrderItemsInput.RequestedItem item = new AddOrderItemsInput.RequestedItem("P-1", 1);
    List<AddOrderItemsInput.RequestedItem> items = List.of(item);

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new AddOrderItemsInput(null, items));

    assertThat(thrown.getMessage()).contains("sessionId must not be null");
  }

  @Test
  void blankSessionId_throwsIae() {
    AddOrderItemsInput.RequestedItem item = new AddOrderItemsInput.RequestedItem("P-1", 1);
    List<AddOrderItemsInput.RequestedItem> items = List.of(item);

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> new AddOrderItemsInput("   ", items));

    assertThat(thrown.getMessage()).contains("sessionId must not be blank");
  }

  @Test
  void nullItems_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new AddOrderItemsInput("S-1", null));

    assertThat(thrown.getMessage()).contains("items must not be null");
  }

  @Test
  void emptyItems_throwsIae() {
    List<AddOrderItemsInput.RequestedItem> items = List.of();

    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> new AddOrderItemsInput("S-1", items));

    assertThat(thrown.getMessage()).contains("items must not be empty");
  }

  // -------------------------
  // RequestedItem
  // -------------------------

  @Test
  void requestedItem_happyPath() {
    AddOrderItemsInput.RequestedItem item = new AddOrderItemsInput.RequestedItem("P-1", 3);

    assertThat(item.productId()).isEqualTo("P-1");
    assertThat(item.quantity()).isEqualTo(3);
  }

  @Test
  void requestedItem_nullProductId_throwsNpe() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class, () -> new AddOrderItemsInput.RequestedItem(null, 1));

    assertThat(thrown.getMessage()).contains("productId must not be null");
  }

  @Test
  void requestedItem_blankProductId_throwsIae() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> new AddOrderItemsInput.RequestedItem("   ", 1));

    assertThat(thrown.getMessage()).contains("productId must not be blank");
  }

  @Test
  void requestedItem_quantityZero_throwsIae() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> new AddOrderItemsInput.RequestedItem("P-1", 0));

    assertThat(thrown.getMessage()).contains("quantity must be > 0");
  }

  @Test
  void requestedItem_quantityNegative_throwsIae() {
    Throwable thrown =
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class, () -> new AddOrderItemsInput.RequestedItem("P-1", -1));

    assertThat(thrown.getMessage()).contains("quantity must be > 0");
  }
}
