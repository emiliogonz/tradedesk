# TradeDesk — Trade Journal & Risk Analytics API

[![CI](https://github.com/emiliogonz/tradedesk/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/emiliogonz/tradedesk/actions/workflows/ci.yml)

A Spring Boot backend for logging trades and tracking risk exposure, modeled
on the kind of system used in trading operations. Built as a portfolio
project focused on backend engineering fundamentals: secure stateless auth,
explicit schema management, real-database integration tests, and CI.

## Stack

- **Java 25**, **Spring Boot 4.1.1**
- **Spring Security** with stateless JWT auth (jjwt 0.12)
- **Spring Data JPA / Hibernate** on **PostgreSQL 16**
- **Flyway** for versioned schema migrations
- **JUnit 5 + Testcontainers** for integration tests against a real Postgres
- **GitHub Actions** for CI

## Architecture

```
Client (curl / Postman)
        │  REST/JSON + Bearer JWT
        ▼
JwtAuthFilter ──► SecurityContext
        │
        ▼
Controller → Service → Repository (Spring Data JPA / Hibernate)
        │
        ▼
PostgreSQL (schema owned by Flyway)
```

Domain entities: `User`, `Instrument`, `Trade`, `Position`, `PnlSnapshot`, `RiskLimit`.

## API

All endpoints except register and login require `Authorization: Bearer <token>`.

| Method | Path                  | Description                                                    |
|--------|-----------------------|----------------------------------------------------------------|
| POST   | `/api/auth/register`  | Create a user account                                          |
| POST   | `/api/auth/login`     | Exchange credentials for a JWT (15-minute expiry)              |
| GET    | `/api/auth/me`        | Return the currently authenticated user                        |
| POST   | `/api/instruments`    | Create an instrument                                           |
| GET    | `/api/instruments`    | List instruments                                               |
| POST   | `/api/trades`         | Record a trade for the authenticated user                      |
| GET    | `/api/trades`         | Search trades — filters: `instrumentId`, `from`, `to` (ISO dates); paginated via `page`, `size`, `sort` |
| GET    | `/api/positions`      | Net exposure per instrument                                    |
| PUT    | `/api/admin/users/{userId}/role` | Change a user's role (ADMIN only)                    |
| PUT    | `/api/risk-limits/{instrumentId}` | Create or replace an instrument's limit (RISK only) |
## Design decisions

- **Flyway owns the schema.** Hibernate runs with `ddl-auto: validate`, so it
  checks the entity mappings against the schema but never alters it. Every
  schema change is an explicit, versioned, reviewable migration.
- **Stateless JWT auth.** No server-side sessions; each request is
  authenticated from its token. CSRF protection is disabled because there are
  no cookie-based sessions to protect.
- **No fallback for the JWT secret.** `jwt.secret` is read only from the
  `JWT_SECRET` environment variable, with no default in `application.yml`.
  The app fails fast at startup if it's missing, rather than running with a
  secret committed to a public repo.
- **N+1 avoidance on trade search.** The search query uses `JOIN FETCH` on
  the instrument, so a page of trades loads in one query instead of 1 + N.
- **Invalid tokens are logged at debug level.** Expired or malformed tokens
  are routine, so they're silent by default but can be surfaced with
  `logging.level.com.example.tradedesk.security: debug` without a redeploy.
- **Integration tests use a real database.** Testcontainers starts a
  throwaway Postgres in Docker for each test run, so tests exercise the real
  Flyway migrations and SQL instead of an in-memory substitute.
- **Positions are maintained on write under a row lock.** Each trade updates
    its instrument's position in the same transaction, after locking the
    position row with `SELECT ... FOR UPDATE`. A concurrency test fires 20
    simultaneous trades at one instrument and asserts no updates are lost;
    without the lock, the same test ends with 3,000 of exposure instead of
    20,000.
- **Risk limits fail closed and are checked under the position lock.** An
  instrument with no configured limit cannot be traded. The check reads the
  exposure while the position row is locked, so concurrent trades cannot
  jointly breach a limit: five simultaneous 300k trades against a 1M limit
  always produce exactly three fills and two rejections. Trades that reduce
  absolute exposure are always allowed, even when the desk is over its limit.
- **One container and one Spring context for the whole test suite.** All
  integration tests extend a shared base class holding a singleton
  Testcontainers Postgres, so Spring's test context cache reuses a single
  application context. Test data uses generated unique emails and tickers so
  classes can share the database safely. This cut suite time from 55s to 21s.

## Known tradeoffs

- `Trade.user` is lazily loaded and not fetched in the search query. This is
  fine today because responses don't include user data, but exposing trader
  identity in `TradeResponse` would require fetching it (or mapping inside
  the transaction) to avoid a `LazyInitializationException`.
- `/api/auth/me` detects unauthenticated requests by checking for Spring
  Security's default `anonymousUser` principal, which relies on anonymous
  authentication being enabled by default.
- **Role changes take effect at the next login.** The role is a claim in the
  stateless JWT, so a demoted user keeps their old permissions until their
  token expires (at most 15 minutes). Instant revocation would require a
  per-request role lookup or a token denylist, trading statelessness for it.

## Local setup

**Requires:** JDK 25 and Docker.

```bash
# 1. Start local Postgres
docker compose up -d

# 2. Set a JWT signing secret (used as raw bytes; must be at least 32 characters for HS256)
export JWT_SECRET="replace-with-a-random-string-of-32-plus-chars"    # macOS/Linux
# $env:JWT_SECRET="replace-with-a-random-string-of-32-plus-chars"   # Windows PowerShell

# 3. Run the app (Flyway migrations run automatically on startup)
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`.

### Bootstrapping the first admin

Registration always creates a `TRADER`. Promote the first admin once per
environment with SQL; after that, admins manage roles through
`PUT /api/admin/users/{userId}/role`.

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';
```

The user must log in again to receive a token with the new role.

### Running tests

Docker must be running; Testcontainers starts its own Postgres, so the
Compose database isn't needed.

```bash
./mvnw test
```

## CI

Every push and pull request to `master` runs the full test suite on GitHub
Actions (`ubuntu-latest`, Temurin JDK 25). `JWT_SECRET` is supplied from a
repository secret.

## Progress

- [x] Project skeleton, local Postgres via Docker Compose
- [x] Domain model + Flyway migration
- [x] Auth: register, login, JWT filter
- [x] Instrument create/list
- [x] Trade create + filtered, paginated search
- [x] Global exception handling
- [x] Testcontainers integration tests (auth flows)
- [x] CI with GitHub Actions
- [x] Position tracking (maintained on write, pessimistic row locking)
- [ ] P&L calculation
- [x] Risk limit checks (fail-closed, enforced under the position lock)
- [ ] Risk limit management endpoint (role-restricted)
- [ ] Remaining trade operations (get by id, amend, cancel)
- [ ] AWS deployment (RDS + ECS or Elastic Beanstalk)
- [ ] Architecture diagram and demo GIF
- [ ] E2E and load testing
- [ ] Web dashboard (Next.js) consuming the API