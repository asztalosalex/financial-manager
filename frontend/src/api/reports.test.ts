import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchReportsSummary } from './reports'
import { ApiError } from './ApiError'
import { setUnauthorizedHandler } from './client'
import { errorResponse, jsonResponse } from '../test/helpers'
import type { ReportsSummaryResponse } from './types'

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
})
