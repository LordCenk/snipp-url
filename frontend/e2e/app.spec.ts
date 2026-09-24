import { expect, test, type APIRequestContext, type Page } from '@playwright/test';

const IPHONE =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1';
const DESKTOP =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36';

function uniqueEmail() {
  return `e2e-${Date.now()}-${Math.floor(Math.random() * 1e6)}@test.dev`;
}

/** Visits a short link like a browser would, without following the redirect to the real site. */
async function visitShortLink(request: APIRequestContext, code: string, headers: Record<string, string> = {}) {
  return request.get(`/s/${code}`, { maxRedirects: 0, headers });
}

async function register(page: Page, email: string, password: string) {
  await page.goto('/register');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page.getByRole('heading', { name: 'Your links' })).toBeVisible();
}

test('sign up, shorten, follow, see stats, edit, delete and log out', async ({ page, request }) => {
  const email = uniqueEmail();
  const password = 'e2e-password-1';

  // Signed-out visitors are sent to the login page
  await page.goto('/');
  await expect(page).toHaveURL(/\/login$/);

  await register(page, email, password);
  await expect(page.getByText(email)).toBeVisible();
  await expect(page.getByText('No links yet')).toBeVisible();

  // Unsafe URLs are rejected before reaching the API
  const urlInput = page.getByPlaceholder('Paste a long URL');
  await urlInput.fill('javascript:alert(1)');
  await page.getByRole('button', { name: 'Shorten' }).click();
  await expect(page.getByRole('alert')).toHaveText('Only http and https links can be shortened.');

  // A URL without a scheme gets https:// added
  await urlInput.fill('example.com/e2e-original');
  await page.getByRole('button', { name: 'Shorten' }).click();
  const row = page.locator('tbody tr').filter({ hasText: 'example.com/e2e-original' });
  await expect(row).toBeVisible();
  await expect(page.getByRole('status').filter({ hasText: 'Your short link' })).toBeVisible();
  const code = (await row.locator('.short-link').textContent())!.trim();
  expect(code).toMatch(/^[0-9A-Za-z]{7}$/);

  // The short link redirects; one click from a phone via Twitter, one direct from a desktop
  const phoneClick = await visitShortLink(request, code, { 'User-Agent': IPHONE, Referer: 'https://twitter.com/someone' });
  expect(phoneClick.status()).toBe(302);
  expect(phoneClick.headers()['location']).toBe('https://example.com/e2e-original');
  expect((await visitShortLink(request, code, { 'User-Agent': DESKTOP })).status()).toBe(302);

  await page.reload();
  await expect(row.locator('td.num')).toHaveText('2');
  await expect(row.getByText('Active')).toBeVisible();

  // Stats reflect both clicks
  await page.getByRole('link', { name: 'Stats' }).click();
  await expect(page.locator('.stat').filter({ hasText: 'Total clicks' })).toContainText('2');
  await expect(page.locator('.stat').filter({ hasText: 'Links' })).toContainText('1');
  await expect(page.locator('.stat').filter({ hasText: 'Top link' })).toContainText(code);
  await expect(page.getByRole('listitem', { name: 'Mobile: 50%' })).toBeVisible();
  await expect(page.getByRole('listitem', { name: 'Desktop: 50%' })).toBeVisible();
  await expect(page.getByRole('listitem', { name: 'twitter.com: 50%' })).toBeVisible();
  await expect(page.getByRole('listitem', { name: 'Direct: 50%' })).toBeVisible();
  // Today is the last column of the clicks-per-day chart
  await expect(page.locator('.chart-hit').last()).toHaveAttribute('aria-label', /: 2 clicks$/);
  await page.getByRole('button', { name: 'Show table' }).click();
  await expect(page.locator('table tbody tr').first().locator('td.num')).toHaveText('2');

  // Editing the destination takes effect on the very next click (cache is evicted)
  await page.getByRole('link', { name: 'Links' }).click();
  await row.getByRole('button', { name: `Edit ${code}` }).click();
  const dialog = page.getByRole('dialog', { name: `Edit ${code}` });
  await dialog.getByLabel('Destination URL').fill('example.com/e2e-updated');
  await dialog.getByRole('button', { name: 'Save' }).click();
  await expect(dialog).toBeHidden();
  await expect(page.locator('tbody tr').filter({ hasText: 'example.com/e2e-updated' })).toBeVisible();
  expect((await visitShortLink(request, code)).headers()['location']).toBe('https://example.com/e2e-updated');

  // Deleting removes the link and the short URL stops working
  await page.getByRole('button', { name: `Delete ${code}` }).click();
  await page.getByRole('dialog', { name: 'Delete this link?' }).getByRole('button', { name: 'Delete link' }).click();
  await expect(page.getByText('No links yet')).toBeVisible();
  expect((await visitShortLink(request, code)).status()).toBe(404);

  // Logging out ends the session
  await page.getByRole('button', { name: 'Log out' }).click();
  await expect(page).toHaveURL(/\/login$/);
  await page.goto('/stats');
  await expect(page).toHaveURL(/\/login$/);

  // Logging back in works and lands on the links page
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Log in' }).click();
  await expect(page.getByRole('heading', { name: 'Your links' })).toBeVisible();
});

test('shows clear errors for bad credentials and duplicate sign-ups', async ({ page }) => {
  const email = uniqueEmail();
  await register(page, email, 'e2e-password-1');
  await page.getByRole('button', { name: 'Log out' }).click();

  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill('wrong-password');
  await page.getByRole('button', { name: 'Log in' }).click();
  await expect(page.getByRole('alert')).toHaveText('Incorrect email or password.');

  await page.getByRole('link', { name: 'Create an account' }).click();
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill('e2e-password-1');
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page.getByRole('alert')).toContainText('already exists');

  await page.getByLabel('Password').fill('short');
  await page.getByRole('button', { name: 'Create account' }).click();
  await expect(page.getByRole('alert')).toHaveText('Password must be at least 8 characters.');
});

test('an expired session sends the user back to login with a message', async ({ page }) => {
  await register(page, uniqueEmail(), 'e2e-password-1');
  // Simulate the server rejecting the token (e.g. it expired or the secret rotated)
  await page.evaluate(() => localStorage.setItem('snipp.token', 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4QHkueiIsImV4cCI6NDEwMjQ0NDgwMH0.bad'));
  await page.reload();
  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole('status')).toHaveText('Your session has expired. Please log in again.');
});
