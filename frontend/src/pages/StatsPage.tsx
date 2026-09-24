import { useEffect, useMemo, useState } from 'react';
import * as api from '../api/client';
import BarList from '../components/BarList';
import ColumnChart, { type Column } from '../components/ColumnChart';
import StatTile from '../components/StatTile';
import { fillDays, formatDay } from '../lib/chart';
import { formatCount } from '../lib/format';
import { displayUrl, groupReferrers } from '../lib/url';

const DAYS = 30;

export default function StatsPage() {
  const [data, setData] = useState<api.Analytics | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showTable, setShowTable] = useState(false);

  useEffect(() => {
    api.getAnalytics().then(setData, (err) => setError(err instanceof Error ? err.message : 'Could not load stats.'));
  }, []);

  const columns = useMemo<Column[]>(() => {
    if (!data) return [];
    return fillDays(data.dailyClicks.map((d) => ({ day: d.day, clicks: d.clicks })), DAYS).map((d) => ({
      key: d.day,
      label: formatDay(d.day),
      longLabel: formatDay(d.day, { weekday: 'short', month: 'short', day: 'numeric' }),
      value: d.clicks,
    }));
  }, [data]);

  const breakdown = useMemo(
    () => (data ? [...data.breakdown].sort((a, b) => b.clickCount - a.clickCount) : []),
    [data],
  );

  if (error) return <p className="alert alert-error" role="alert">{error}</p>;
  if (!data) return <p className="muted" aria-busy="true">Loading stats…</p>;

  const recentClicks = columns.reduce((sum, c) => sum + c.value, 0);

  return (
    <div className="stack">
      <h1 className="page-title">Stats</h1>

      <div className="stats-row">
        <StatTile label="Total clicks" value={formatCount(data.totalClicks)} />
        <StatTile label="Links" value={formatCount(data.totalUrls)} />
        <StatTile
          label="Top link"
          value={data.topUrl && data.topUrl.clickCount > 0 ? data.topUrl.shortCode : '—'}
          hint={
            data.topUrl && data.topUrl.clickCount > 0
              ? `${formatCount(data.topUrl.clickCount)} clicks · ${displayUrl(data.topUrl.longUrl)}`
              : 'No clicks yet'
          }
        />
      </div>

      <section className="card">
        <div className="section-head">
          <div>
            <h2>Clicks per day</h2>
            <span className="muted">Last {DAYS} days · {formatCount(recentClicks)} clicks</span>
          </div>
          <button type="button" className="btn btn-ghost btn-sm" onClick={() => setShowTable(!showTable)} aria-pressed={showTable}>
            {showTable ? 'Show chart' : 'Show table'}
          </button>
        </div>
        {showTable ? (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr><th scope="col">Day</th><th scope="col" className="num">Clicks</th></tr>
              </thead>
              <tbody>
                {[...columns].reverse().map((c) => (
                  <tr key={c.key}><td>{c.longLabel}</td><td className="num">{formatCount(c.value)}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <ColumnChart data={columns} valueLabel="Clicks" />
        )}
      </section>

      <div className="grid-2">
        <section className="card">
          <h2>Devices</h2>
          <BarList items={data.devices} emptyText="No clicks yet." />
        </section>
        <section className="card">
          <h2>Referrers</h2>
          <BarList items={groupReferrers(data.referrers)} emptyText="No clicks yet." />
        </section>
      </div>

      <section className="card">
        <h2>Clicks by link</h2>
        {breakdown.length === 0 ? (
          <p className="empty">No links yet.</p>
        ) : (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th scope="col">Short link</th>
                  <th scope="col">Destination</th>
                  <th scope="col" className="num">Clicks</th>
                </tr>
              </thead>
              <tbody>
                {breakdown.map((link) => (
                  <tr key={link.id}>
                    <td data-label="Short link">
                      <a href={api.shortUrl(link.shortCode)} target="_blank" rel="noreferrer" className="short-link">
                        {link.shortCode}
                      </a>
                    </td>
                    <td data-label="Destination" className="destination" title={link.longUrl}>
                      <span>{displayUrl(link.longUrl)}</span>
                    </td>
                    <td data-label="Clicks" className="num">{formatCount(link.clickCount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
