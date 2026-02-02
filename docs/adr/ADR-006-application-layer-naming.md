# ADR-006: Application Layer Naming and Structure (Hybrid Model)

## Status
Accepted (Amended)

## Context
The application layer orchestrates domain logic and coordinates persistence and
external systems without HTTP, UI, or framework coupling.

As the number of business workflows grows, a single application service per
feature risks becoming a "god object", while one service per action can reduce
discoverability and increase surface area.

A balanced approach is required that preserves:
- Clear feature boundaries
- Explicit business workflows
- Small, testable orchestration units
- Pragmatic, industry-aligned naming

## Decision
The application layer follows a **hybrid Service + Handler model**:

### Service
- Each feature exposes **exactly one public `*Service`**.
- The `*Service` acts as a **feature façade** and entry point.
- The service is aggregate- or feature-centric (e.g. `TableSessionService`).
- The service must remain thin and must not accumulate domain rules.

### Handler
- Each **business action / workflow** is implemented as a dedicated `*Handler`.
- A handler orchestrates:
  - Domain model interactions
  - Repository interfaces
  - Gateway interfaces
- A handler represents **one reason to change**.
- Handlers are internal to the feature package and are invoked by the feature service.

### Naming conventions
- Application façade: `*Service`
- Workflow implementation: `*Handler`
- Inputs: `*Input` or `*Params`
- Outputs: `*Result`
- The term `Query` is reserved for repository concerns only.

### Packaging
- A **feature-first package structure** is used.
- Each feature package contains:
  - One public `*Service`
  - One or more `*Handler`
  - Feature-specific input/output models

### Integration boundaries
Integration boundaries are expressed using role-based interfaces:
- **Repository** interfaces represent persistence-facing dependencies.
- **Gateway** interfaces represent external systems where the application controls interaction (e.g. payments).
- **Client** interfaces represent external systems consumed by the application where it is a consumer of an external API.

Generic terms such as "port" are intentionally avoided in favor of clear,
role-based naming.

## Consequences
- Feature APIs remain cohesive and discoverable.
- Individual workflows stay small, explicit, and testable.
- Application services do not degrade into god objects.
- Workflow growth is linear and predictable.
- The structure remains compatible with Clean Architecture principles without adopting academic terminology.
- Architectural intent is explicit and enforceable via ArchUnit rules.
