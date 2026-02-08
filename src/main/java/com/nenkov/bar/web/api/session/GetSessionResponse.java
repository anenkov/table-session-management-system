package com.nenkov.bar.web.api.session;

import java.util.List;

/**
 * Read-only API projection of a table session.
 *
 * <p>This is mapped from the application-layer {@code GetTableSessionResult}.
 */
public record GetSessionResponse(
    String sessionId,
    String currency,
    List<PayableItem> payableItems,
    List<ItemWriteOff> itemWriteOffs,
    List<SessionWriteOff> sessionWriteOffs) {

  /** Payable state snapshot of a single order item. */
  public record PayableItem(String itemId, Money unitPrice, int remainingQuantity) {}

  /** Item-scoped write-off applied to a specific item and quantity. */
  public record ItemWriteOff(
      String itemId, int quantity, Money amount, String reason, String note) {}

  /** Session-level write-off applied to the overall total. */
  public record SessionWriteOff(Money amount, String reason, String note) {}

  /** Monetary amount representation suitable for JSON contracts. */
  public record Money(String amount, String currency) {}
}
