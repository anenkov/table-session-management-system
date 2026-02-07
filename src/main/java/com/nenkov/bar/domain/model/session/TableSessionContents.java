package com.nenkov.bar.domain.model.session;

import com.nenkov.bar.domain.model.writeoff.ItemWriteOff;
import com.nenkov.bar.domain.model.writeoff.WriteOff;
import com.nenkov.bar.domain.service.payment.SessionItemSnapshot;
import java.util.List;
import java.util.Objects;

/**
 * Immutable container for list-based state owned by {@link TableSession}.
 *
 * <p>Introduced to keep {@link TableSession}'s constructor small and readable while preserving
 * immutability.
 */
public record TableSessionContents(
    List<SessionItemSnapshot> payableItems,
    List<OrderItem> orderItems,
    List<ItemWriteOff> itemWriteOffs,
    List<WriteOff> sessionWriteOffs) {

  public TableSessionContents {
    payableItems =
        List.copyOf(Objects.requireNonNull(payableItems, "payableItems must not be null"));
    orderItems = List.copyOf(Objects.requireNonNull(orderItems, "orderItems must not be null"));
    itemWriteOffs =
        List.copyOf(Objects.requireNonNull(itemWriteOffs, "itemWriteOffs must not be null"));
    sessionWriteOffs =
        List.copyOf(Objects.requireNonNull(sessionWriteOffs, "sessionWriteOffs must not be null"));
  }

  public static TableSessionContents empty() {
    return new TableSessionContents(List.of(), List.of(), List.of(), List.of());
  }
}
