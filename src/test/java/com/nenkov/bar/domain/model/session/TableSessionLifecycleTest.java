package com.nenkov.bar.domain.model.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nenkov.bar.domain.exceptions.IllegalDomainStateException;
import com.nenkov.bar.domain.model.money.Money;
import com.nenkov.bar.domain.service.payment.SessionItemSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class TableSessionLifecycleTest {

  @Test
  void constructor_open_requiresClosedAtNull() {
    TableSessionId id = TableSessionId.of("S-1");
    Instant closedAt = Instant.parse("2026-01-01T00:00:00Z");
    TableSessionContents contents = TableSessionContents.empty();

    Throwable thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new TableSession(id, "EUR", contents, TableSessionStatus.OPEN, closedAt));

    assertThat(thrown.getMessage()).contains("closedAt must be null when status is OPEN");
  }

  @Test
  void constructor_closed_requiresClosedAtNonNull() {
    TableSessionId id = TableSessionId.of("S-1");
    TableSessionContents contents = TableSessionContents.empty();

    Throwable thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new TableSession(id, "EUR", contents, TableSessionStatus.CLOSED, null));

    assertThat(thrown.getMessage()).contains("closedAt must not be null when status is CLOSED");
  }

  @Test
  void closeByManager_transitionsToClosedWithTimestamp() {
    TableSessionId id = TableSessionId.of("S-1");
    TableSession open =
        new TableSession(id, "EUR", TableSessionContents.empty(), TableSessionStatus.OPEN, null);

    Instant closedAt = Instant.parse("2026-01-01T00:00:00Z");
    TableSession closed = open.closeByManager(closedAt);

    assertThat(closed.status()).isEqualTo(TableSessionStatus.CLOSED);
    assertThat(closed.closedAt()).isEqualTo(closedAt);
    assertThat(closed.id()).isEqualTo(id);
    assertThat(closed.currency()).isEqualTo("EUR");
  }

  @Test
  void closeByManager_nullClosedAt_throwsNpe() {
    TableSessionId id = TableSessionId.of("S-1");
    TableSession open =
        new TableSession(id, "EUR", TableSessionContents.empty(), TableSessionStatus.OPEN, null);

    Throwable thrown = assertThrows(NullPointerException.class, () -> open.closeByManager(null));

    assertThat(thrown.getMessage()).contains("closedAt must not be null");
  }

  @Test
  void closeByManager_whenAlreadyClosed_throwsIllegalDomainState() {
    TableSessionId id = TableSessionId.of("S-1");
    TableSession closed =
        new TableSession(
            id,
            "EUR",
            TableSessionContents.empty(),
            TableSessionStatus.CLOSED,
            Instant.parse("2026-01-01T00:00:00Z"));

    Instant closedAt = Instant.parse("2026-01-02T00:00:00Z");

    Throwable thrown =
        assertThrows(IllegalDomainStateException.class, () -> closed.closeByManager(closedAt));

    assertThat(thrown.getMessage()).contains("already CLOSED");
  }

  @Test
  void closeByManager_whenDeliveredItemMissingPayableSnapshot_throwsIllegalDomainState() {
    TableSessionId id = TableSessionId.of("S-1");
    OrderItemId itemId = OrderItemId.of(UUID.randomUUID());
    OrderItem delivered = new OrderItem(itemId, "P-1", 2, OrderItemStatus.DELIVERED);

    // Payable snapshots are intentionally empty -> illegal state for DELIVERED items
    TableSessionContents contents =
        new TableSessionContents(List.of(), List.of(delivered), List.of(), List.of());

    TableSession open = new TableSession(id, "EUR", contents, TableSessionStatus.OPEN, null);

    Instant closedAt = Instant.parse("2026-01-01T00:00:00Z");

    Throwable thrown =
        assertThrows(IllegalDomainStateException.class, () -> open.closeByManager(closedAt));

    assertThat(thrown.getMessage()).contains("Missing payable snapshot");
  }

  @Test
  void closeByManager_whenAcceptedOrderItemsExist_throwsIllegalDomainState() {
    TableSession open =
        openSessionWith(
            List.of(),
            List.of(orderItem(UUID.randomUUID().toString(), OrderItemStatus.ACCEPTED, 1)));

    Instant closedAt = Instant.parse("2026-01-01T00:00:00Z");

    Throwable thrown =
        assertThrows(IllegalDomainStateException.class, () -> open.closeByManager(closedAt));

    assertThat(thrown.getMessage()).contains("ACCEPTED").contains("IN_PROGRESS");
  }

  @Test
  void closeByManager_whenInProgressOrderItemsExist_throwsIllegalDomainState() {
    TableSession open =
        openSessionWith(
            List.of(),
            List.of(orderItem(UUID.randomUUID().toString(), OrderItemStatus.IN_PROGRESS, 1)));

    Instant closedAt = Instant.parse("2026-01-01T00:00:00Z");

    assertThrows(IllegalDomainStateException.class, () -> open.closeByManager(closedAt));
  }

  @Test
  void closeByManager_whenDeliveredItemsHaveRemainingQuantity_throwsIllegalDomainState() {
    OrderItem delivered = orderItem(UUID.randomUUID().toString(), OrderItemStatus.DELIVERED, 2);

    SessionItemSnapshot payable =
        new SessionItemSnapshot(
            delivered.id(), Money.of("EUR", new BigDecimal("2.00")), 1 // remaining unpaid
            );

    TableSession open = openSessionWith(List.of(payable), List.of(delivered));

    Instant closedAt = Instant.parse("2026-01-01T00:00:00Z");

    Throwable thrown =
        assertThrows(IllegalDomainStateException.class, () -> open.closeByManager(closedAt));

    assertThat(thrown.getMessage()).contains("unpaid").contains("DELIVERED");
  }

  @Test
  void closeByManager_whenDeliveredItemsAreFullyPaid_transitionsToClosed() {
    OrderItem delivered = orderItem(UUID.randomUUID().toString(), OrderItemStatus.DELIVERED, 2);

    SessionItemSnapshot payable =
        new SessionItemSnapshot(
            delivered.id(), Money.of("EUR", new BigDecimal("2.00")), 0 // fully paid
            );

    TableSession open = openSessionWith(List.of(payable), List.of(delivered));

    Instant closedAt = Instant.parse("2026-01-01T00:00:00Z");
    TableSession closed = open.closeByManager(closedAt);

    assertThat(closed.status()).isEqualTo(TableSessionStatus.CLOSED);
    assertThat(closed.closedAt()).isEqualTo(closedAt);
  }

  @Test
  void closeByManager_ignoresNonDeliveredItemsInPayableCheck() {
    OrderItem accepted =
        new OrderItem(
            OrderItemId.of(UUID.fromString("00000000-0000-0000-0000-000000000010")),
            "P-1",
            1,
            OrderItemStatus.ACCEPTED);

    // No payable snapshots on purpose
    TableSession open =
        new TableSession(
            TableSessionId.of("S-non-delivered"),
            "EUR",
            new TableSessionContents(List.of(), List.of(accepted), List.of(), List.of()),
            TableSessionStatus.OPEN,
            null);

    Instant closedAt = Instant.parse("2026-01-01T00:00:00Z");

    IllegalDomainStateException ex =
        assertThrows(IllegalDomainStateException.class, () -> open.closeByManager(closedAt));

    // Failure must be due to ACTIVE items, not payable snapshot logic
    assertThat(ex.getMessage()).contains("ACCEPTED");
  }

  private static TableSession openSessionWith(
      List<SessionItemSnapshot> payableItems, List<OrderItem> orderItems) {

    TableSessionId id = TableSessionId.of("S-1");
    TableSessionContents contents =
        new TableSessionContents(payableItems, orderItems, List.of(), List.of());
    return new TableSession(id, "EUR", contents, TableSessionStatus.OPEN, null);
  }

  private static OrderItem orderItem(String uuid, OrderItemStatus status, int quantity) {
    OrderItemId id = OrderItemId.of(UUID.fromString(uuid));
    return new OrderItem(id, "P-1", quantity, status);
  }
}
