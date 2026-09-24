# snipp web app

The React frontend for snipp-url: sign up, shorten links, manage them, and see click stats.

Built with React 19, TypeScript, Vite and React Router. It has no UI or chart libraries: the charts are small SVG components in `src/components`.

## Running it

The easiest way is the whole stack from the repository root:

```bash
docker compose up --build   # then open http://localhost:3000
```

To work on the frontend, run the backend on port 8080 (see the root README), then:

```bash
npm install
npm run dev        # http://localhost:5173
```

The dev server forwards API paths (`/auth/`, `/urls/`, `/analytics/`, `/s/`, `/api/`) to `BACKEND_URL` (default `http://localhost:8080`). The browser only ever talks to one origin, so CORS doesn't come into play.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Dev server with hot reload |
| `npm run build` | Type-check and build to `dist/` |
| `npm run preview` | Serve the production build locally (same proxy as `dev`) |
| `npm run typecheck` | TypeScript only |
| `npm test` | Unit tests (Vitest) |
| `npm run e2e` | Browser tests (Playwright) against a running backend |

For the end-to-end tests, install a browser once with `npx playwright install chromium`. By default `npm run e2e` starts the dev server itself. Set `E2E_BASE_URL=http://localhost:3000` to test the docker compose stack instead.

## Configuration

| Variable | When | Default | |
|---|---|---|---|
| `BACKEND_URL` | dev server / nginx container | `http://localhost:8080` (dev), `http://app:8080` (container) | Where API requests are forwarded |
| `VITE_API_BASE_URL` | build time | same origin | Set only when the API is on a different origin, e.g. `https://api.example.com`. The backend's `APP_CORS_ALLOWED_ORIGINS` must then include the frontend's origin. |
| `VITE_SHORT_URL_BASE` | build time | the API origin | Origin used when displaying short links, if different |

## Deployment

`Dockerfile` builds the app and serves it with nginx (`nginx.conf.template`). nginx:
- serves the built files, sending every app route to `index.html`
- forwards API and short-link paths to `BACKEND_URL`, passing the browser's host, port and scheme along so the backend's same-origin checks work behind proxies
- caches hashed assets for a year and never caches `index.html`

To host the frontend separately (e.g. on Vercel or Netlify), build with `VITE_API_BASE_URL` pointing at the API and configure the host to serve `index.html` for unknown paths.

## Structure

```
src/
  api/client.ts      fetch wrapper: auth token, error messages, endpoints
  auth/              session state (log in/out, expiry, 401 handling)
  pages/             AuthPage (login + register), LinksPage, StatsPage
  components/        Layout, Dialog, CopyButton, ColumnChart, BarList, StatTile
  lib/               URL normalization, number/date formatting, chart helpers
e2e/                 Playwright end-to-end tests
```
