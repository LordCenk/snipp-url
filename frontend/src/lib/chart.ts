/** Clean axis ticks from 0 to at least `max`: steps of 1, 2 or 5 x 10^n. */
export function niceTicks(max: number, target = 4): number[] {
  if (max <= 0) return [0, 1];
  const rough = max / target;
  const magnitude = 10 ** Math.floor(Math.log10(rough));
  const step = [1, 2, 5, 10].map((m) => m * magnitude).find((s) => s >= rough) ?? 10 * magnitude;
  const niceStep = Math.max(1, step);
  const top = Math.ceil(max / niceStep) * niceStep;
  const ticks: number[] = [];
  for (let t = 0; t <= top + niceStep / 2; t += niceStep) ticks.push(t);
  return ticks;
}

export interface DayCount {
  day: string; // YYYY-MM-DD
  clicks: number;
}

function isoDay(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

/**
 * The API only returns days that had clicks. A time axis needs every day, so
 * fill the last `days` days (ending today) with zeros where there is no data.
 */
export function fillDays(counts: DayCount[], days: number, today = new Date()): DayCount[] {
  const byDay = new Map(counts.map((c) => [c.day, c.clicks]));
  const result: DayCount[] = [];
  for (let i = days - 1; i >= 0; i--) {
    const date = new Date(today.getFullYear(), today.getMonth(), today.getDate() - i);
    const day = isoDay(date);
    result.push({ day, clicks: byDay.get(day) ?? 0 });
  }
  return result;
}

export function formatDay(day: string, options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' }): string {
  const [y, m, d] = day.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString(undefined, options);
}
