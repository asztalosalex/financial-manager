import type { CategoryReportItem } from '../api/types'

export interface CategoryColor {
  categoryId: number | null
  categoryName: string
  total: number
  percentage: number | null
  color: string
}

export interface DonutStop {
  color: string
  startPercent: number
  endPercent: number
}

export function formatMonthLabel(monthValue: string): string {
  const [yearPart, monthPart] = monthValue.split('-')
  const year = Number(yearPart)
  const month = Number(monthPart)
  const date = new Date(year, month - 1, 1)
  return new Intl.DateTimeFormat('en-US', { month: 'short' }).format(date)
}

export function computeBarHeightPx(value: number, max: number, maxPx = 140): number {
  if (max <= 0) {
    return 0
  }
  return (value / max) * maxPx
}

export function colorizeCategories(
  categories: CategoryReportItem[],
  palette: string[],
): CategoryColor[] {
  return categories.map((category, index) => ({
    categoryId: category.categoryId,
    categoryName: category.categoryName,
    total: category.total,
    percentage: category.percentage,
    color: palette[index % palette.length],
  }))
}

export function buildDonutSlices(
  categories: CategoryReportItem[],
  palette: string[],
  otherColor: string,
  maxSlices = 5,
): CategoryColor[] {
  if (categories.length <= maxSlices) {
    return colorizeCategories(categories, palette)
  }

  const head = colorizeCategories(categories.slice(0, maxSlices), palette)
  const rest = categories.slice(maxSlices)
  const otherTotal = rest.reduce((sum, category) => sum + category.total, 0)
  const otherPercentage = rest.reduce((sum, category) => sum + (category.percentage ?? 0), 0)

  return [
    ...head,
    {
      categoryId: null,
      categoryName: 'Other',
      total: otherTotal,
      percentage: otherPercentage,
      color: otherColor,
    },
  ]
}

export function buildDonutStops(slices: CategoryColor[]): DonutStop[] {
  const stops: DonutStop[] = []
  let cumulative = 0

  slices.forEach((slice, index) => {
    const startPercent = cumulative
    cumulative += slice.percentage ?? 0
    const isLast = index === slices.length - 1
    stops.push({
      color: slice.color,
      startPercent,
      endPercent: isLast ? 100 : cumulative,
    })
  })

  return stops
}

export function buildConicGradient(stops: DonutStop[]): string {
  const segments = stops.map((stop) => `${stop.color} ${stop.startPercent}% ${stop.endPercent}%`)
  return `conic-gradient(${segments.join(', ')})`
}
