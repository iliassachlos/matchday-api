# matchday-api

High-contention ticket sales platform. Models a real scenario: the Greece national
football team vs Netherlands and Germany (October 2026), where ~100k people competed
for a few thousand seats and the real system repeatedly went down.

This is a **portfolio project built to be read by recruiters and interviewers**, so
every decision should be defensible out loud, and the README is a first-class
deliverable rather than an afterthought.

## The one invariant

**A seat is never sold twice.** Everything else — services, Kafka, the waiting room —
is secondary to this. When a design choice trades correctness for throughput, pick
correctness and write down why.

## Stack

- Java 25 (LTS), Spring Boot 4.0.x
- Maven, multi-module, via the Maven wrapper (`./mvnw` — no system Maven installed)
- PostgreSQL 16 + Flyway migrations
- Redis 7 (seat holds, waiting-room queue, rate limiting)
- Kafka (KRaft mode, no Zookeeper)
- Spring Security with asymmetric JWT (RS256) + JWKS
- Resilience4j (circuit breakers, retries)
- Testcontainers + JUnit 5 + Awaitility
- Micrometer -> Prometheus -> Grafana, OpenTelemetry tracing
- k6 for load testing
- Docker Compose for everything. **No Kubernetes** — deliberately out of scope.

### Version notes

Spring Boot 4.x requires **Java 25 minimum** (Boot 4.0 raised the floor from 17 to
25). Both are current as of October 2026. Take whatever Boot 4.0.x version Spring
Initializr lists as current — not a SNAPSHOT or milestone release.

Virtual threads are enabled (`spring.threads.virtual.enabled: true`). This workload is
I/O-bound with high concurrency, which is exactly what virtual threads are for, and it
is worth being able to explain the difference from a platform-thread pool.

Note: virtual threads and synchronized blocks interact badly under pinning in some
cases — prefer `ReentrantLock` over `synchronized` where a lock is actually needed.
Most contention here belongs in the database, not in Java locks.

Do not introduce **Spring Cloud Netflix** components. Eureka, Ribbon, Hystrix and
Zuul are legacy, and a reviewer reading them assumes outdated training:

| Don't use | Use instead | Why |
|---|---|---|
| Eureka | Docker DNS (service name resolves) | Platform does discovery; no extra server |
| Ribbon | Docker/platform load balancing | Ribbon is retired |
| Hystrix | Resilience4j | Hystrix is retired |
| Zuul | Spring Cloud Gateway | Current, reactive |
| Feign | `RestClient` or `@HttpExchange` interfaces | Framework-native since Boot 3.x |
| `RestTemplate` | `RestClient` | `RestTemplate` is deprecated for new code |

## Services

| Module | Responsibility |
|---|---|
| `identity-service` | Register/login, BCrypt passwords, mints RS256 JWTs, exposes JWKS, refresh tokens |
| `ticket-service` | Events, seats, reservations. **The concurrency core — the most important module** |
| `order-service` | Orders, payment orchestration, saga + compensation |
| `waiting-room-service` | Redis-backed queue admission, SSE position stream |
| `gateway` | Spring Cloud Gateway: single entry point, routing, rate limiting |
| `common` | Shared DTOs, events, error types. Keep thin — no business logic |

Four services plus a gateway is the ceiling. Do not add more; depth in
`ticket-service` is worth more than breadth across new modules.

## Architecture rules

**Service-to-service communication defaults to Kafka, not HTTP.** Every synchronous
call couples two services' availability together (uptime multiplies). The write path
— reserve, pay, issue ticket — goes through events. Synchronous calls are only for
reads that need an answer within the request (e.g. seat availability for a page load).

**Each service validates JWTs locally** against `identity-service`'s JWKS endpoint. No
network call per request to an auth server. Only `identity-service` holds the private
key and can mint tokens; everyone else verifies with the public key.

**Never trust the client for identity.** `userId` comes from the validated token, never
from a request body or query parameter.

**Events are published via the transactional outbox pattern.** Writing to the DB and
publishing to Kafka in one unit is not atomic across two systems, so events go into an
`outbox` table in the same transaction as the business write, then a relay publishes
them. No "committed but never published" holes.

**All infrastructure addresses come from environment variables** — DB URLs, Kafka
brokers, Redis hosts, JWKS URIs. Never hardcode `localhost` outside local defaults.
Deployment later swaps Compose for managed services (Neon, Upstash) by changing env
vars only, with no code change.

**Database per service.** Separate schemas at minimum; no cross-service joins or
foreign keys across service boundaries.

## Code style

**Google Java Style**, enforced by Spotless with google-java-format. Run
`./mvnw spotless:apply` before committing; CI fails on violations. google-java-format
is intentionally non-configurable (2-space indent, 100-column lines) and will reformat
whole files — this is expected.

Conventions:
- Java **records** for DTOs, events and value objects. Lombok only for JPA entities
  (`@Getter`, `@Setter`, builders) where records cannot apply.
- Constructor injection only. No `@Autowired` on fields.
- Package by feature, not by layer: `com.matchday.ticket.reservation` holds its
  controller, service, repository and entities together — not `controller/`,
  `service/`, `repository/` trees.
- `application.yml` (not `.properties`), with per-profile files for `local`, `test`,
  `docker`, `prod`.
- Flyway migrations are **append-only and never edited once committed**. Name them
  `V<n>__snake_case_description.sql`.
- Custom exceptions plus a `@RestControllerAdvice` returning RFC 7807
  `ProblemDetail`. No raw stack traces or 500s leaking to clients.

## Testing

Tests are a deliverable here, not a chore — they are the evidence the system works.

- **The concurrency test is the single most important test in the repo**: N threads
  race for 1 seat, assert exactly one wins and the rest fail cleanly. Write it
  *before* the reservation logic, watch it fail, then make it pass.
- Integration tests use **Testcontainers** with real Postgres, Redis and Kafka. No
  H2, no embedded fakes — the behaviour under test is database-specific locking.
- Reuse one container set across tests via a shared base class; starting containers
  per test class is slow enough to discourage running them.
- Security tests use `spring-security-test` (`@WithMockUser`), and must include
  negative cases: user A cannot read user B's order.
- Awaitility for async/Kafka assertions. Never `Thread.sleep`.

## Build phases

Each phase ends with a working, committable state. Never leave the repo broken
between phases.

1. **Foundation** — Compose (Postgres/Redis/Kafka), module layout, Flyway,
   Testcontainers, Actuator health
2. **Identity + security** — register/login, BCrypt, RS256 JWT + JWKS, resource-server
   config, refresh tokens, `ROLE_USER` / `ROLE_ADMIN`
3. **Seat reservation** — the concurrency core. Pessimistic (`SELECT ... FOR UPDATE`)
   and optimistic (`@Version`) locking with the tradeoff documented, Redis seat holds
   with TTL, idempotency keys
4. **Orders + Kafka saga** — transactional outbox, order -> payment -> issuance with
   compensation, mock payment provider that can be made slow or failing on demand,
   Resilience4j circuit breaker
5. **Waiting room + gateway** — Redis queue admission, per-user rate limiting, SSE
   position stream
6. **Observability + load testing** — Prometheus/Grafana dashboards, OTel tracing,
   then k6 ramping to tens of thousands of virtual users asserting zero oversells
7. **Deploy + README** — Fly.io + Neon + Upstash (all free tier), GitHub Actions CI,
   architecture diagram, decision-led README

Phase 3 deserves extra time; it is disproportionately what interviews probe. If time
runs short, **cut phase 5, not phase 6** — load-test evidence is worth more than the
waiting room.

Run a cheap load smoke test (a few hundred concurrent users) as soon as phase 3 works,
rather than waiting for phase 6. Load testing tends to surface design problems that
are expensive to fix late.

## Deployment reality

Free tiers cannot host the full stack (four services + Kafka + Redis + Prometheus +
Grafana needs several GB of RAM). The intended split:

- Full system runs locally via `docker compose up`
- Trimmed version deploys free: services on **Fly.io**, Postgres on **Neon**,
  Redis + Kafka on **Upstash**
- Grafana/Prometheus stay local; README screenshots cover them

This is normal for a portfolio project and should be stated plainly in the README
rather than hidden.

## Scope discipline

The realistic failure mode for this project is not a wrong database — it is 40% of an
over-large system that does not run. A smaller system that provably never oversells,
is load-tested and is well documented beats a half-finished sprawl. When in doubt,
finish and polish what exists instead of adding a service.

Out of scope, deliberately: Kubernetes, service mesh, OAuth2 social login, real
payment providers, email/SMS delivery, admin UI beyond the bare minimum.

## Local commands

    docker compose up -d           # infrastructure + services
    ./mvnw clean verify            # full build with tests
    ./mvnw spotless:apply          # format to Google Java Style
    ./mvnw -pl ticket-service test # single module's tests

## Tooling

IntelliJ IDEA Community Edition for Java. Spring-specific navigation is Ultimate-only,
which is tolerable. VS Code only for the optional React waiting-room page.
