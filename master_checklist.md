## MASTER CHECKLIST — Table Session Management System

This checklist is the **authoritative, living** phase tracker (0–7).  
It is intentionally concise at phase/subtask level and avoids duplicating detailed domain rules that live in code/ADRs/tests.
Last verified against code: 2026-02-15

Legend: ✅ DONE · 🟨 IN PROGRESS · ⬜ PLANNED

---

## Phase 0 — Product Framing ✅ DONE / FROZEN

- Core flow and roles locked (customer tablets vs manager terminal)
- Partial payment per items/quantities (delivered-only selection)
- Close rules locked (block close on ACCEPTED/IN_PROGRESS or unpaid DELIVERED)
- Multiple bills/checks per session supported
- Write-off on remainder on close (administrative/manager flow)

---

## Phase 1 — Architecture & Design ✅ DONE

- Architecture style and layering decisions
- Tech stack decisions
- API error strategy (eventually RFC7807)
- Non-goals and scope constraints

---

## Phase 2 — Setup & Infrastructure ✅ DONE

- Project skeleton, build, formatting, baseline docs
- Local Postgres + migrations foundation
- Auth foundation (JWT, WebFlux Security)
- CI build + tests, SonarCloud integration, JaCoCo XML to Sonar

---

## Phase 3 — Core Product Implementation

### 3.1 Domain Model & Pricing ✅ DONE
- Money value object (immutable, strict invariants, no FX)
- WriteOff / WriteOffReason modeling
- Aggregate modeling decisions (TableSession as canonical tab)
- Pricing & payment calculation responsibilities split

### 3.1.3 Payment & Pricing Services Refactor ✅ DONE
- PaymentCalculationContext snapshot
- DefaultCheckAmountCalculator orchestrator
- ItemWriteOffAllocation extracted
- ProportionalAllocator + deterministic remainder distribution policy
- WORK_CONTEXT MathContext rule locked

### 3.1.4 Domain Tests ✅ DONE
- DiscountCalculator
- ProportionalAllocator + RemainderDistributor
- ItemWriteOffAllocation
- DefaultCheckAmountCalculator
- Coverage thresholds and test conventions

---

### 3.2 Application Use Cases ✅ DONE
- 3.2.0 Application foundation (feature-first packages; boundaries; ArchUnit guardrails)
- 3.2.1 Integration interfaces (repositories/gateways; interface-only boundaries)
- 3.2.2 Application Use Cases — Implementation Phase ✅ DONE
  - 3.2.2.1 Session Open & Get ✅ DONE
  - 3.2.2.2 Ordering Add Order Items ✅ DONE
  - 3.2.2.3 Payment Create Check ✅ DONE
  - 3.2.2.4 Payment Record Payment Attempt (Idempotent) ✅ DONE
  - 3.2.2.5 Session Close ✅ DONE
  - 3.2.2.6 Coverage & Consolidation ✅ DONE

---

### 3.3 API Layer 🟨 IN PROGRESS
- 3.3.1 API Foundation & Conventions ✅ DONE
  - RFC7807 Problem Details + ApiProblemCode
  - Global ApiExceptionHandler (WebFlux)
  - Validation semantics + correlationId propagation
  - Spring Boot 4 web testing conventions
- 3.3.2 Session API — Open & Get Session ✅ DONE
- 3.3.3 Ordering API — Add Order Items ✅ DONE
- 3.3.4 Payment API — Create Check ✅ DONE
- 3.3.5 Payment API — Record Payment Attempt (Idempotent) ✅ DONE
- 3.3.6 Session API — Close Session (Manager-Only) ✅ DONE (role-hardening pending)
- 3.3.7 Global Error Handling & HTTP Semantics ⬜ PLANNED
- 3.3.8 API Security Integration ⬜ PLANNED
- 3.3.9 API Contract Regression Suite (Web Layer) ⬜ PLANNED (starts after 3.3.7 and 3.3.8)
- 3.3.10 OpenAPI / Swagger Generation ✅ DONE
- 3.3.x Review & Gap Check ⬜ PLANNED

---

### 3.4 Persistence ⬜ PLANNED
- 3.4.1 Persistence foundation (R2DBC + Flyway conventions)
- 3.4.2 TableSession persistence (aggregate mapping; constraints)
- 3.4.3 Ordering persistence
- 3.4.4 Payment persistence (checks + attempts)
- 3.4.5 Integration tests / testcontainers (if adopted)

---

### 3.2.7 Product Catalog Foundation ⬜ PLANNED
- ProductId + MenuProduct model
- Application boundary for catalog access
- Enforce product existence/active state during ordering
- Snapshot product name + unit price at order time
- Persistence may be seeded or stubbed (manager CRUD optional/out of scope)

---

## Phase 4 — UI ⬜ PLANNED
- UI scope intentionally undecided (not forced minimal)
- Manager vs customer UX to be defined later

---

## Phase 5 — Hardening ⬜ PLANNED
- Operational configuration + resilience
- Logging/monitoring conventions
- Dockerization

---

## Phase 6 — Delivery ⬜ PLANNED
- Architecture documentation
- Demo scenario + runbook
- Release readiness checklist

---

## Phase 7 — CI/CD (CI DONE, CD PLANNED) 🟨 PARTIAL
- CI pipeline (build + tests) ✅ DONE
- SonarCloud Quality Gate ✅ DONE
- CD / deployment strategy ⬜ PLANNED
