const SCHEME = /^[a-z][a-z0-9+.-]*:/i;

export type NormalizedUrl = { ok: true; url: string } | { ok: false; error: string };

/**
 * Prepares user input for the API, which only accepts absolute http(s) URLs:
 * "example.com/page" becomes "https://example.com/page".
 */
export function normalizeUrl(input: string): NormalizedUrl {
  const trimmed = input.trim();
  if (!trimmed) return { ok: false, error: 'Enter a URL to shorten.' };

  const candidate = SCHEME.test(trimmed) ? trimmed : `https://${trimmed}`;
  let parsed: URL;
  try {
    parsed = new URL(candidate);
  } catch {
    return { ok: false, error: "That doesn't look like a valid URL." };
  }
  if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
    return { ok: false, error: 'Only http and https links can be shortened.' };
  }
  if (!parsed.hostname.includes('.') && parsed.hostname !== 'localhost') {
    return { ok: false, error: "That doesn't look like a valid URL." };
  }
  if (candidate.length > 2048) return { ok: false, error: 'URLs can be at most 2048 characters.' };
  return { ok: true, url: candidate };
}

/**
 * Groups referrer shares by site: "https://www.google.com/search?q=x" and
 * "https://google.com/" both count as "google.com". Sorted largest first.
 */
export function groupReferrers(shares: { name: string; percentage: number }[]): { name: string; percentage: number }[] {
  const totals = new Map<string, number>();
  for (const { name, percentage } of shares) {
    let site = name;
    try {
      site = new URL(name).hostname.replace(/^www\./i, '') || name;
    } catch {
      // not a URL (e.g. "Direct"): keep as is
    }
    totals.set(site, (totals.get(site) ?? 0) + percentage);
  }
  return [...totals.entries()]
    .map(([name, percentage]) => ({ name, percentage }))
    .sort((a, b) => b.percentage - a.percentage || a.name.localeCompare(b.name));
}

/** "https://www.example.com/a/b?c" -> "example.com/a/b?c", for compact display */
export function displayUrl(url: string): string {
  return url.replace(/^https?:\/\//i, '').replace(/^www\./i, '').replace(/\/$/, '');
}
