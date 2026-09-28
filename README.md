# TradeDesk — Trade Journal & Risk Analytics API

A Spring Boot backend for logging trades, computing P&L, and tracking risk
exposure — built as a portfolio project modeling the kind of system used
in trading operations, from the engineering side.

## Status

🚧 Under active development. See progress below.

## Stack

- Spring Boot 3.x (Java 21)
- Spring Data JPA / Hibernate
- Spring Security + JWT
- PostgreSQL + Flyway migrations
- springdoc-openapi (Swagger UI)
- JUnit5, Mockito, Testcontainers

## Architecture

```
React dashboard (Next.js)
        │  REST/JSON
        ▼
Spring Boot API
  Controller → Service → Repository (Spring Data JPA / Hibernate)
        │
        ▼
PostgreSQL
```

_(Full architecture diagram + design decisions to be added as the project develops.)_

## Local setup

Requires Docker and JDK 21.

```bash
# 1. Start local Postgres
docker compose up -d

# 2. Run the app (Flyway migrations run automatically on startup)
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

## Progress log

- [x] Project skeleton, local Postgres via Docker Compose
- [ ] Domain model + first Flyway migration
- [ ] Trade CRUD + unit tests
- [ ] Auth (Spring Security + JWT)
- [ ] P&L / position aggregation logic
- [ ] Testcontainers integration tests
- [ ] AWS deployment (RDS + ECS/Beanstalk)
- [ ] CI/CD (GitHub Actions)
- [ ] React/Next.js dashboard
- [ ] E2E (Playwright) + load testing (JMeter)
