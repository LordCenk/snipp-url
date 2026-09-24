import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import { isExpired, toApiInstant, toDateTimeInput } from './format';

// Run these as a user in India (UTC+05:30) so time-zone handling is actually exercised
const originalTz = process.env.TZ;
beforeAll(() => {
  process.env.TZ = 'Asia/Kolkata';
});
afterAll(() => {
  process.env.TZ = originalTz;
});

describe('toApiInstant', () => {
  it("converts the user's wall-clock time to a UTC instant", () => {
    expect(toApiInstant('2026-12-31T23:59')).toBe('2026-12-31T18:29:00.000Z');
  });

  it('returns null for empty or invalid input', () => {
    expect(toApiInstant('')).toBeNull();
    expect(toApiInstant('not a date')).toBeNull();
  });
});

describe('toDateTimeInput', () => {
  it("shows an API instant in the user's time zone", () => {
    expect(toDateTimeInput('2026-12-31T18:29:00Z')).toBe('2026-12-31T23:59');
  });

  it('round-trips with toApiInstant', () => {
    expect(toDateTimeInput(toApiInstant('2027-03-01T08:15'))).toBe('2027-03-01T08:15');
  });

  it('is empty when there is no expiry', () => {
    expect(toDateTimeInput(null)).toBe('');
  });
});

describe('isExpired', () => {
  it('compares instants, not wall-clock times', () => {
    const now = new Date('2026-09-24T12:00:00Z');
    expect(isExpired('2026-09-24T11:59:59Z', now)).toBe(true);
    expect(isExpired('2026-09-24T12:00:01Z', now)).toBe(false);
    expect(isExpired(null, now)).toBe(false);
  });
});
