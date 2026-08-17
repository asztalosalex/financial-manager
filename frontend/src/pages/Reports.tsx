import { useEffect, useState } from 'react'
import StatCard from '../components/dashboard/StatCard'
import TrendChart, { type TrendChartPoint } from '../components/dashboard/TrendChart'
import CategoryDonut, {
  type DonutLegendItem,
} from '../components/dashboard/CategoryDonut'
import MonthPicker from '../components/reports/MonthPicker'
import BudgetStatusList, {
  type BudgetStatusRowItem,
} from '../components/reports/BudgetStatusList'
import {
  fetchReportsBudgetStatus,
  fetchReportsCategories,
  fetchReportsSummary,
  fetchReportsTrend,
} from '../api/reports'
import { isAbortError, toFormError } from '../api/ApiError'
import {
  buildDonutSlices,
  buildDonutStops,
  computeBarHeightPx,
  formatMonthLabel,
  type DonutStop,
} from '../lib/charts'
import {
  computeDeltaTone,
  formatBudgetMonthLabel,
  formatCurrencyHuf,
  formatPercent,
  formatSignedPercent,
  formatSignedPoints,
} from '../lib/format'
import type {
  BudgetStatusResponse,
  CategoryReportResponse,
  ReportsSummaryResponse,
  TrendReportResponse,
} from '../api/types'

const CATEGORY_PALETTE = [
  'var(--accent)',
  'var(--success)',
  'var(--danger)',
  'var(--cat-amber)',
  'var(--cat-violet)',
]
const OTHER_COLOR = 'var(--neutral-strong)'

const ICON_PROPS = {
  width: 14,
  height: 14,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2.2,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
} as const

function BalanceIcon() {
  return (
    <svg {...ICON_PROPS}>
      <rect x="1" y="4" width="22" height="16" rx="2" ry="2" />
      <line x1="1" y1="10" x2="23" y2="10" />
    </svg>
  )
}

function IncomeIcon() {
  return (
    <svg {...ICON_PROPS}>
      <polyline points="23 6 13.5 15.5 8.5 10.5 1 18" />
      <polyline points="17 6 23 6 23 12" />
    </svg>
  )
}

function ExpenseIcon() {
  return (
    <svg {...ICON_PROPS}>
      <polyline points="23 18 13.5 8.5 8.5 13.5 1 6" />
      <polyline points="17 18 23 18 23 12" />
    </svg>
  )
}

function SavingsRateIcon() {
  return (
    <svg {...ICON_PROPS}>
      <line x1="19" y1="5" x2="5" y2="19" />
      <circle cx="6.5" cy="6.5" r="2.5" />
      <circle cx="17.5" cy="17.5" r="2.5" />
    </svg>
  )
}

function currentMonthIso(): string {
  return new Date().toISOString().slice(0, 7)
}

function buildDeltaText(delta: number | null, formatter: (value: number) => string): string | null {
  if (delta === null) {
    return null
  }
  return `${formatter(delta)} vs last month`
}

function isTrendEmpty(trend: TrendReportResponse): boolean {
  return trend.points.every((point) => point.income === 0 && point.expense === 0)
}

function buildTrendPoints(trend: TrendReportResponse): TrendChartPoint[] {
  const max = trend.points.reduce(
    (acc, point) => Math.max(acc, point.income, point.expense),
    0,
  )
  return trend.points.map((point) => ({
    monthLabel: formatMonthLabel(point.month),
    incomeHeightPx: computeBarHeightPx(point.income, max),
    expenseHeightPx: computeBarHeightPx(point.expense, max),
    incomeLabel: formatCurrencyHuf(point.income),
    expenseLabel: formatCurrencyHuf(point.expense),
  }))
}

function buildDonutStopsProp(categories: CategoryReportResponse): DonutStop[] {
  const slices = buildDonutSlices(categories.categories, CATEGORY_PALETTE, OTHER_COLOR)
  return buildDonutStops(slices)
}

function buildDonutLegend(categories: CategoryReportResponse): DonutLegendItem[] {
  const slices = buildDonutSlices(categories.categories, CATEGORY_PALETTE, OTHER_COLOR)
  return slices.map((slice) => ({
    color: slice.color,
    label: slice.categoryName,
    percentageLabel: slice.percentage === null ? '—' : formatPercent(slice.percentage),
  }))
}

function buildBudgetStatusItems(status: BudgetStatusResponse): BudgetStatusRowItem[] {
  return status.categories.map((item) => {
    const barTone: 'accent' | 'danger' =
      item.percentageUsed !== null && item.percentageUsed > 100 ? 'danger' : 'accent'
    const remainingTone: 'success' | 'danger' = item.remaining >= 0 ? 'success' : 'danger'
    const remainingLabel =
      item.remaining >= 0
        ? `${formatCurrencyHuf(item.remaining)} left`
        : `${formatCurrencyHuf(Math.abs(item.remaining))} over`

    return {
      categoryId: item.categoryId,
      categoryName: item.categoryName,
      budgetedLabel: formatCurrencyHuf(item.budgeted),
      spentLabel: formatCurrencyHuf(item.spent),
      remainingLabel,
      remainingTone,
      percentageLabel: item.percentageUsed === null ? '—' : formatPercent(item.percentageUsed),
      barWidthPercent: Math.min(Math.max(item.percentageUsed ?? 0, 0), 100),
      barTone,
    }
  })
}

function Reports() {
  const [selectedMonth, setSelectedMonth] = useState(currentMonthIso())

  const [summary, setSummary] = useState<ReportsSummaryResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [categoryReport, setCategoryReport] = useState<CategoryReportResponse | null>(null)
  const [trendReport, setTrendReport] = useState<TrendReportResponse | null>(null)
  const [chartsLoading, setChartsLoading] = useState(true)
  const [chartsError, setChartsError] = useState('')

  const [budgetStatus, setBudgetStatus] = useState<BudgetStatusResponse | null>(null)
  const [budgetStatusLoading, setBudgetStatusLoading] = useState(true)
  const [budgetStatusError, setBudgetStatusError] = useState('')

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)

    async function load() {
      try {
        const data = await fetchReportsSummary({ month: selectedMonth }, controller.signal)
        setSummary(data)
        setError('')
      } catch (err) {
        if (isAbortError(err)) {
          return
        }
        setError(toFormError(err).message)
      } finally {
        setLoading(false)
      }
    }

    void load()
    return () => controller.abort()
  }, [selectedMonth])

  useEffect(() => {
    const controller = new AbortController()
    setChartsLoading(true)

    async function load() {
      try {
        const [categories, trend] = await Promise.all([
          fetchReportsCategories({ month: selectedMonth }, controller.signal),
          fetchReportsTrend({ month: selectedMonth, months: 6 }, controller.signal),
        ])
        setCategoryReport(categories)
        setTrendReport(trend)
        setChartsError('')
      } catch (err) {
        if (isAbortError(err)) {
          return
        }
        setChartsError(toFormError(err).message)
      } finally {
        setChartsLoading(false)
      }
    }

    void load()
    return () => controller.abort()
  }, [selectedMonth])

  useEffect(() => {
    const controller = new AbortController()
    setBudgetStatusLoading(true)

    async function load() {
      try {
        const data = await fetchReportsBudgetStatus({ month: selectedMonth }, controller.signal)
        setBudgetStatus(data)
        setBudgetStatusError('')
      } catch (err) {
        if (isAbortError(err)) {
          return
        }
        setBudgetStatusError(toFormError(err).message)
      } finally {
        setBudgetStatusLoading(false)
      }
    }

    void load()
    return () => controller.abort()
  }, [selectedMonth])

  return (
    <div className="shell-page">
      <div className="shell-page-header">
        <h1>Reports</h1>
        <MonthPicker value={selectedMonth} onChange={setSelectedMonth} />
      </div>

      {loading && (
        <div className="loading" role="status">
          Loading your report...
        </div>
      )}

      {!loading && error && (
        <div className="auth-error" role="alert">
          {error}
        </div>
      )}

      {!loading && !error && summary && (
        <div className="stat-grid">
          <StatCard
            label="Balance"
            value={formatCurrencyHuf(summary.balance.current)}
            deltaText={buildDeltaText(summary.balance.deltaPercent, formatSignedPercent)}
            deltaTone={computeDeltaTone(summary.balance.deltaPercent, true)}
            icon={<BalanceIcon />}
            iconVariant="accent"
          />
          <StatCard
            label="Monthly Income"
            value={formatCurrencyHuf(summary.income.current)}
            deltaText={buildDeltaText(summary.income.deltaPercent, formatSignedPercent)}
            deltaTone={computeDeltaTone(summary.income.deltaPercent, true)}
            icon={<IncomeIcon />}
            iconVariant="success"
          />
          <StatCard
            label="Monthly Expense"
            value={formatCurrencyHuf(summary.expense.current)}
            deltaText={buildDeltaText(summary.expense.deltaPercent, formatSignedPercent)}
            deltaTone={computeDeltaTone(summary.expense.deltaPercent, false)}
            icon={<ExpenseIcon />}
            iconVariant="danger"
          />
          <StatCard
            label="Savings Rate"
            value={
              summary.savingsRate.current === null
                ? '—'
                : formatPercent(summary.savingsRate.current)
            }
            deltaText={buildDeltaText(summary.savingsRate.deltaPoints, formatSignedPoints)}
            deltaTone={computeDeltaTone(summary.savingsRate.deltaPoints, true)}
            icon={<SavingsRateIcon />}
            iconVariant="accent"
          />
        </div>
      )}

      {chartsLoading && (
        <div className="loading" role="status">
          Loading your charts...
        </div>
      )}

      {!chartsLoading && chartsError && (
        <div className="auth-error" role="alert">
          {chartsError}
        </div>
      )}

      {!chartsLoading && !chartsError && categoryReport && trendReport && (
        <div className="dashboard-charts">
          <TrendChart points={buildTrendPoints(trendReport)} isEmpty={isTrendEmpty(trendReport)} />
          <CategoryDonut
            stops={buildDonutStopsProp(categoryReport)}
            legend={buildDonutLegend(categoryReport)}
            centerValueLabel={
              categoryReport.categories.length === 0 ? '—' : formatCurrencyHuf(categoryReport.total)
            }
            isEmpty={categoryReport.categories.length === 0}
          />
        </div>
      )}

      {budgetStatusLoading && (
        <div className="loading" role="status">
          Loading your budget status...
        </div>
      )}

      {!budgetStatusLoading && budgetStatusError && (
        <div className="auth-error" role="alert">
          {budgetStatusError}
        </div>
      )}

      {!budgetStatusLoading && !budgetStatusError && budgetStatus && (
        <BudgetStatusList
          items={buildBudgetStatusItems(budgetStatus)}
          totalBudgetedLabel={formatCurrencyHuf(budgetStatus.totalBudgeted)}
          totalSpentLabel={formatCurrencyHuf(budgetStatus.totalSpent)}
          unbudgetedSpendingLabel={
            budgetStatus.unbudgetedSpending > 0
              ? formatCurrencyHuf(budgetStatus.unbudgetedSpending)
              : null
          }
          monthLabel={formatBudgetMonthLabel(selectedMonth)}
          isEmpty={budgetStatus.categories.length === 0}
        />
      )}
    </div>
  )
}

export default Reports
