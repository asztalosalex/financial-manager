import { describe, expect, it } from 'vitest'
import {
  buildConicGradient,
  buildDonutSlices,
  buildDonutStops,
  colorizeCategories,
  computeBarHeightPx,
  formatMonthLabel,
} from './charts'
import type { CategoryReportItem } from '../api/types'

const PALETTE = ['red', 'green', 'blue', 'amber', 'violet']
const OTHER_COLOR = 'grey'

function category(
  categoryId: number,
  categoryName: string,
  total: number,
  percentage: number | null,
): CategoryReportItem {
  return { categoryId, categoryName, total, percentage }
}

describe('formatMonthLabel', () => {
  it('formats a YYYY-MM value into a short English month name', () => {
    expect(formatMonthLabel('2026-03')).toBe('Mar')
  })

  it('formats December correctly', () => {
    expect(formatMonthLabel('2025-12')).toBe('Dec')
  })

  it('formats January correctly', () => {
    expect(formatMonthLabel('2026-01')).toBe('Jan')
  })
})

describe('computeBarHeightPx', () => {
  it('returns 0 when max is 0, never NaN', () => {
    expect(computeBarHeightPx(0, 0)).toBe(0)
    expect(computeBarHeightPx(100, 0)).toBe(0)
    expect(Number.isNaN(computeBarHeightPx(100, 0))).toBe(false)
  })

  it('returns 0 when max is negative', () => {
    expect(computeBarHeightPx(100, -5)).toBe(0)
  })

  it('scales the value proportionally against max using the default maxPx of 140', () => {
    expect(computeBarHeightPx(50, 100)).toBe(70)
    expect(computeBarHeightPx(100, 100)).toBe(140)
  })

  it('honors a custom maxPx', () => {
    expect(computeBarHeightPx(50, 100, 200)).toBe(100)
  })
})

describe('colorizeCategories', () => {
  it('keeps every category and assigns colors by index modulo palette length', () => {
    const categories = [
      category(1, 'Food', 100, 40),
      category(2, 'Rent', 150, 60),
    ]

    const result = colorizeCategories(categories, PALETTE)

    expect(result).toHaveLength(2)
    expect(result[0]).toEqual({
      categoryId: 1,
      categoryName: 'Food',
      total: 100,
      percentage: 40,
      color: 'red',
    })
    expect(result[1].color).toBe('green')
  })

  it('wraps the palette when there are more categories than colors', () => {
    const categories = Array.from({ length: 7 }, (_, i) =>
      category(i, `Cat ${i}`, 10, 10),
    )

    const result = colorizeCategories(categories, PALETTE)

    expect(result).toHaveLength(7)
    expect(result[0].color).toBe('red')
    expect(result[5].color).toBe('red')
    expect(result[6].color).toBe('green')
  })
})

describe('buildDonutSlices', () => {
  it('returns the same result as colorizeCategories when there are 5 or fewer categories', () => {
    const categories = [
      category(1, 'Food', 100, 50),
      category(2, 'Rent', 100, 50),
    ]

    expect(buildDonutSlices(categories, PALETTE, OTHER_COLOR)).toEqual(
      colorizeCategories(categories, PALETTE),
    )
  })

  it('collapses categories beyond the top 5 into a single Other bucket', () => {
    const categories = [
      category(1, 'A', 10, 10),
      category(2, 'B', 10, 10),
      category(3, 'C', 10, 10),
      category(4, 'D', 10, 10),
      category(5, 'E', 10, 10),
      category(6, 'F', 10, 10),
      category(7, 'G', 10, 10),
    ]

    const result = buildDonutSlices(categories, PALETTE, OTHER_COLOR)

    expect(result).toHaveLength(6)
    expect(result.slice(0, 5).map((s) => s.categoryName)).toEqual(['A', 'B', 'C', 'D', 'E'])

    const other = result[5]
    expect(other.categoryId).toBeNull()
    expect(other.categoryName).toBe('Other')
    expect(other.color).toBe(OTHER_COLOR)
  })

  it('sums total and percentage across all remaining categories in the Other bucket, not just the first', () => {
    const categories = [
      category(1, 'A', 10, 10),
      category(2, 'B', 10, 10),
      category(3, 'C', 10, 10),
      category(4, 'D', 10, 10),
      category(5, 'E', 10, 10),
      category(6, 'F', 20, 15),
      category(7, 'G', 30, 20),
      category(8, 'H', 40, 25),
    ]

    const result = buildDonutSlices(categories, PALETTE, OTHER_COLOR)
    const other = result[result.length - 1]

    expect(other.total).toBe(90)
    expect(other.percentage).toBe(60)
  })

  it('treats a null percentage as 0 when summing the Other bucket', () => {
    const categories = [
      category(1, 'A', 10, 10),
      category(2, 'B', 10, 10),
      category(3, 'C', 10, 10),
      category(4, 'D', 10, 10),
      category(5, 'E', 10, 10),
      category(6, 'F', 20, null),
    ]

    const result = buildDonutSlices(categories, PALETTE, OTHER_COLOR)
    const other = result[result.length - 1]

    expect(other.total).toBe(20)
    expect(other.percentage).toBe(0)
  })
})

describe('buildDonutStops', () => {
  it('builds cumulative start/end boundaries from slice percentages', () => {
    const slices = [
      { categoryId: 1, categoryName: 'A', total: 10, percentage: 30, color: 'red' },
      { categoryId: 2, categoryName: 'B', total: 10, percentage: 30, color: 'green' },
      { categoryId: 3, categoryName: 'C', total: 10, percentage: 40, color: 'blue' },
    ]

    const stops = buildDonutStops(slices)

    expect(stops).toEqual([
      { color: 'red', startPercent: 0, endPercent: 30 },
      { color: 'green', startPercent: 30, endPercent: 60 },
      { color: 'blue', startPercent: 60, endPercent: 100 },
    ])
  })

  it('forces the last endPercent to exactly 100 even with a rounding remainder', () => {
    const slices = [
      { categoryId: 1, categoryName: 'A', total: 10, percentage: 33.3, color: 'red' },
      { categoryId: 2, categoryName: 'B', total: 10, percentage: 33.3, color: 'green' },
      { categoryId: 3, categoryName: 'C', total: 10, percentage: 33.3, color: 'blue' },
    ]

    const stops = buildDonutStops(slices)

    expect(stops[stops.length - 1].endPercent).toBe(100)
  })

  it('treats a null percentage as 0 in the cumulative sum', () => {
    const slices = [
      { categoryId: 1, categoryName: 'A', total: 10, percentage: null, color: 'red' },
      { categoryId: 2, categoryName: 'B', total: 10, percentage: 40, color: 'green' },
    ]

    const stops = buildDonutStops(slices)

    expect(stops[0]).toEqual({ color: 'red', startPercent: 0, endPercent: 0 })
    expect(stops[1]).toEqual({ color: 'green', startPercent: 0, endPercent: 100 })
  })

  it('returns an empty array for an empty slice list', () => {
    expect(buildDonutStops([])).toEqual([])
  })
})

describe('buildConicGradient', () => {
  it('builds a conic-gradient string from the given stops', () => {
    const stops = [
      { color: 'red', startPercent: 0, endPercent: 40 },
      { color: 'green', startPercent: 40, endPercent: 100 },
    ]

    expect(buildConicGradient(stops)).toBe('conic-gradient(red 0% 40%, green 40% 100%)')
  })

  it('returns an empty conic-gradient call for an empty stop list', () => {
    expect(buildConicGradient([])).toBe('conic-gradient()')
  })
})
