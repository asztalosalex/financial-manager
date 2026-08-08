import { api } from './client'
import type {
  BudgetStatusResponse,
  CategoryReportResponse,
  ReportsSummaryResponse,
  TrendReportResponse,
} from './types'

export interface ReportsSummaryQuery {
  month?: string
}

export interface ReportsCategoriesQuery {
  month?: string
}

export interface ReportsTrendQuery {
  month?: string
  months?: number
}

export interface ReportsBudgetStatusQuery {
  month?: string
}

const REPORTS_SUMMARY_PATH = '/api/reports/summary'
const REPORTS_CATEGORIES_PATH = '/api/reports/categories'
const REPORTS_TREND_PATH = '/api/reports/trend'
const REPORTS_BUDGET_STATUS_PATH = '/api/reports/budget-status'

function appendParam(
  params: URLSearchParams,
  name: string,
  value: string | number | undefined,
): void {
  if (value === undefined) {
    return
  }
  if (typeof value === 'number') {
    if (!Number.isFinite(value)) {
      return
    }
    params.set(name, String(value))
    return
  }
  if (value.trim().length === 0) {
    return
  }
  params.set(name, value)
}

function serializeParams(params: URLSearchParams): string {
  const serialized = params.toString()
  return serialized.length === 0 ? '' : `?${serialized}`
}

function buildSummaryQueryString(query: ReportsSummaryQuery): string {
  const params = new URLSearchParams()
  appendParam(params, 'month', query.month)
  return serializeParams(params)
}

function buildCategoriesQueryString(query: ReportsCategoriesQuery): string {
  const params = new URLSearchParams()
  appendParam(params, 'month', query.month)
  return serializeParams(params)
}

function buildTrendQueryString(query: ReportsTrendQuery): string {
  const params = new URLSearchParams()
  appendParam(params, 'month', query.month)
  appendParam(params, 'months', query.months)
  return serializeParams(params)
}

function buildBudgetStatusQueryString(query: ReportsBudgetStatusQuery): string {
  const params = new URLSearchParams()
  appendParam(params, 'month', query.month)
  return serializeParams(params)
}

export function fetchReportsSummary(
  query: ReportsSummaryQuery = {},
  signal?: AbortSignal,
): Promise<ReportsSummaryResponse> {
  return api.get<ReportsSummaryResponse>(
    `${REPORTS_SUMMARY_PATH}${buildSummaryQueryString(query)}`,
    { signal },
  )
}

export function fetchReportsCategories(
  query: ReportsCategoriesQuery = {},
  signal?: AbortSignal,
): Promise<CategoryReportResponse> {
  return api.get<CategoryReportResponse>(
    `${REPORTS_CATEGORIES_PATH}${buildCategoriesQueryString(query)}`,
    { signal },
  )
}

export function fetchReportsTrend(
  query: ReportsTrendQuery = {},
  signal?: AbortSignal,
): Promise<TrendReportResponse> {
  return api.get<TrendReportResponse>(
    `${REPORTS_TREND_PATH}${buildTrendQueryString(query)}`,
    { signal },
  )
}

export function fetchReportsBudgetStatus(
  query: ReportsBudgetStatusQuery = {},
  signal?: AbortSignal,
): Promise<BudgetStatusResponse> {
  return api.get<BudgetStatusResponse>(
    `${REPORTS_BUDGET_STATUS_PATH}${buildBudgetStatusQueryString(query)}`,
    { signal },
  )
}
