package com.nenkov.bar.domain.model.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class TableSessionAddOrderItemsTest {

  @Test
  void adds_order_items_and_returns_created_ids() {
    TableSession session = openSession();

    List<OrderItemDraft> drafts =
        List.of(new OrderItemDraft("beer", 2), new OrderItemDraft("burger", 1));

    OrderItemsAdded result = session.addOrderItems(drafts);

    TableSession updated = result.session();

    assertThat(updated.orderItems()).hasSize(2);
    assertThat(result.createdOrderItemIds()).hasSize(2);

    OrderItem first = updated.orderItems().getFirst();
    assertThat(first.status()).isEqualTo(OrderItemStatus.ACCEPTED);
  }

  @Test
  void rejects_empty_drafts() {
    TableSession session = openSession();

    List<OrderItemDraft> drafts = List.of();

    assertThrows(IllegalArgumentException.class, () -> session.addOrderItems(drafts));
  }

  private static TableSession openSession() {
    return new TableSession(
        TableSessionId.of("session-1"),
        "EUR",
        TableSessionContents.empty(),
        TableSessionStatus.OPEN,
        null);
  }
}
