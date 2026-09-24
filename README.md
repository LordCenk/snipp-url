# snipp-url

A URL shortener REST API with user accounts, per-link expiry and click analytics.

Built with Spring Boot 4 (Java 21), PostgreSQL, Flyway and JWT authentication.

## Features

- Register and log in; requests are authenticated with a JWT bearer token
- Shorten `http(s)` URLs to 7-character codes, with an optional expiry
- `GET /s/{code}` redirects (302). Expired links return 410, unknown links 404
- Update and delete your own links
- Click analytics: totals, clicks per day, user agents, referrers, per-link counts
- Rate limiting on login, registration and link creation
- Health checks at `/api/health` and OpenAPI docs at `/swagger-ui.html`

The full API reference is in [DOCUMENTATION.md](DOCUMENTATION.md).

## Quick start (Docker)

```bash
cp .env.example .env
# set APP_JWT_SECRET in .env, e.g. to the output of: openssl rand -base64 48
docker compose up --build
```

The API is then available at http://localhost:8080. Try http://localhost:8080/api/health and http://localhost:8080/swagger-ui.html.

## Running without Docker

Requirements: Java 21 and PostgreSQL 12+.

```bash
createdb snipp

export DATABASE_URL=jdbc:postgresql://localhost:5432/snipp
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export APP_JWT_SECRET="$(openssl rand -base64 48)"

./mvnw spring-boot:run
```

Flyway creates the schema on startup.

## Configuration

All configuration is done with environment variables.

| Variable | Required | Default | Description |
|---|---|---|---|
| `DATABASE_URL` | yes | | JDBC URL, e.g. `jdbc:postgresql://host:5432/db`. The `postgres://` form some hosts show is not accepted. |
| `SPRING_DATASOURCE_USERNAME` | yes | | Database user |
| `SPRING_DATASOURCE_PASSWORD` | yes | | Database password |
| `APP_JWT_SECRET` | yes | | JWT signing key, **at least 32 characters**. The app refuses to start otherwise. |
| `APP_JWT_EXPIRATION_MS` | no | `86400000` (24h) | Token lifetime |
| `APP_CORS_ALLOWED_ORIGINS` | no | localhost dev ports and the existing Vercel frontends | Comma-separated list of frontend origins. Set this in production. |
| `APP_RATE_LIMIT_AUTH_PER_MINUTE` | no | `10` | Login and registration requests per minute per client IP |
| `APP_RATE_LIMIT_CREATE_PER_MINUTE` | no | `30` | Link creations per minute per client IP |
| `APP_LOG_LEVEL` | no | `INFO` | Log level for application code |
| `SERVER_PORT` | no | `8080` | HTTP port |
| `FORWARD_HEADERS_STRATEGY` | no | `native` | How `X-Forwarded-*` headers from a reverse proxy are handled |
| `JPA_DDL_AUTO` | no | `validate` | Hibernate schema mode. Leave as is; the schema is managed by Flyway. |

## Database migrations

The schema lives in `src/main/resources/db/migration` and is applied by Flyway at startup.

- Add a change as a new file, `V<next number>__description.sql`. Never edit a migration that has already been applied.
- Databases created before Flyway was introduced are baselined at V1 automatically; only later migrations are applied to them.

## Tests

The tests run against a real PostgreSQL database:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/snipp_test
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export APP_JWT_SECRET=test-secret-0123456789abcdef0123456789

./mvnw verify
```

CI (`.github/workflows/ci.yml`) runs the same on every pull request against a Postgres service container. It then builds the Docker image and smoke-tests it.

## Deployment

The `Dockerfile` builds a self-contained image:

- It runs as a non-root user.
- The JVM heap is sized from the container's memory limit (`JAVA_OPTS`).
- A `HEALTHCHECK` calls `/api/health/liveness`.

Health endpoints for a platform's health checks:

| Endpoint | Meaning |
|---|---|
| `/api/health` | Overall status, including the database. Returns 503 when the database is unreachable. |
| `/api/health/liveness` | The process is running |
| `/api/health/readiness` | The app is ready to take traffic |

Checklist for production:

1. Set `APP_JWT_SECRET` to a random value of at least 32 characters.
2. Set `APP_CORS_ALLOWED_ORIGINS` to your frontend's URL(s) only.
3. Point the platform's health check at `/api/health`.
4. Rate limits are kept in memory per instance. If you run more than one instance, lower the limits accordingly, or move rate limiting to a shared store or your gateway.

## Project structure

```
src/main/java/com/yato/urlShortenerb/
  config/      security, JWT, CORS, rate limiting
  controller/  REST endpoints
  dto/         request and response bodies
  entity/      JPA entities
  exception/   error-to-status mapping
  repo/        Spring Data repositories
  service/     business logic
  util/        short code generation, URL validation, log masking
src/main/resources/
  application.properties
  db/migration/   Flyway migrations
```
