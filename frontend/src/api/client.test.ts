import { describe, expect, it } from 'vitest';
import { decodeToken, errorMessage, isTokenExpired } from './client';

function jwt(payload: object): string {
  const b64 = (o: object) => Buffer.from(JSON.stringify(o)).toString('base64url');
  return `${b64({ alg: 'HS256' })}.${b64(payload)}.signature`;
}

describe('errorMessage', () => {
  it('uses the backend plain-text message', async () => {
    expect(await errorMessage(new Response('Invalid URL: must be an absolute http(s) URL', { status: 400 }))).toBe(
      'Invalid URL: must be an absolute http(s) URL',
    );
  });

  it('turns 429 into a wait time from Retry-After', async () => {
    const res = new Response('Too many requests, try again later', { status: 429, headers: { 'Retry-After': '42' } });
    expect(await errorMessage(res)).toBe('Too many attempts. Try again in 42 seconds.');
  });

  it('falls back to a friendly message for empty or HTML bodies', async () => {
    expect(await errorMessage(new Response('', { status: 401 }))).toBe('Your session has expired. Please log in again.');
    expect(await errorMessage(new Response('<html>502</html>', { status: 502 }))).toBe(
      'Something went wrong. Please try again.',
    );
  });
});

describe('tokens', () => {
  it('reads the subject and expiry', () => {
    const token = jwt({ sub: 'a@test.com', exp: 2_000_000_000 });
    expect(decodeToken(token)).toEqual({ sub: 'a@test.com', exp: 2_000_000_000 });
  });

  it('detects expired and malformed tokens', () => {
    const now = 1_700_000_000_000;
    expect(isTokenExpired(jwt({ exp: now / 1000 - 1 }), now)).toBe(true);
    expect(isTokenExpired(jwt({ exp: now / 1000 + 60 }), now)).toBe(false);
    expect(isTokenExpired('garbage', now)).toBe(true);
  });
});
