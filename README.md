# Table Session Management System

Backend system for managing **table sessions (tabs/bills)** in a hospitality venue (bar).
Focus: session lifecycle, orders, partial payments, manager-approved write-offs, and production-like authentication.

## Tech stack
- Java 25
- Spring Boot (WebFlux) + Project Reactor
- PostgreSQL + R2DBC
- Flyway migrations
- JWT access-token authentication (Spring Security)
- CI pipeline (GitHub Actions)
- SonarCloud Quality Gate (coverage + code quality)

## Scope
- Table sessions with OPEN / CLOSED lifecycle
- Orders and ordering constraints (e.g., ordering blocked when session is CLOSED)
- Partial payments and write-offs with approval (application + domain groundwork exists)
- Role-based access control (manager-only administrative actions planned for close endpoints)

## Status
Last verified against code: 2026-02-15

### Implemented so far
- Auth API (login flow)
- RFC7807 Problem Details:
  - Global `ApiExceptionHandler`
  - Stable `ApiProblemCode`
  - Correlation ID via `X-Request-Id`
- Session API (open + get session)
- Session API (open + get + close)
- Ordering API (add order items)
- Payment API (create check + record payment attempt idempotently)
- CI:
  - Build + tests on pull requests / main
  - SonarCloud analysis with Quality Gate

### Not implemented yet (planned next)
- Manager-only role hardening for close endpoint (explicit role checks)
- Persistence (Phase 3.4): repositories are currently placeholder/stubbed for bootstrapping
- CD / deployment (explicitly out of scope for now)

### API docs
- Swagger UI (local): `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`

## How to run the project

### Prerequisites
- Java 25
- PostgreSQL 18
- Maven (or IntelliJ Maven integration)

### Environment variables
The following environment variables must be provided (never commit secrets):

- `JWT_SECRET` – HMAC secret for signing JWT access tokens

Optional / local-only (if not using defaults in `application-local.yml`):
- Override `spring.r2dbc.*` and `spring.flyway.*` in `application-local.yml`.

### Application configuration
- `app.currency` must be a single ISO-4217 currency code (e.g. `EUR`).
  - The project operates with **one configured currency** and does **not** support multi-currency.

### Local database setup (PostgreSQL 18)
1. Start PostgreSQL 18 (Docker or local install)
2. Create database and user (defaults in `application-local.yml`: `tsms` / `bar` / `bar`, host `localhost:5432`)
3. Ensure the database is reachable from your machine

Flyway migrations run automatically on startup when using the `local` profile.

### Run locally (IntelliJ)
1. Create a Spring Boot Run Configuration
2. Set environment variables (at least `JWT_SECRET`)
3. Activate the `local` profile
4. Run `com.nenkov.bar.Application`

### Run tests
```bash
mvn test
```

### Coverage report (JaCoCo)
After running tests:

- XML: `target/site/jacoco/jacoco.xml`
