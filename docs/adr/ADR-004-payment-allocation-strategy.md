# ADR-004: Payment Allocation Strategy

## Status
Accepted

## Context
Partial payments and discounts require proportional allocation with deterministic rounding.

## Decision
- Introduce PaymentCalculationContext as immutable snapshot.
- Allocation is proportional and quantity-based.
- Remainders are distributed deterministically using largest fractional remainder.
- All division uses shared MathContext.
- Final rounding occurs only at Money boundaries.

## Consequences
- Deterministic, repeatable payment calculations.
- No rounding drift or ambiguity.
