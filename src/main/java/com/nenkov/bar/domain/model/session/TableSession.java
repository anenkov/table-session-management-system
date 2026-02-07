package com.nenkov.bar.domain.model.session;

import com.nenkov.bar.domain.exceptions.IllegalDomainStateException;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOff;
import com.nenkov.bar.domain.service.payment.SessionItemSnapshot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root representing a single table session (canonical tab for a table).
 *
 * <p>The {@code TableSession} owns ordering state and payable state. Payment calculation uses
 * read-only snapshots exposed by this aggregate.
 *
 * <p>Ordering model (3.2.2.2) is minimal and supports only adding order items:
 *
 * <ul>
 *   <li>Identity: {@link OrderItemId}
 *   <li>Quantity
 *   <li>Status: {@link OrderItemStatus}
 * </ul>
 */
public final class TableSession {

  private final TableSessionId id;
  private final String currency;
  private final TableSessionContents contents;

  private final TableSessionStatus status;
  private final Instant closedAt;

  public TableSession(
      TableSessionId id,
      String currency,
      TableSessionContents contents,
      TableSessionStatus status,
      Instant closedAt) {

    this.id = Objects.requireNonNull(id, "id must not be null");
    this.currency = Objects.requireNonNull(currency, "currency must not be null");
    this.contents = Objects.requireNonNull(contents, "contents must not be null");

    this.status = Objects.requireNonNull(status, "status must not be null");
    this.closedAt = closedAt;

    if (status == TableSessionStatus.CLOSED && closedAt == null) {
      throw new IllegalArgumentException("closedAt must not be null when status is CLOSED");
    }
    if (status == TableSessionStatus.OPEN && closedAt != null) {
      throw new IllegalArgumentException("closedAt must be null when status is OPEN");
    }
  }

  public TableSessionId id() {
    return id;
  }

  public String currency() {
    return currency;
  }

  public TableSessionStatus status() {
    return status;
  }

  public Instant closedAt() {
    return closedAt;
  }

  /** Snapshot of payable items consumed by payment calculation services. */
  public List<SessionItemSnapshot> payableItemsSnapshot() {
    return contents.payableItems();
  }

  /** Ordering state belonging to this session. */
  public List<OrderItem> orderItems() {
    return contents.orderItems();
  }

  public List<ItemWriteOff> itemWriteOffs() {
    return contents.itemWriteOffs();
  }

  public List<WriteOff> sessionWriteOffs() {
    return contents.sessionWriteOffs();
  }

  /**
   * Adds new order items to this session.
   *
   * <p>Invariants:
   *
   * <ul>
   *   <li>{@code drafts} must be non-null and non-empty
   *   <li>Each {@link OrderItemDraft} validates productId and quantity at construction time
   * </ul>
   *
   * <p>Behavior:
   *
   * <ul>
   *   <li>Generates a new {@link OrderItemId} per draft
   *   <li>Initial status is {@link OrderItemStatus#ACCEPTED}
   *   <li>Returns updated session plus created ids
   * </ul>
   */
  public OrderItemsAdded addOrderItems(List<OrderItemDraft> drafts) {
    Objects.requireNonNull(drafts, "drafts must not be null");
    if (drafts.isEmpty()) {
      throw new IllegalArgumentException("drafts must not be empty");
    }

    List<OrderItem> newItems = new ArrayList<>();
    List<OrderItemId> createdIds = new ArrayList<>();

    for (OrderItemDraft draft : drafts) {
      OrderItemId orderItemId = OrderItemId.random();
      newItems.add(
          new OrderItem(
              orderItemId, draft.productId(), draft.quantity(), OrderItemStatus.ACCEPTED));
      createdIds.add(orderItemId);
    }

    List<OrderItem> updatedOrderItems = new ArrayList<>(contents.orderItems());
    updatedOrderItems.addAll(newItems);

    TableSessionContents updatedContents =
        new TableSessionContents(
            contents.payableItems(),
            updatedOrderItems,
            contents.itemWriteOffs(),
            contents.sessionWriteOffs());

    TableSession updated = new TableSession(id, currency, updatedContents, status, closedAt);

    return new OrderItemsAdded(updated, List.copyOf(createdIds));
  }

  /**
   * Administratively closes this session (manager-only action).
   *
   * <p><b>Business invariants enforced:</b>
   *
   * <ul>
   *   <li>The session must not contain any {@link OrderItemStatus#ACCEPTED} or {@link
   *       OrderItemStatus#IN_PROGRESS} order items.
   *   <li>The session must not contain any {@link OrderItemStatus#DELIVERED} order items with
   *       unpaid remainder (i.e. a payable snapshot exists and {@code remainingQuantity > 0}).
   * </ul>
   *
   * <p><b>Domain consistency requirement:</b> each {@link OrderItemStatus#DELIVERED} order item
   * must have a corresponding {@link SessionItemSnapshot} entry in {@link
   * TableSessionContents#payableItems()}.
   *
   * @param closedAt the moment the session is closed (must not be null)
   * @return a new session instance with {@link TableSessionStatus#CLOSED} status
   * @throws IllegalDomainStateException if the session is already CLOSED or if any close invariant
   *     is violated
   */
  public TableSession closeByManager(Instant closedAt) {
    Objects.requireNonNull(closedAt, "closedAt must not be null");
    if (status == TableSessionStatus.CLOSED) {
      throw new IllegalDomainStateException("Session is already CLOSED");
    }

    assertNoActiveOrderItems();
    assertNoUnpaidDeliveredItems();

    return new TableSession(id, currency, contents, TableSessionStatus.CLOSED, closedAt);
  }

  private void assertNoActiveOrderItems() {
    boolean hasActive =
        contents.orderItems().stream()
            .anyMatch(
                item ->
                    item.status() == OrderItemStatus.ACCEPTED
                        || item.status() == OrderItemStatus.IN_PROGRESS);

    if (hasActive) {
      throw new IllegalDomainStateException(
          "Session cannot be closed while there are ACCEPTED or IN_PROGRESS order items");
    }
  }

  private void assertNoUnpaidDeliveredItems() {
    // Precondition: assertNoActiveOrderItems() already passed.
    // Given current OrderItemStatus values, all order items here must be DELIVERED.
    for (OrderItem orderItem : contents.orderItems()) {
      SessionItemSnapshot payableSnapshot = findPayableSnapshot(orderItem.id());
      if (payableSnapshot.remainingQuantity() > 0) {
        throw new IllegalDomainStateException(
            "Session cannot be closed while there are unpaid DELIVERED order items");
      }
    }
  }

  private SessionItemSnapshot findPayableSnapshot(OrderItemId itemId) {
    return contents.payableItems().stream()
        .filter(s -> s.itemId().equals(itemId))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalDomainStateException(
                    "Missing payable snapshot for DELIVERED order item: " + itemId));
  }
}
