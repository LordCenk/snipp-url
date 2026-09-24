// Thin wrapper around fetch for the snipp-url REST API.
// Error bodies from the backend are plain-text messages; they are surfaced as ApiError.

const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
const TOKEN_KEY = 'snipp.token';

export const UNAUTHORIZED_EVENT = 'snipp:unauthorized';

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export interface Link {
  id: number;
  shortCode: string;
  longUrl: string;
  clickCount: number;
  expiry: string | null;
  createdAt: string | null;
}

export interface LinkPage {
  items: Link[];
  total: number;
}

export interface Share {
  name: string;
  percentage: number;
}

export interface Analytics {
  totalClicks: number;
  totalUrls: number;
  topUrl: Link | null;
  dailyClicks: { date: string; day: string; clicks: number }[];
  devices: Share[];
  referrers: Share[];
  breakdown: Pick<Link, 'id' | 'shortCode' | 'longUrl' | 'clickCount'>[];
}

// --- token storage (browser storage can be unavailable, e.g. in private mode) ---

export function getToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function setToken(token: string | null): void {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token);
    else localStorage.removeItem(TOKEN_KEY);
  } catch {
    // ignore: the session then lasts only as long as the page
  }
}

/** Reads the JWT payload without verifying it (the server does that). */
export function decodeToken(token: string): { sub?: string; exp?: number } | null {
  try {
    const payload = token.split('.')[1];
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
    return JSON.parse(json);
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string, now = Date.now()): boolean {
  const exp = decodeToken(token)?.exp;
  return exp === undefined || exp * 1000 <= now;
}

// --- requests ---

interface RequestOptions {
  method?: string;
  body?: unknown;
  auth?: boolean;
}

async function send(path: string, { method = 'GET', body, auth = true }: RequestOptions = {}): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json, text/plain' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = getToken();
  if (auth && token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(API_BASE + path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, 'Could not reach the server. Check your connection and try again.');
  }

  if (!response.ok) {
    if (response.status === 401 && auth) {
      setToken(null);
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    throw new ApiError(response.status, await errorMessage(response));
  }
  return response;
}

export async function errorMessage(response: Response): Promise<string> {
  if (response.status === 429) {
    const retry = Number(response.headers.get('Retry-After'));
    return retry > 0
      ? `Too many attempts. Try again in ${retry} second${retry === 1 ? '' : 's'}.`
      : 'Too many attempts. Try again in a minute.';
  }
  let text = '';
  try {
    text = (await response.text()).trim();
  } catch {
    // fall through to the default message
  }
  // The backend's error bodies are short plain-text messages; ignore anything else (e.g. HTML error pages)
  if (text && text.length <= 200 && !text.startsWith('<')) return text;
  switch (response.status) {
    case 401:
      return 'Your session has expired. Please log in again.';
    case 403:
      return "You don't have access to that link.";
    case 404:
      return 'Not found.';
    default:
      return 'Something went wrong. Please try again.';
  }
}

async function json<T>(response: Response): Promise<T> {
  return (await response.json()) as T;
}

// --- endpoints ---

export async function register(email: string, password: string): Promise<void> {
  await send('/auth/register', { method: 'POST', body: { email, password }, auth: false });
}

export async function login(email: string, password: string): Promise<string> {
  const res = await send('/auth/login', { method: 'POST', body: { email, password }, auth: false });
  const { token } = await json<{ token: string }>(res);
  return token;
}

export async function listLinks(page: number, size: number): Promise<LinkPage> {
  const res = await send(`/urls/all?page=${page}&size=${size}`);
  const items = await json<Link[]>(res);
  const total = Number(res.headers.get('X-Total-Count') ?? items.length);
  return { items, total };
}

export async function createLink(longUrl: string, expiry: string | null): Promise<Link> {
  const res = await send('/urls/create', { method: 'POST', body: { longUrl, expiry } });
  return json<Link>(res);
}

export async function updateLink(id: number, longUrl: string, expiry: string | null): Promise<void> {
  await send(`/urls/update/${id}`, { method: 'POST', body: { longUrl, expiry } });
}

export async function deleteLink(id: number): Promise<void> {
  await send(`/urls/delete/${id}`, { method: 'DELETE' });
}

export async function getAnalytics(): Promise<Analytics> {
  return json<Analytics>(await send('/analytics/overview'));
}

/** Public short URL for a code. Short links are served by the backend at /s/{code}. */
export function shortUrl(code: string): string {
  const base = (import.meta.env.VITE_SHORT_URL_BASE ?? API_BASE) || window.location.origin;
  return `${base.replace(/\/$/, '')}/s/${code}`;
}
