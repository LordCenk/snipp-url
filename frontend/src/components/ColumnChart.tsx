import { useEffect, useId, useRef, useState } from 'react';
import { niceTicks } from '../lib/chart';
import { formatCount } from '../lib/format';

export interface Column {
  key: string;
  /** Short axis label, e.g. "Sep 24" */
  label: string;
  /** Full label for the tooltip and table, e.g. "Wed, Sep 24" */
  longLabel: string;
  value: number;
}

const HEIGHT = 220;
const MARGIN = { top: 12, right: 8, bottom: 26, left: 40 };
const MAX_COLUMN = 24;
const RADIUS = 4;

/** Rect with rounded top corners and a square base, growing up from the baseline. */
function columnPath(x: number, y: number, w: number, h: number): string {
  const r = Math.min(RADIUS, h, w / 2);
  return `M${x},${y + h}V${y + r}Q${x},${y} ${x + r},${y}H${x + w - r}Q${x + w},${y} ${x + w},${y + r}V${y + h}Z`;
}

/** Single-series column chart with a hover/focus tooltip on each column. */
export default function ColumnChart({ data, valueLabel }: { data: Column[]; valueLabel: string }) {
  const wrapRef = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(0);
  const [active, setActive] = useState<number | null>(null);
  const titleId = useId();

  useEffect(() => {
    const el = wrapRef.current;
    if (!el) return;
    const observer = new ResizeObserver(([entry]) => setWidth(Math.floor(entry.contentRect.width)));
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const max = Math.max(0, ...data.map((d) => d.value));
  const ticks = niceTicks(max);
  const top = ticks[ticks.length - 1];
  const plotW = Math.max(0, width - MARGIN.left - MARGIN.right);
  const plotH = HEIGHT - MARGIN.top - MARGIN.bottom;
  const band = data.length ? plotW / data.length : 0;
  const colW = Math.max(2, Math.min(MAX_COLUMN, band * 0.62));
  const y = (v: number) => MARGIN.top + plotH - (v / top) * plotH;
  const labelEvery = Math.max(1, Math.ceil(data.length / Math.max(1, Math.floor(plotW / 64))));

  const activeDatum = active !== null ? data[active] : null;
  const tooltipX = active !== null ? MARGIN.left + band * active + band / 2 : 0;

  return (
    <div className="chart" ref={wrapRef}>
      {width > 0 && (
        <svg width={width} height={HEIGHT} role="img" aria-labelledby={titleId} onPointerLeave={() => setActive(null)}>
          <title id={titleId}>{valueLabel} per day</title>
          {ticks.map((t) => (
            <g key={t}>
              <line
                className={t === 0 ? 'chart-baseline' : 'chart-grid'}
                x1={MARGIN.left}
                x2={width - MARGIN.right}
                y1={y(t)}
                y2={y(t)}
              />
              <text className="chart-tick" x={MARGIN.left - 8} y={y(t)} dy="0.32em" textAnchor="end">
                {formatCount(t)}
              </text>
            </g>
          ))}

          {data.map((d, i) => {
            const cx = MARGIN.left + band * i + band / 2;
            const h = y(0) - y(d.value);
            return (
              <g key={d.key}>
                {d.value > 0 && (
                  <path
                    className={`chart-column${active === i ? ' is-active' : ''}`}
                    d={columnPath(cx - colW / 2, y(d.value), colW, h)}
                  />
                )}
                {/* The whole band is the hit target, not just the painted column */}
                <rect
                  className="chart-hit"
                  x={MARGIN.left + band * i}
                  y={MARGIN.top}
                  width={band}
                  height={plotH}
                  tabIndex={0}
                  aria-label={`${d.longLabel}: ${formatCount(d.value)} ${valueLabel.toLowerCase()}`}
                  onPointerEnter={() => setActive(i)}
                  onFocus={() => setActive(i)}
                  onBlur={() => setActive(null)}
                />
                {/* Thin the axis labels to fit; always label the latest day, skipping a neighbour that would collide */}
                {(i === data.length - 1 || (i % labelEvery === 0 && data.length - 1 - i >= labelEvery / 2)) && (
                  <text className="chart-tick" x={cx} y={HEIGHT - 8} textAnchor="middle">
                    {d.label}
                  </text>
                )}
              </g>
            );
          })}
        </svg>
      )}
      {activeDatum && (
        <div
          className="chart-tooltip"
          role="presentation"
          style={{
            left: Math.min(Math.max(tooltipX, 70), width - 70),
            top: Math.max(0, y(activeDatum.value) - 12),
          }}
        >
          <strong>{formatCount(activeDatum.value)}</strong>
          <span>{activeDatum.longLabel}</span>
        </div>
      )}
    </div>
  );
}
