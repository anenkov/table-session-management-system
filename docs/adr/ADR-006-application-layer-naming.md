# ADR-006: Application Layer Naming

## Status
Accepted

## Context
The application layer orchestrates domain logic and coordinates persistence and
external systems without HTTP or UI coupling.

## Decision
- Application orchestrators are named *Service.
- Focused operations may use *Handler.
- Inputs are named *Input or *Params.
- Outputs are named *Result.
- The term Query is reserved for repository concerns.
- Feature-first package structure is used.

## Consequences
- Naming is pragmatic and industry-aligned.
- No academic or HTTP-driven terminology leaks into the application layer.
