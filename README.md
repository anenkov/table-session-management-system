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