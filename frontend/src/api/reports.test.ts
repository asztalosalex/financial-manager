import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  fetchReportsBudgetStatus,
  fetchReportsCategories,
  fetchReportsSummary,
  fetchReportsTrend,
} from './reports'
import { ApiError } from './ApiError'
import { setUnauthorizedHandler } from './client'
import { errorResponse, jsonResponse } from '../test/helpers'
import type {
  BudgetStatusResponse,
  CategoryReportResponse,
  ReportsSummaryResponse,
  TrendReportResponse,
} from './types'

const FULL_SUMMARY: ReportsSummaryResponse = {
  month: '2026-08',
  previousMonth: '2026-07',
  balance: { current: 1248500.0, previous: 1198200.0, deltaPercent: 4.2 },
  income: { current: 450000.0, previous: 440700.0, deltaPercent: 2.1 },
  expense: { current: 312000.0, previous: 287300.0, deltaPercent: 8.6 },
  savingsRate: { current: 30.7, previous: 34.8, deltaPoints: -4.1 },
}

const NEW_USER_SUMMARY: ReportsSummaryResponse = {
  month: '2026-08',
  previousMonth: '2026-07',
  balance: { current: 0, previous: 0, deltaPercent: null },
  income: { current: 0, previous: 0, deltaPercent: null },
  expense: { current: 0, previous: 0, deltaPercent: null },
  savingsRate: { current: null, previous: null, deltaPoints: null },
}

const CATEGORY_BREAKDOWN: CategoryReportResponse = {
  month: '2026-08',
  total: 312000.0,
  categories: [
    { categoryId: 4, categoryName: 'Lakhatás', total: 145000.0, percentage: 46.5 },
    { categoryId: 1, categoryName: 'Élelmiszer', total: 92000.0, percentage: 29.5 },
  ],
}

const EMPTY_CATEGORY_BREAKDOWN: CategoryReportResponse = {
  month: '2026-08',
  total: 0,
  categories: [],
}

const CATEGORY_BREAKDOWN_WITH_NULL_PERCENTAGE: CategoryReportResponse = {
  month: '2026-08',
  total: 0,
  categories: [{ categoryId: 4, categoryName: 'Lakhatás', total: 0, percentage: null }],
}

const BUDGET_STATUS: BudgetStatusResponse = {
  month: '2026-08',
  totalBudgeted: 250000.0,
  totalSpent: 198000.0,
  unbudgetedSpending: 42000.0,
  categories: [
    {
      categoryId: 4,
      categoryName: 'Lakhatás',
      budgeted: 150000.0,
      spent: 162000.0,
      remaining: -12000.0,
      percentageUsed: 108.0,
    },
    {
      categoryId: 1,
      categoryName: 'Élelmiszer',
      budgeted: 100000.0,
      spent: 36000.0,
      remaining: 64000.0,
      percentageUsed: 36.0,
    },
  ],
}

const BUDGET_STATUS_WITH_NULL_PERCENTAGE: BudgetStatusResponse = {
  month: '2026-08',
  totalBudgeted: 0,
  totalSpent: 0,
  unbudgetedSpending: 0,
  categories: [
    {
      categoryId: 2,
      categoryName: 'Egyéb',
      budgeted: 0,
      spent: 0,
      remaining: 0,
      percentageUsed: null,
    },
  ],
}

const BUDGET_STATUS_NO_BUDGETS: BudgetStatusResponse = {
  month: '2026-08',
  totalBudgeted: 0,
  totalSpent: 0,
  unbudgetedSpending: 42000.0,
  categories: [],
}

const TREND_WITH_GAP: TrendReportResponse = {
  month: '2026-08',
  months: 6,
  points: [
    { month: '2026-03', income: 430000.0, expense: 298000.0 },
    { month: '2026-04', income: 0, expense: 0 },
    { month: '2026-05', income: 410000.0, expense: 305000.0 },
    { month: '2026-06', income: 420000.0, expense: 310000.0 },
    { month: '2026-07', income: 440700.0, expense: 287300.0 },
    { month: '2026-08', income: 450000.0, expense: 312000.0 },
  ],
}

function respondWith(body: unknown, status = 200) {
  vi.mocked(globalThis.fetch).mockImplementation(() => Promise.resolve(jsonResponse(status, body)))
}

function lastCall() {
  const calls = vi.mocked(globalThis.fetch).mock.calls
  return calls[calls.length - 1]
}

function requestedUrl(): string {
  return String(lastCall()[0])
}

function requestedParams(): URLSearchParams {
  const [, search] = requestedUrl().split('?')
  return new URLSearchParams(search ?? '')
}

describe('reports api', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('requests the bare summary path when no month is given', async () => {
    respondWith(FULL_SUMMARY)

    const summary = await fetchReportsSummary()

    expect(requestedUrl()).toBe('/api/reports/summary')
    const [, init] = lastCall()
    expect(init?.method).toBe('GET')
    expect(init?.credentials).toBe('include')
    expect(summary).toEqual(FULL_SUMMARY)
  })

  it('serializes the month parameter when supplied', async () => {
    respondWith(FULL_SUMMARY)

    await fetchReportsSummary({ month: '2026-08' })

    const params = requestedParams()
    expect(params.get('month')).toBe('2026-08')
    expect([...params.keys()]).toHaveLength(1)
    expect(requestedUrl()).toBe('/api/reports/summary?month=2026-08')
  })

  it('omits a blank month instead of sending it empty', async () => {
    respondWith(FULL_SUMMARY)

    await fetchReportsSummary({ month: '   ' })

    expect(requestedUrl()).toBe('/api/reports/summary')
    expect(requestedUrl()).not.toContain('month=')
  })

  it('returns the metric values exactly as they came off the wire', async () => {
    respondWith(FULL_SUMMARY)

    const summary = await fetchReportsSummary({ month: '2026-08' })

    expect(summary.month).toBe('2026-08')
    expect(summary.previousMonth).toBe('2026-07')
    expect(summary.balance.current).toBe(1248500.0)
    expect(summary.balance.previous).toBe(1198200.0)
    expect(summary.balance.deltaPercent).toBe(4.2)
    expect(summary.income.deltaPercent).toBe(2.1)
    expect(summary.expense.deltaPercent).toBe(8.6)
    expect(summary.savingsRate.current).toBe(30.7)
    expect(summary.savingsRate.previous).toBe(34.8)
    expect(summary.savingsRate.deltaPoints).toBe(-4.1)
  })

  it('surfaces the null delta fields for a new user with no prior activity', async () => {
    respondWith(NEW_USER_SUMMARY)

    const summary = await fetchReportsSummary({ month: '2026-08' })

    expect(summary.balance.current).toBe(0)
    expect(summary.balance.deltaPercent).toBeNull()
    expect(summary.income.deltaPercent).toBeNull()
    expect(summary.expense.deltaPercent).toBeNull()
    expect(summary.savingsRate.current).toBeNull()
    expect(summary.savingsRate.previous).toBeNull()
    expect(summary.savingsRate.deltaPoints).toBeNull()
  })

  it('forwards the abort signal to the request', async () => {
    respondWith(FULL_SUMMARY)
    const controller = new AbortController()

    await fetchReportsSummary({}, controller.signal)

    const [, init] = lastCall()
    expect(init?.signal).toBe(controller.signal)
  })

  it('raises a rejected month parameter as an ApiError with the backend field key', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        errorResponse(400, 'Validation failed', { month: 'month must match YYYY-MM' }, '/api/reports/summary'),
      ),
    )

    const caught = await fetchReportsSummary({ month: 'not-a-month' }).catch((error: unknown) => error)

    expect(caught).toBeInstanceOf(ApiError)
    const apiError = caught as ApiError
    expect(apiError.status).toBe(400)
    expect(apiError.message).toBe('Validation failed')
    expect(apiError.fieldErrors).toEqual({ month: 'month must match YYYY-MM' })
    expect(requestedUrl()).toBe('/api/reports/summary?month=not-a-month')
  })

  it('requests the bare categories path when no month is given', async () => {
    respondWith(CATEGORY_BREAKDOWN)

    const breakdown = await fetchReportsCategories()

    expect(requestedUrl()).toBe('/api/reports/categories')
    expect(breakdown).toEqual(CATEGORY_BREAKDOWN)
  })

  it('serializes the month parameter for the categories query', async () => {
    respondWith(CATEGORY_BREAKDOWN)

    await fetchReportsCategories({ month: '2026-08' })

    expect(requestedUrl()).toBe('/api/reports/categories?month=2026-08')
  })

  it('returns the category breakdown values exactly as they came off the wire', async () => {
    respondWith(CATEGORY_BREAKDOWN)

    const breakdown = await fetchReportsCategories({ month: '2026-08' })

    expect(breakdown.month).toBe('2026-08')
    expect(breakdown.total).toBe(312000.0)
    expect(breakdown.categories).toHaveLength(2)
    expect(breakdown.categories[0]).toEqual({
      categoryId: 4,
      categoryName: 'Lakhatás',
      total: 145000.0,
      percentage: 46.5,
    })
    expect(breakdown.categories[1]).toEqual({
      categoryId: 1,
      categoryName: 'Élelmiszer',
      total: 92000.0,
      percentage: 29.5,
    })
  })

  it('returns an empty category list for a month with no expenses', async () => {
    respondWith(EMPTY_CATEGORY_BREAKDOWN)

    const breakdown = await fetchReportsCategories({ month: '2026-08' })

    expect(breakdown.total).toBe(0)
    expect(breakdown.categories).toEqual([])
  })

  it('raises a rejected categories month parameter as an ApiError with the backend field key', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        errorResponse(400, 'Validation failed', { month: 'month must match YYYY-MM' }, '/api/reports/categories'),
      ),
    )

    const caught = await fetchReportsCategories({ month: 'not-a-month' }).catch((error: unknown) => error)

    expect(caught).toBeInstanceOf(ApiError)
    const apiError = caught as ApiError
    expect(apiError.status).toBe(400)
    expect(apiError.fieldErrors).toEqual({ month: 'month must match YYYY-MM' })
    expect(requestedUrl()).toBe('/api/reports/categories?month=not-a-month')
  })

  it('requests the bare trend path when neither month nor months is given', async () => {
    respondWith(TREND_WITH_GAP)

    const trend = await fetchReportsTrend()

    expect(requestedUrl()).toBe('/api/reports/trend')
    expect(trend).toEqual(TREND_WITH_GAP)
  })

  it('serializes month and months together for the trend query', async () => {
    respondWith(TREND_WITH_GAP)

    await fetchReportsTrend({ month: '2026-08', months: 6 })

    const params = requestedParams()
    expect(params.get('month')).toBe('2026-08')
    expect(params.get('months')).toBe('6')
    expect([...params.keys()]).toHaveLength(2)
  })

  it('sends months=0 in the query string instead of silently defaulting it', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        errorResponse(400, 'Validation failed', { months: 'months must be between 1 and 24' }, '/api/reports/trend'),
      ),
    )

    const caught = await fetchReportsTrend({ month: '2026-08', months: 0 }).catch((error: unknown) => error)

    expect(requestedUrl()).toBe('/api/reports/trend?month=2026-08&months=0')
    expect(caught).toBeInstanceOf(ApiError)
    const apiError = caught as ApiError
    expect(apiError.status).toBe(400)
    expect(apiError.fieldErrors).toEqual({ months: 'months must be between 1 and 24' })
  })

  it('omits months from the query string when not supplied, leaving the backend default of 6', async () => {
    respondWith(TREND_WITH_GAP)

    await fetchReportsTrend({ month: '2026-08' })

    expect(requestedUrl()).toBe('/api/reports/trend?month=2026-08')
    expect(requestedUrl()).not.toContain('months=')
  })

  it('returns a mid-series zero-filled month exactly as received, with points equal to months in length', async () => {
    respondWith(TREND_WITH_GAP)

    const trend = await fetchReportsTrend({ month: '2026-08', months: 6 })

    expect(trend.months).toBe(6)
    expect(trend.points).toHaveLength(6)
    expect(trend.points[0]).toEqual({ month: '2026-03', income: 430000.0, expense: 298000.0 })
    expect(trend.points[1]).toEqual({ month: '2026-04', income: 0, expense: 0 })
    expect(trend.points[5]).toEqual({ month: '2026-08', income: 450000.0, expense: 312000.0 })
  })

  it('raises a rejected months parameter as an ApiError with the backend field key', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        errorResponse(400, 'Validation failed', { months: 'months must be between 1 and 24' }, '/api/reports/trend'),
      ),
    )

    const caught = await fetchReportsTrend({ month: '2026-08', months: 25 }).catch((error: unknown) => error)

    expect(caught).toBeInstanceOf(ApiError)
    const apiError = caught as ApiError
    expect(apiError.status).toBe(400)
    expect(apiError.fieldErrors).toEqual({ months: 'months must be between 1 and 24' })
    expect(requestedUrl()).toBe('/api/reports/trend?month=2026-08&months=25')
  })

  it('returns a null percentage for a category when the month total is zero', async () => {
    respondWith(CATEGORY_BREAKDOWN_WITH_NULL_PERCENTAGE)

    const breakdown = await fetchReportsCategories({ month: '2026-08' })

    expect(breakdown.categories[0].percentage).toBeNull()
  })

  it('requests the bare budget-status path when no month is given', async () => {
    respondWith(BUDGET_STATUS)

    const status = await fetchReportsBudgetStatus()

    expect(requestedUrl()).toBe('/api/reports/budget-status')
    expect(status).toEqual(BUDGET_STATUS)
  })

  it('serializes the month parameter for the budget-status query', async () => {
    respondWith(BUDGET_STATUS)

    await fetchReportsBudgetStatus({ month: '2026-08' })

    const params = requestedParams()
    expect(params.get('month')).toBe('2026-08')
    expect([...params.keys()]).toHaveLength(1)
    expect(requestedUrl()).toBe('/api/reports/budget-status?month=2026-08')
  })

  it('omits a blank month instead of sending it empty for budget-status', async () => {
    respondWith(BUDGET_STATUS)

    await fetchReportsBudgetStatus({ month: '   ' })

    expect(requestedUrl()).toBe('/api/reports/budget-status')
    expect(requestedUrl()).not.toContain('month=')
  })

  it('forwards the abort signal to the budget-status request', async () => {
    respondWith(BUDGET_STATUS)
    const controller = new AbortController()

    await fetchReportsBudgetStatus({}, controller.signal)

    const [, init] = lastCall()
    expect(init?.signal).toBe(controller.signal)
  })

  it('returns an overspent category with negative remaining and percentageUsed above 100', async () => {
    respondWith(BUDGET_STATUS)

    const status = await fetchReportsBudgetStatus({ month: '2026-08' })

    expect(status.totalBudgeted).toBe(250000.0)
    expect(status.totalSpent).toBe(198000.0)
    expect(status.unbudgetedSpending).toBe(42000.0)
    expect(status.categories).toHaveLength(2)
    expect(status.categories[0]).toEqual({
      categoryId: 4,
      categoryName: 'Lakhatás',
      budgeted: 150000.0,
      spent: 162000.0,
      remaining: -12000.0,
      percentageUsed: 108.0,
    })
    expect(status.categories[1]).toEqual({
      categoryId: 1,
      categoryName: 'Élelmiszer',
      budgeted: 100000.0,
      spent: 36000.0,
      remaining: 64000.0,
      percentageUsed: 36.0,
    })
  })

  it('returns a null percentageUsed when a category has no budgeted amount', async () => {
    respondWith(BUDGET_STATUS_WITH_NULL_PERCENTAGE)

    const status = await fetchReportsBudgetStatus({ month: '2026-08' })

    expect(status.categories[0].budgeted).toBe(0)
    expect(status.categories[0].percentageUsed).toBeNull()
  })

  it('returns unbudgeted spending even when there are no budgeted categories', async () => {
    respondWith(BUDGET_STATUS_NO_BUDGETS)

    const status = await fetchReportsBudgetStatus({ month: '2026-08' })

    expect(status.totalBudgeted).toBe(0)
    expect(status.categories).toEqual([])
    expect(status.unbudgetedSpending).toBe(42000.0)
  })

  it('raises a rejected budget-status month parameter as an ApiError with the backend field key', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        errorResponse(
          400,
          'Validation failed',
          { month: 'month must match YYYY-MM' },
          '/api/reports/budget-status',
        ),
      ),
    )

    const caught = await fetchReportsBudgetStatus({ month: 'not-a-month' }).catch((error: unknown) => error)

    expect(caught).toBeInstanceOf(ApiError)
    const apiError = caught as ApiError
    expect(apiError.status).toBe(400)
    expect(apiError.message).toBe('Validation failed')
    expect(apiError.fieldErrors).toEqual({ month: 'month must match YYYY-MM' })
    expect(requestedUrl()).toBe('/api/reports/budget-status?month=not-a-month')
  })
})
