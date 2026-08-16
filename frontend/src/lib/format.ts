export type DeltaTone = 'positive' | 'negative' | 'neutral'

export function formatCurrencyHuf(value: number): string {
  const rounded = Math.round(value)
  return `${rounded.toLocaleString('hu-HU')} Ft`
}

export function formatPercent(value: number): string {
  return `${value.toFixed(1)}%`
}

function formatSigned(value: number, unit: string): string {
  const rounded = Number(value.toFixed(1))
  if (rounded === 0) {
    return `0.0${unit}`
  }
  const sign = rounded > 0 ? '+' : '−'
  return `${sign}${Math.abs(rounded).toFixed(1)}${unit}`
}

export function formatSignedPercent(value: number): string {
  return formatSigned(value, '%')
}

export function formatSignedPoints(value: number): string {
  return formatSigned(value, 'pts')
}

export function formatHeaderDate(date: Date): string {
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  }).format(date)
}

function isSameCalendarDay(a: Date, b: Date): boolean {
  return (
    a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate()
  )
}

export function formatTransactionDate(dateStr: string, today: Date): string {
  const [year, month, day] = dateStr.split('-').map(Number)
  const date = new Date(year, month - 1, day)

  if (isSameCalendarDay(date, today)) {
    return 'Today'
  }

  const yesterday = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1)
  if (isSameCalendarDay(date, yesterday)) {
    return 'Yesterday'
  }

  if (date.getFullYear() === today.getFullYear()) {
    return new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric' }).format(date)
  }

  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(date)
}

export function formatTransactionListDate(dateStr: string): string {
  const [year, month, day] = dateStr.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(date)
}

export function formatBudgetMonthLabel(monthStr: string): string {
  const [year, month] = monthStr.split('-').map(Number)
  const date = new Date(year, month - 1, 1)
  return new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(date)
}

export function computeDeltaTone(delta: number | null, higherIsBetter: boolean): DeltaTone {
  if (delta === null || delta === 0) {
    return 'neutral'
  }
  const isIncrease = delta > 0
  const isGoodChange = isIncrease === higherIsBetter
  return isGoodChange ? 'positive' : 'negative'
}
