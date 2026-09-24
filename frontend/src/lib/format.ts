const compact = new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 });
const whole = new Intl.NumberFormat();

/** 1284 -> "1,284"; 12900 -> "12.9K" (compact from 10,000 up) */
export function formatCount(value: number): string {
  return value >= 10_000 ? compact.format(value) : whole.format(value);
}

/** Backend date-times are UTC instants, e.g. "2026-12-31T18:29:00Z" */
export function parseDateTime(value: string | null | undefined): Date | null {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

/** Shown in the user's own time zone */
export function formatDateTime(value: string | null | undefined): string {
  const date = parseDateTime(value);
  return date
    ? date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
    : '';
}

export function isExpired(expiry: string | null | undefined, now = new Date()): boolean {
  const date = parseDateTime(expiry);
  return date !== null && date.getTime() <= now.getTime();
}

const pad = (n: number) => String(n).padStart(2, '0');

/** An API instant as a value for <input type="datetime-local"> in the user's time zone: "YYYY-MM-DDTHH:mm" */
export function toDateTimeInput(value: string | null | undefined): string {
  const date = parseDateTime(value);
  if (!date) return '';
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

/**
 * A <input type="datetime-local"> value (the user's wall-clock time) as an exact
 * UTC instant for the API, so the server compares the right moment whatever its
 * own time zone: "2026-12-31T23:59" in India -> "2026-12-31T18:29:00.000Z".
 */
export function toApiInstant(localInput: string): string | null {
  if (!localInput) return null;
  const date = new Date(localInput); // no zone suffix: parsed as local time
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
}

/** e.g. "Asia/Kolkata", for labelling time inputs */
export function userTimeZone(): string {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone || 'your local time';
  } catch {
    return 'your local time';
  }
}
