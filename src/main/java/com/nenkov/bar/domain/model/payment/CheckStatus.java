package com.nenkov.bar.domain.model.payment;

/**
 * Lifecycle status of a {@link Check}.
 *
 * <p>The lifecycle is intentionally small. It captures the minimum states needed to represent a
 * payment attempt and its result.
 */
public enum CheckStatus {

  /** Check is created and ready to be submitted to a payment portal. */
  CREATED,

  /**
   * Payment has been authorized but not captured.
   *
   * <p>This is reserved for future payment flows and may remain unused initially.
   */
  AUTHORIZED,

  /** Payment completed successfully. */
  PAID,

  /** Payment attempt failed. */
  FAILED,

  /** Check was canceled before it completed. */
  CANCELED
}
