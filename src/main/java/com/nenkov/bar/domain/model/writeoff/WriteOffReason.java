package com.nenkov.bar.domain.model.writeoff;

/**
 * Enumerates the auditable reasons for applying a write-off.
 *
 * <p>This enum is intentionally small and explicit. It exists to support reporting, policy
 * decisions, and human audit trails. It must not contain business logic.
 */
public enum WriteOffReason {

  /** Price reduction initiated by a discount policy or manual discount. */
  DISCOUNT,

  /** Complimentary reduction (e.g. on-the-house item or service recovery). */
  COMPENSATION,

  /** Reduction due to a promotion or campaign (e.g. voucher, happy hour). */
  PROMOTION,

  /** Administrative correction that isn't covered by other reasons. */
  ADMIN_ADJUSTMENT,

  /** Catch-all reason. Use only when no other enum value fits. */
  OTHER
}
