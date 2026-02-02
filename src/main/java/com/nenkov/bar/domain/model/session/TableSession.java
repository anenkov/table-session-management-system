package com.nenkov.bar.domain.model.session;

import com.nenkov.bar.domain.exceptions.IllegalDomainStateException;
import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOff;
import com.nenkov.bar.domain.service.payment.SessionItemSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root representing a single table session (canonical tab for a table).
 *
 * <p>The {@code TableSession} is the source of truth for payable state: items, remaining
 * quantities, and write-offs. Payment-related domain services consume read-only snapshots exposed
 * by this aggregate.
 *
 * <p>Lifecycle is introduced minimally to enable application orchestration of administrative close.
 * Domain rules for when close is allowed are added in later phases.
 */
public final class TableSession {

  private final TableSessionId id;
  private final String currency;
  private final List<SessionItemSnapshot> payableItems;
  private final List<ItemWriteOff> itemWriteOffs;
  private final List<WriteOff> sessionWriteOffs;

  private final TableSessionStatus status;
  private final Instant closedAt;

  public TableSession(
      TableSessionId id,
      String currency,
      List<SessionItemSnapshot> payableItems,
      List<ItemWriteOff> itemWriteOffs,
      List<WriteOff> sessionWriteOffs,
      TableSessionStatus status,
      Instant closedAt) {

    this.id = Objects.requireNonNull(id, "id must not be null");
    this.currency = Objects.requireNonNull(currency, "currency must not be null");
    this.payableItems =
        List.copyOf(Objects.requireNonNull(payableItems, "payableItems must not be null"));
    this.itemWriteOffs =
        List.copyOf(Objects.requireNonNull(itemWriteOffs, "itemWriteOffs must not be null"));
    this.sessionWriteOffs =
        List.copyOf(Objects.requireNonNull(sessionWriteOffs, "sessionWriteOffs must not be null"));

    this.status = Objects.requireNonNull(status, "status must not be null");
    this.closedAt = closedAt;

    if (status == TableSessionStatus.CLOSED && closedAt == null) {
      throw new IllegalArgumentException("closedAt must not be null when status is CLOSED");
    }
    if (status == TableSessionStatus.OPEN && closedAt != null) {
      throw new IllegalArgumentException("closedAt must be null when status is OPEN");
    }
  }

  /** Returns the session identifier. */
  public TableSessionId id() {
    return id;
  }

  /** Returns the session currency (single-currency model). */
  public String currency() {
    return currency;
  }

  /** Returns the session lifecycle status. */
  public TableSessionStatus status() {
    return status;
  }

  /** Returns the closure timestamp if the session is closed, otherwise {@code null}. */
  public Instant closedAt() {
    return closedAt;
  }

  /**
   * Returns a snapshot of payable session items at the time of access.
   *
   * <p>This snapshot is consumed by payment calculation services.
   */
  public List<SessionItemSnapshot> payableItemsSnapshot() {
    return payableItems;
  }

  /**
   * Returns item-scoped write-offs applied to this session.
   *
   * <p>These are applied before session-level write-offs during payment calculation.
   */
  public List<ItemWriteOff> itemWriteOffs() {
    return itemWriteOffs;
  }

  /** Returns session-level write-offs applied proportionally across payable items. */
  public List<WriteOff> sessionWriteOffs() {
    return sessionWriteOffs;
  }

  /**
   * Administrative close operation (manager-only at the application boundary).
   *
   * <p>Domain invariant at this stage: cannot close an already closed session.
   *
   * <p>More business rules (e.g., no in-progress items) will be added later.
   *
   * @param closedAt non-null closure time
   * @return a new {@code TableSession} instance in status {@link TableSessionStatus#CLOSED}
   */
  public TableSession closeByManager(Instant closedAt) {
    Objects.requireNonNull(closedAt, "closedAt must not be null");
    if (status == TableSessionStatus.CLOSED) {
      throw new IllegalDomainStateException("Session is already CLOSED");
    }

    return new TableSession(
        id,
        currency,
        payableItems,
        itemWriteOffs,
        sessionWriteOffs,
        TableSessionStatus.CLOSED,
        closedAt);
  }
}
