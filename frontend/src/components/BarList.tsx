import type { Share } from '../api/client';

/**
 * Horizontal bars for shares of a whole (devices, referrers). One hue: the row
 * label carries identity, and each bar is labelled with its value at the tip.
 */
export default function BarList({ items, emptyText }: { items: Share[]; emptyText: string }) {
  if (items.length === 0) return <p className="empty">{emptyText}</p>;
  const max = Math.max(...items.map((i) => i.percentage), 1);
  return (
    <ul className="barlist">
      {items.map((item) => (
        <li key={item.name} className="barlist-row" tabIndex={0} aria-label={`${item.name}: ${item.percentage}%`}>
          <span className="barlist-name" title={item.name}>{item.name}</span>
          <span className="barlist-track" aria-hidden="true">
            <span className="barlist-bar" style={{ width: `${Math.max(1.5, (item.percentage / max) * 100)}%` }} />
            <span className="barlist-value">{item.percentage}%</span>
          </span>
        </li>
      ))}
    </ul>
  );
}
