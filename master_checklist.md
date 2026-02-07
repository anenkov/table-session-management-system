## MASTER CHECKLIST — Table Session Management System

> **Purpose:** Track completed and upcoming work at phase level  
> **Non-goal:** Capture detailed business rules, test inventories, or UI specifics

---

## 0. Phase 0 — Product Framing *(FROZEN)*

- 0.1 Core product scope and flow defined
- 0.2 Partial payment model defined
- 0.3 Session lifecycle rules defined
- 0.4 Manager-only administrative actions defined
- 0.5 Phase explicitly frozen (no further scope changes)

**Status:** ✅ DONE / FROZEN

---

## 1. Phase 1 — Architecture & Design *(DONE)*

- 1.1 Technology stack selected and locked
- 1.2 Domain-first architecture defined
- 1.3 Reactive infrastructure chosen
- 1.4 Persistence approach defined
- 1.5 Explicit non-goals documented

**Status:** ✅ DONE

---

## 2. Phase 2 — Setup & Infrastructure *(DONE)*

- 2.1 Development environment set up
- 2.2 Project skeleton created
- 2.3 Profiles strategy implemented
- 2.4 Database infrastructure configured
- 2.5 Authentication & security foundation implemented
- 2.6 CI pipeline with quality checks enabled
- 2.7 Documentation for running the project added

**Status:** ✅ DONE

---

## 3. Phase 3 — Core Business Logic

### 3.1 Domain & Pricing Model *(DONE)*

- 3.1.1 Money value object designed and locked
- 3.1.2 Single configured currency (EUR) via app configuration
- 3.1.3 Write-off model designed
- 3.1.4 Core aggregates and entities designed
- 3.1.5 Allocation and pricing responsibilities separated

**Status:** ✅ DONE

---

### 3.1.3 Payment & Pricing Services Refactor *(DONE)*

- 3.1.3.1 Payment calculation logic refactored
- 3.1.3.2 Allocation logic extracted and made deterministic
- 3.1.3.3 Rounding and remainder distribution rules locked
- 3.1.3.4 Payment calculation orchestration clarified
- 3.1.3.5 Codebase cleanup completed

**Status:** ✅ DONE

---

### 3.1.4 Domain Tests *(DONE)*

- 3.1.4.1 Unit tests for pricing, allocation, and payment services
- 3.1.4.2 Edge cases fully covered
- 3.1.4.3 Deterministic behavior verified

**Status:** ✅ DONE

---

### 3.2 Application Use Cases *(NOT STARTED)*

- 3.2.1 Session lifecycle use cases
- 3.2.2 Order handling use cases
- 3.2.3 Partial payment use cases
- 3.2.4 Closing and cancellation rules

**Status:** ⬜ IN PROGRESS

---

### 3.3 API Layer *(NOT STARTED)*

- 3.3.1 REST endpoints
- 3.3.2 DTOs and validation
- 3.3.3 Error model
- 3.3.4 Security integration

**Status:** ⬜ NOT STARTED

---

### 3.4 Persistence & Integration *(NOT STARTED)*

- 3.4.1 Domain-to-database mapping
- 3.4.2 Business migrations
- 3.4.3 Integration tests

**Status:** ⬜ NOT STARTED

---

## 4. Phase 4 — UI *(NOT STARTED)*

- 4.1 UI scope intentionally undecided
- 4.2 UI complexity to be decided later

**Status:** ⬜ NOT STARTED

---

## 5. Phase 5 — Hardening *(NOT STARTED)*

- 5.1 Operational configuration
- 5.2 Dockerization
- 5.3 Logging and monitoring

**Status:** ⬜ NOT STARTED

---

## 6. Phase 6 — Delivery *(NOT STARTED)*

- 6.1 Architecture documentation
- 6.2 API snapshot
- 6.3 Demo scenario

**Status:** ⬜ NOT STARTED

---

## 7. CI / CD — Optional / Future

- 7.1 Branch protection rules
- 7.2 Enforced quality gates
- 7.3 Deployment strategy

**Status:** ⬜ OPTIONAL