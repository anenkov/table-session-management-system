package com.nenkov.bar.domain.model.session;

/**
 * Lifecycle status of an order item inside a table session.
 *
 * <p>Only the initial status (ACCEPTED) is used for now. Transitions are intentionally out of scope
 * for this phase.
 */
public enum OrderItemStatus {
  ACCEPTED,
  IN_PROGRESS,
  DELIVERED
}
