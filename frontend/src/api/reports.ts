import { api } from './client'
import type { ReportsSummaryResponse } from './types'

export interface ReportsSummaryQuery {
  month?: string
}

const REPORTS_SUMMARY_PATH = '/api/reports/summary'

function appendParam(
  params: URLSearchParams,
  name: string,
  value: string | undefined,
): void {
  if (value === undefined) {
    return
  }
  if (value.trim().length === 0) {
    return
  }
  params.set(name, value)
}

function buildQueryString(query: ReportsSummaryQuery): string {
  const params = new URLSearchParams()
  appendParam(params, 'month', query.month)

  const serialized = params.toString()
  return serialized.length === 0 ? '' : `?${serialized}`
}

export function fetchReportsSummary(
  query: ReportsSummaryQuery = {},
  signal?: AbortSignal,
): Promise<ReportsSummaryResponse> {
  return api.get<ReportsSummaryResponse>(
    `${REPORTS_SUMMARY_PATH}${buildQueryString(query)}`,
    { signal },
  )
}
