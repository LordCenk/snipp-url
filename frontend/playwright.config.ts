import { defineConfig, devices } from '@playwright/test';

// Runs against E2E_BASE_URL when set (e.g. the docker compose stack on :3000);
// otherwise starts the Vite dev server, which proxies API calls to BACKEND_URL
// (default http://localhost:8080). Either way the backend must already be running.
const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:5173';

export default defineConfig({
  testDir: 'e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    // Lets the tests use a preinstalled Chromium instead of downloading one
    launchOptions: { executablePath: process.env.PLAYWRIGHT_CHROMIUM_PATH || undefined },
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : {
        command: 'npm run dev -- --port 5173 --strictPort',
        url: baseURL,
        reuseExistingServer: true,
        timeout: 60_000,
      },
});
