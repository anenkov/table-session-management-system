package com.nenkov.bar.domain.model.discount;

/** Defines how a {@link Discount} specifies its reduction. */
public enum DiscountType {

  /** Discount expressed as a percentage of a base amount. */
  PERCENT,

  /** Discount expressed as a fixed monetary amount. */
  FLAT_AMOUNT
}
