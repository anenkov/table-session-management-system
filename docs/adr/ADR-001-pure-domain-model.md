# ADR-001: Pure Domain Model

## Status
Accepted

## Context
The core domain represents business rules and invariants that must remain stable,
testable, and independent from infrastructure and frameworks.

## Decision
- The domain model is pure, synchronous, and framework-free.
- No dependencies on Spring, Reactor, R2DBC, persistence, or infrastructure code.
- Domain logic is expressed via entities, value objects, and domain services only.
- Enforced via ArchUnit rules.

## Consequences
- Domain logic is highly testable.
- Infrastructure concerns cannot leak into business logic.
- Clear separation of responsibilities.
