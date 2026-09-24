import { describe, expect, it } from 'vitest';
import { fillDays, niceTicks } from './chart';

describe('niceTicks', () => {
  it('uses clean 1/2/5 steps that cover the max', () => {
    expect(niceTicks(25)).toEqual([0, 10, 20, 30]);
    expect(niceTicks(7)).toEqual([0, 2, 4, 6, 8]);
    expect(niceTicks(1300)).toEqual([0, 500, 1000, 1500]);
  });

  it('never produces fractional steps for counts', () => {
    expect(niceTicks(1)).toEqual([0, 1]);
    expect(niceTicks(3)).toEqual([0, 1, 2, 3]);
  });

  it('handles no data', () => {
    expect(niceTicks(0)).toEqual([0, 1]);
  });
});

describe('fillDays', () => {
  it('fills every day in the window, ending today, with zeros for missing days', () => {
    const today = new Date(2026, 8, 24); // Sep 24, 2026
    const result = fillDays(
      [
        { day: '2026-09-22', clicks: 4 },
        { day: '2026-09-24', clicks: 6 },
        { day: '2026-08-01', clicks: 99 }, // outside the window
      ],
      3,
      today,
    );
    expect(result).toEqual([
      { day: '2026-09-22', clicks: 4 },
      { day: '2026-09-23', clicks: 0 },
      { day: '2026-09-24', clicks: 6 },
    ]);
  });

  it('crosses month and year boundaries', () => {
    const result = fillDays([], 3, new Date(2027, 0, 1));
    expect(result.map((d) => d.day)).toEqual(['2026-12-30', '2026-12-31', '2027-01-01']);
  });
});
