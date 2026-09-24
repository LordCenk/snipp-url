# snipp-url API reference

Base URL: wherever the service runs, e.g. `http://localhost:8080`. An interactive version of this reference is served at `/swagger-ui.html`, with the OpenAPI spec at `/v3/api-docs`.

For setup, configuration and deployment, see [README.md](README.md).

## Conventions

- Request and success bodies are JSON unless noted otherwise.
- Error bodies are plain-text messages, e.g. `Invalid credentials`.
- Authenticated endpoints need the header `Authorization: Bearer <token>`. The token comes from `POST /auth/login`.
- Dates use ISO-8601 local date-time without a time zone, e.g. `2026-12-31T23:59` or `2026-12-31T23:59:00`.

### Status codes

| Code | When |
|---|---|
| 200 | Success |
| 302 | Short link redirect |
| 400 | Invalid input: a validation error (`field: message`), malformed JSON, a bad URL or date, a non-numeric id |
| 401 | Missing, invalid or expired token; wrong credentials |
| 403 | The link belongs to another user |
| 404 | Link or short code not found |
| 405 | Wrong HTTP method |
| 409 | Conflicting data, e.g. a concurrent registration with the same email |
| 410 | Short link has expired |
| 429 | Rate limit exceeded. The `Retry-After` header says how many seconds to wait. |
| 500 | Unexpected server error |

## Authentication

### `POST /auth/register`

Creates an account.

```json
{ "email": "alice@example.com", "password": "correct-horse" }
```

- `email`: a valid email address
- `password`: 8–72 characters

| Response | Body |
|---|---|
| 200 | `User registered` |
| 400 | `Email already exists`, or a validation message |
| 429 | Rate limited (default 10 requests/minute per IP, shared with login) |

### `POST /auth/login`

```json
{ "email": "alice@example.com", "password": "correct-horse" }
```

| Response | Body |
|---|---|
| 200 | `{ "token": "<jwt>" }`. The token is valid for 24 hours by default. |
| 401 | `Invalid credentials` |
| 429 | Rate limited |

## Links

All endpoints in this section require authentication.

A link is returned as:

```json
{ "id": 42, "shortCode": "aZ3kQ9x", "longUrl": "https://example.com/page", "clickCount": 7 }
```

The public short URL is `<base URL>/s/<shortCode>`.

### `POST /urls/create`

```json
{ "longUrl": "https://example.com/page", "expiry": "2026-12-31T23:59" }
```

- `longUrl`: required. Must be an absolute `http` or `https` URL with a host, up to 2048 characters.
- `expiry`: optional. After this time the short link returns 410.

| Response | Body |
|---|---|
| 200 | The created link |
| 400 | `Invalid URL: must be an absolute http(s) URL` or `Invalid expiry format` |
| 429 | Rate limited (default 30 requests/minute per IP) |

### `GET /urls/all`

Lists your links.

- Without query parameters it returns every link as an array.
- With `page` (0-based) and/or `size` (1–100, default 20), it returns one page as an array, newest first. The total number of links is in the `X-Total-Count` response header.

| Response | Body |
|---|---|
| 200 | Array of links |
| 400 | Invalid `page` or `size` |

### `POST /urls/update/{id}`

```json
{ "longUrl": "https://example.com/new", "expiry": "2027-01-31T00:00" }
```

Both fields are optional; omitted or blank fields are left unchanged. The same validation as create applies.

| Response | Body |
|---|---|
| 200 | `URL updated successfully` |
| 400 | Invalid URL or expiry |
| 403 | `Forbidden` (not your link) |
| 404 | `URL not found` |

### `DELETE /urls/delete/{id}`

Deletes the link and its click history.

| Response | Body |
|---|---|
| 200 | `Deleted` |
| 403 | `Forbidden` (not your link) |
| 404 | `URL not found` |

## Redirect

### `GET /s/{shortCode}`

Public. Records a click (user agent, referrer, time) and redirects to the original URL.

| Response | Meaning |
|---|---|
| 302 | Redirect; the `Location` header holds the original URL |
| 404 | Unknown short code |
| 410 | The link has expired |
| 400 | The stored target is not a safe `http(s)` URL (links created before URL validation existed) |

## Analytics

### `GET /analytics/overview`

Requires authentication. Summarises clicks across all of your links.

```json
{
  "totalClicks": 120,
  "totalUrls": 4,
  "topUrl": { "id": 42, "shortCode": "aZ3kQ9x", "longUrl": "https://example.com/page", "clickCount": 90 },
  "dailyClicks": [ { "date": "Sep 24", "clicks": 35 } ],
  "devices":     [ { "name": "Mozilla/5.0 (...)", "percentage": 80 } ],
  "referrers":   [ { "name": "https://twitter.com/", "percentage": 60 }, { "name": "Direct", "percentage": 40 } ],
  "breakdown":   [ { "id": 42, "shortCode": "aZ3kQ9x", "longUrl": "https://example.com/page", "clickCount": 90 } ]
}
```

- `topUrl` is `null` when you have no links.
- `devices` groups clicks by the raw `User-Agent` header.
- `referrers` groups clicks by the `Referer` header. Clicks without one are listed as `Direct`.
- Percentages are rounded, so they may not add up to exactly 100.

## Health

Public; no authentication needed.

| Endpoint | Response |
|---|---|
| `GET /api/health` | `{"status":"UP"}`, or 503 with `{"status":"DOWN"}` when the database is unreachable |
| `GET /api/health/liveness` | Whether the process is running |
| `GET /api/health/readiness` | Whether the app is ready to take traffic |

## Data model

The schema is defined by the Flyway migrations in `src/main/resources/db/migration`.

| Table | Columns |
|---|---|
| `users` | `id`, `email` (unique), `password` (BCrypt hash) |
| `urls` | `id`, `user_id` → users, `short_code` (unique), `long_url`, `click_count`, `crt_at`, `expiry` |
| `analytics_event` | `id`, `url_id` → urls, `device` (User-Agent), `referrer`, `timestamp` |
