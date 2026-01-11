# Table Session Management System

Backend system for managing table sessions (bills) in a hospitality venue (bar).
Focus: session lifecycle, orders, partial payments, manager-approved write-offs,
and production-like authentication.

## Tech stack (planned)
- Java 25
- Spring Boot (WebFlux) + Project Reactor
- PostgreSQL + R2DBC
- Flyway migrations
- JWT access-token authentication (Spring Security)

## Scope
- Table sessions with OPEN / CLOSED lifecycle
- Orders with manager-controlled cancellation
- Partial payments and write-offs with approval
- Role-based access control (waiter, bartender, manager, owner)

## Status
Work in progress.

## How to run the project

### Prerequisites
- Java 25
- PostgreSQL 18
- Maven (or IntelliJ Maven integration)

### Environment variables
The following environment variables must be provided (never commit secrets):

- `JWT_SECRET` – HMAC secret for signing JWT access tokens

Optional / local-only (if not using defaults in `application-local.yml`):
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`

### Local database setup (PostgreSQL 18)
1. Start PostgreSQL 18 (Docker or local install)
2. Create database and user
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

XML: target/site/jacoco/jacoco.xml
