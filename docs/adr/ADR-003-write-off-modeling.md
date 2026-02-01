# ADR-003: Write-Off Modeling

## Status
Accepted

## Context
Discounts, rounding, and compensations must be explicit, auditable, and safe.

## Decision
- WriteOff represents a positive reduction.
- No negative Money values are allowed anywhere.
- WriteOff is immutable and has no identity.
- WriteOffReason enum defines explicit business reasons.
- Session-level and item-level write-offs are modeled separately.

## Consequences
- Financial intent is explicit and auditable.
- No ambiguity in calculations.
