# ms-passport-inspector

Core service of the [passport-inspector-platform](https://github.com/NAIIIK/passport-inspector-platform).
Handles passport verification requests, orchestrates async jobs, and
integrates with SMEV (via [smev-api-mock](https://github.com/NAIIIK/smev-api-mock))
to check document validity.

Runs behind [ms-passport-inspector-gateway](https://github.com/NAIIIK/ms-passport-inspector-gateway) -
this service does not accept traffic directly in the full platform setup, but
it can be run and called on its own for local development.

## Stack

Java 21, Spring Boot 4.1, Spring Security (OAuth2 resource server, JWT),
Spring Data JPA, PostgreSQL, Liquibase, MinIO, OpenFeign, Prometheus/Micrometer.

## Running standalone

Requires PostgreSQL, MinIO, and `smev-api-mock` running (see the
[umbrella repo](https://github.com/NAIIIK/passport-inspector-platform) for
`docker compose up`, or run each dependency yourself and adjust the env vars
below).

```bash
./mvnw spring-boot:run
```

Default port: `8080`.

## Configuration

|         Env var         |                      Default                          |               Purpose              |
|-------------------------|-------------------------------------------------------|------------------------------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/passport_inspector` |         Postgres connection        |
| `MINIO_URL` | `http://localhost:9000` | Object storage for uploaded CSVs |
| `SMEV_CLIENT_URL` | `http://localhost:8081` | smev-api-mock base URL |
| `SMEV_CLIENT_TOKEN` | `test-token` | Header sent to SMEV on each check |
| `JWT_SECRET` | (demo default - override in any real deployment) | Shared HS256 secret with the gateway |
| `CSV_PARSER_WORKER_ENABLED` / `PASSPORT_CHECK_WORKER_ENABLED` | `true` | Toggle each background worker independently |

Full list in `src/main/resources/application.yaml`.

## API

All endpoints are under `/v1/internal/validation/smev/4/clients`, require a
bearer JWT with role `CLIENT`, and are merchant-scoped (the `merchantId`
header must match the token's `merchantId` claim).

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/check` | Start a single passport verification job |
| `GET` | `/check/{jobId}` | Poll result of a single check |
| `POST` | `/check/batch` | Upload a CSV for batch verification |
| `GET` | `/check/batch/{jobId}` | Poll result of a batch check |

All four are async - `POST` calls return immediately with a `jobId`;
`GET` calls report `IN_PROGRESS`, `COMPLETED`, or `FAILED`.

## Monitoring

`/actuator/health`, `/actuator/info`, and `/actuator/prometheus` are public
(no JWT required); all other actuator endpoints require authentication.

## Tests

```bash
./mvnw test
```

Unit and slice tests cover the orchestration service, background workers,
CSV parsing/validation, the merchant-scoped authorization guard, the global
exception handler, and the security configuration itself.