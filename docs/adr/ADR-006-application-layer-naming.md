# ADR-006: Application Layer Naming

## Status
Accepted

## Context
The application layer orchestrates domain logic and coordinates persistence and
external systems without HTTP or UI coupling.

## Decision
- Application orchestrators are named *Service.
- Focused operations may be implemented as *Handler.
- Inputs are named *Input or *Params.
- Outputs are named *Result.
- The term Query is reserved for repository concerns only.
- A feature-first package structure is used.

Integration boundaries are expressed using role-based interfaces:
- Repository interfaces represent persistence-facing dependencies.
- Gateway interfaces represent external system dependencies where the
  application controls the interaction (e.g. payments).
- Client interfaces represent external systems consumed by the application
  where the application is a consumer of an external API.

Generic terms such as "port" are intentionally avoided in favor of
clear, role-based naming.

## Consequences
- Naming is pragmatic and industry-aligned.
- Integration points are explicit and self-describing.
- External dependencies are clearly categorized by role.
- No academic or HTTP-driven terminology leaks into the application layer.
