import { describe, expect, it } from 'vitest'
import {
  computeDeltaTone,
  formatCurrencyHuf,
  formatHeaderDate,
  formatPercent,
  formatSignedPercent,
  formatSignedPoints,
  formatTransactionDate,
  formatTransactionListDate,
} from './format'

describe('formatCurrencyHuf', () => {
  it('rounds to the nearest whole forint', () => {
    expect(formatCurrencyHuf(1248500.4)).toBe(`${(1248500).toLocaleString('hu-HU')} Ft`)
    expect(formatCurrencyHuf(1248500.6)).toBe(`${(1248501).toLocaleString('hu-HU')} Ft`)
  })

  it('groups thousands using the hu-HU locale and appends Ft', () => {
    expect(formatCurrencyHuf(1248500)).toBe(`${(1248500).toLocaleString('hu-HU')} Ft`)
  })

  it('handles zero', () => {
    expect(formatCurrencyHuf(0)).toBe('0 Ft')
  })

  it('handles negative values', () => {
    expect(formatCurrencyHuf(-500)).toBe(`${(-500).toLocaleString('hu-HU')} Ft`)
  })
})

describe('formatPercent', () => {
  it('formats with exactly one decimal and no sign', () => {
    expect(formatPercent(30.7)).toBe('30.7%')
    expect(formatPercent(30)).toBe('30.0%')
  })
})

describe('formatSignedPercent', () => {
  it('prefixes positive values with a plus sign', () => {
    expect(formatSignedPercent(4.2)).toBe('+4.2%')
  })

  it('prefixes negative values with a minus sign character', () => {
    expect(formatSignedPercent(-8.6)).toBe('−8.6%')
    expect(formatSignedPercent(-8.6)).not.toMatch('-8.6%')
  })

  it('shows no sign for exactly zero', () => {
    expect(formatSignedPercent(0)).toBe('0.0%')
  })

  it('shows no sign for a value that rounds to zero', () => {
    expect(formatSignedPercent(0.04)).toBe('0.0%')
    expect(formatSignedPercent(-0.04)).toBe('0.0%')
  })
})

describe('formatSignedPoints', () => {
  it('uses pts instead of percent', () => {
    expect(formatSignedPoints(4.2)).toBe('+4.2pts')
    expect(formatSignedPoints(-1.3)).toBe('−1.3pts')
    expect(formatSignedPoints(0)).toBe('0.0pts')
  })
})

describe('formatHeaderDate', () => {
  it('formats as an english long date with weekday', () => {
    expect(formatHeaderDate(new Date(2026, 7, 14))).toBe('Friday, August 14, 2026')
  })

  it('never uses hungarian formatting', () => {
    const result = formatHeaderDate(new Date(2026, 0, 1))
    expect(result).not.toMatch(/\./)
  })
})

describe('formatTransactionDate', () => {
  const today = new Date(2026, 7, 16)

  it('labels a transaction dated today as Today', () => {
    expect(formatTransactionDate('2026-08-16', today)).toBe('Today')
  })

  it('labels a transaction dated the day before today as Yesterday', () => {
    expect(formatTransactionDate('2026-08-15', today)).toBe('Yesterday')
  })

  it('formats a same-year date that is neither today nor yesterday as short month and day', () => {
    expect(formatTransactionDate('2026-08-01', today)).toBe('Aug 1')
  })

  it('formats a same-year date further in the past without a year', () => {
    expect(formatTransactionDate('2026-01-05', today)).toBe('Jan 5')
  })

  it('includes the year for a date in a prior year', () => {
    expect(formatTransactionDate('2025-08-25', today)).toBe('Aug 25, 2025')
  })

  it('never labels a prior-year date as Today or Yesterday', () => {
    expect(formatTransactionDate('2025-08-16', today)).not.toBe('Today')
    expect(formatTransactionDate('2025-08-15', today)).not.toBe('Yesterday')
  })

  it('resolves Yesterday correctly across a year boundary', () => {
    const newYearsDay = new Date(2026, 0, 1)
    expect(formatTransactionDate('2025-12-31', newYearsDay)).toBe('Yesterday')
  })

  it('parses the raw date as a local calendar day, not as UTC', () => {
    const lastDayOfMonth = new Date(2026, 7, 31)
    expect(formatTransactionDate('2026-08-30', lastDayOfMonth)).toBe('Yesterday')
    expect(formatTransactionDate('2026-08-01', lastDayOfMonth)).toBe('Aug 1')
  })

  it('accepts the reference date as an explicit parameter rather than reading the system clock', () => {
    const arbitraryToday = new Date(2030, 2, 10)
    expect(formatTransactionDate('2030-03-10', arbitraryToday)).toBe('Today')
    expect(formatTransactionDate('2030-03-09', arbitraryToday)).toBe('Yesterday')
  })
})

describe('formatTransactionListDate', () => {
  it('formats a date with abbreviated month, numeric day and full year', () => {
    expect(formatTransactionListDate('2026-08-14')).toBe('Aug 14, 2026')
  })

  it('always includes the year, even for the current calendar year', () => {
    const now = new Date()
    const isoThisYear = `${now.getFullYear()}-01-15`
    expect(formatTransactionListDate(isoThisYear)).toBe(
      new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(
        new Date(now.getFullYear(), 0, 15),
      ),
    )
  })

  it('never returns a relative label like Today or Yesterday, even for the current date', () => {
    const today = new Date()
    const isoToday = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(
      today.getDate(),
    ).padStart(2, '0')}`
    expect(formatTransactionListDate(isoToday)).not.toBe('Today')
    expect(formatTransactionListDate(isoToday)).not.toBe('Yesterday')
  })

  it('parses the raw date as a local calendar day, not as UTC', () => {
    expect(formatTransactionListDate('2026-01-01')).toBe('Jan 1, 2026')
    expect(formatTransactionListDate('2025-12-31')).toBe('Dec 31, 2025')
  })
})

describe('computeDeltaTone', () => {
  it('is neutral when delta is null', () => {
    expect(computeDeltaTone(null, true)).toBe('neutral')
    expect(computeDeltaTone(null, false)).toBe('neutral')
  })

  it('is neutral when delta is exactly zero', () => {
    expect(computeDeltaTone(0, true)).toBe('neutral')
    expect(computeDeltaTone(0, false)).toBe('neutral')
  })

  it('is positive when the metric increases and higher is better', () => {
    expect(computeDeltaTone(4.2, true)).toBe('positive')
  })

  it('is negative when the metric decreases and higher is better', () => {
    expect(computeDeltaTone(-4.2, true)).toBe('negative')
  })

  it('is negative when the metric increases and higher is worse (the expense trap)', () => {
    expect(computeDeltaTone(8.6, false)).toBe('negative')
  })

  it('is positive when the metric decreases and higher is worse', () => {
    expect(computeDeltaTone(-8.6, false)).toBe('positive')
  })
})
