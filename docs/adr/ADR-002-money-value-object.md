# ADR-002: Money Value Object

## Status
Accepted

## Context
Money calculations require strict correctness, consistency, and immutability.

## Decision
- Money is an immutable value object.
- Single configured currency (EUR).
- Currency is ISO-4217, uppercase, length 3.
- Amount is non-null, scale=2, rounding=HALF_UP, >= 0.
- No negative Money values.
- No FX, VAT, percentage, or discount logic inside Money.

## Consequences
- Monetary logic is predictable and safe.
- All higher-level calculations are delegated to domain services.
