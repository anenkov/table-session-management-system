# ADR-005: Single Currency (EUR)

## Status
Accepted

## Context
Supporting multiple currencies significantly increases complexity and is out of scope.

## Decision
- The application operates with a single configured currency: EUR.
- Multi-currency support is explicitly not supported.

## Consequences
- Simpler domain and payment logic.
- Reduced validation and configuration complexity.
