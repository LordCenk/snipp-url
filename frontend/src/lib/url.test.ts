import { describe, expect, it } from 'vitest';
import { displayUrl, groupReferrers, normalizeUrl } from './url';

describe('normalizeUrl', () => {
  it('adds https:// when the scheme is missing', () => {
    expect(normalizeUrl('example.com/page')).toEqual({ ok: true, url: 'https://example.com/page' });
    expect(normalizeUrl('  www.example.com  ')).toEqual({ ok: true, url: 'https://www.example.com' });
  });

  it('keeps explicit http and https URLs', () => {
    expect(normalizeUrl('http://example.com')).toEqual({ ok: true, url: 'http://example.com' });
    expect(normalizeUrl('https://example.com/a?b=c#d')).toEqual({ ok: true, url: 'https://example.com/a?b=c#d' });
  });

  it('rejects schemes the API refuses', () => {
    for (const input of ['javascript:alert(1)', 'data:text/html,hi', 'file:///etc/passwd', 'ftp://example.com']) {
      expect(normalizeUrl(input).ok).toBe(false);
    }
  });

  it('rejects empty and malformed input', () => {
    expect(normalizeUrl('   ')).toEqual({ ok: false, error: 'Enter a URL to shorten.' });
    expect(normalizeUrl('not a url').ok).toBe(false);
    expect(normalizeUrl('nodot').ok).toBe(false);
  });

  it('rejects URLs over the API limit', () => {
    expect(normalizeUrl('https://example.com/' + 'a'.repeat(2048)).ok).toBe(false);
  });
});

describe('displayUrl', () => {
  it('strips scheme, www and trailing slash', () => {
    expect(displayUrl('https://www.example.com/')).toBe('example.com');
    expect(displayUrl('http://example.com/a/b?c')).toBe('example.com/a/b?c');
  });
});

describe('groupReferrers', () => {
  it('groups by site and sorts by share', () => {
    expect(
      groupReferrers([
        { name: 'https://www.google.com/search?q=a', percentage: 10 },
        { name: 'Direct', percentage: 30 },
        { name: 'https://google.com/', percentage: 25 },
        { name: 'https://twitter.com/', percentage: 35 },
      ]),
    ).toEqual([
      { name: 'google.com', percentage: 35 },
      { name: 'twitter.com', percentage: 35 },
      { name: 'Direct', percentage: 30 },
    ]);
  });
});
