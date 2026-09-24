const compact = new Intl.NumberFormat(undefined, { notation: 'compact', maximumFractionDigits: 1 });
const whole = new Intl.NumberFormat();

/** 1284 -> "1,284"; 12900 -> "12.9K" (compact from 10,000 up) */
export function formatCount(value: number): string {
  return value >= 10_000 ? compact.format(value) : whole.format(value);
}

/** Backend date-times are ISO local date-times without a zone, e.g. "2026-12-31T23:59:00" */
export function parseLocalDateTime(value: string | null | undefined): Date | null {
  if (!value) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

export function formatDateTime(value: string | null | undefined): string {
  const date = parseLocalDateTime(value);
  return date
    ? date.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })
    : '';
}

export function isExpired(expiry: string | null | undefined, now = new Date()): boolean {
  const date = parseLocalDateTime(expiry);
  return date !== null && date.getTime() <= now.getTime();
}

/** Value for <input type="datetime-local">: "YYYY-MM-DDTHH:mm" */
export function toDateTimeInput(value: string | null | undefined): string {
  return value ? value.slice(0, 16) : '';
}
