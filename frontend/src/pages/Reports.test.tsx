import type { ReactElement } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Reports from './Reports'
import { clearCookies, errorResponse, jsonResponse } from '../test/helpers'
import type {
  BudgetStatusResponse,
  CategoryReportResponse,
  ReportsSummaryResponse,
  TrendReportResponse,
} from '../api/types'

type FetchHandler = (url: string, init: RequestInit | undefined) => Response

function renderWithRouter(ui: ReactElement) {
  return render(<MemoryRouter>{ui}</MemoryRouter>)
}

function paramsOf(url: string): URLSearchParams {
  return new URL(url, 'http://localhost').searchParams
}

function huf(value: number): string {
  return `${value.toLocaleString('hu-HU')} Ft`.replace(/\u00A0/g, ' ')
}

function mockFetch(handler: FetchHandler) {
  vi.mocked(globalThis.fetch).mockImplementation((input, init) =>
    Promise.resolve(handler(String(input), init)),
  )
}

function currentMonthIso(): string {
  return new Date().toISOString().slice(0, 7)
}

const FULL_SUMMARY: ReportsSummaryResponse = {
  month: '2026-08',
  previousMonth: '2026-07',
  balance: { current: 1248500, previous: 1198000, deltaPercent: 4.2 },
  income: { current: 500000, previous: 489000, deltaPercent: 2.1 },
  expense: { current: 300000, previous: 276000, deltaPercent: 8.6 },
  savingsRate: { current: 40, previous: 41.3, deltaPoints: -1.3 },
}

const OTHER_MONTH_SUMMARY: ReportsSummaryResponse = {
  month: '2026-05',
  previousMonth: '2026-04',
  balance: { current: 999000, previous: 900000, deltaPercent: 11 },
  income: { current: 400000, previous: 380000, deltaPercent: 5.2 },
  expense: { current: 200000, previous: 210000, deltaPercent: -4.7 },
  savingsRate: { current: 50, previous: 44.7, deltaPoints: 5.3 },
}

const EMPTY_CATEGORY_REPORT: CategoryReportResponse = {
  month: '2026-08',
  total: 0,
  categories: [],
}

const FULL_CATEGORY_REPORT: CategoryReportResponse = {
  month: '2026-08',
  total: 300000,
  categories: [
    { categoryId: 1, categoryName: 'Housing', total: 150000, percentage: 50 },
    { categoryId: 2, categoryName: 'Food', total: 90000, percentage: 30 },
    { categoryId: 3, categoryName: 'Transport', total: 60000, percentage: 20 },
  ],
}

const ZERO_TREND_REPORT: TrendReportResponse = {
  month: '2026-08',
  months: 6,
  points: [
    { month: '2026-03', income: 0, expense: 0 },
    { month: '2026-04', income: 0, expense: 0 },
    { month: '2026-05', income: 0, expense: 0 },
    { month: '2026-06', income: 0, expense: 0 },
    { month: '2026-07', income: 0, expense: 0 },
    { month: '2026-08', income: 0, expense: 0 },
  ],
}

const EMPTY_BUDGET_STATUS: BudgetStatusResponse = {
  month: '2026-08',
  totalBudgeted: 0,
  totalSpent: 0,
  unbudgetedSpending: 0,
  categories: [],
}

const FULL_BUDGET_STATUS: BudgetStatusResponse = {
  month: '2026-08',
  totalBudgeted: 150000,
  totalSpent: 142000,
  unbudgetedSpending: 0,
  categories: [
    {
      categoryId: 2,
      categoryName: 'Food',
      budgeted: 50000,
      spent: 62000,
      remaining: -12000,
      percentageUsed: 124,
    },
    {
      categoryId: 1,
      categoryName: 'Housing',
      budgeted: 100000,
      spent: 80000,
      remaining: 20000,
      percentageUsed: 80,
    },
  ],
}

interface FetchRoutes {
  summary?: () => Response
  categories?: () => Response
  trend?: () => Response
  budgetStatus?: () => Response
}

function mockFetchRouted(routes: FetchRoutes) {
  mockFetch((url) => {
    if (url.includes('/api/reports/categories')) {
      return (routes.categories ?? (() => jsonResponse(200, EMPTY_CATEGORY_REPORT)))()
    }
    if (url.includes('/api/reports/trend')) {
      return (routes.trend ?? (() => jsonResponse(200, ZERO_TREND_REPORT)))()
    }
    if (url.includes('/api/reports/budget-status')) {
      return (routes.budgetStatus ?? (() => jsonResponse(200, EMPTY_BUDGET_STATUS)))()
    }
    return (routes.summary ?? (() => jsonResponse(200, FULL_SUMMARY)))()
  })
}

async function renderLoaded(summary: ReportsSummaryResponse = FULL_SUMMARY) {
  mockFetchRouted({ summary: () => jsonResponse(200, summary) })
  renderWithRouter(<Reports />)
  await screen.findByText('Balance')
  await waitFor(() => {
    expect(document.querySelector('.dashboard-charts')).not.toBeNull()
  })
}

describe('Reports', () => {
  beforeEach(() => {
    clearCookies()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    clearCookies()
  })

  it('defaults the month picker to the real current calendar month', async () => {
    await renderLoaded()

    expect(screen.getByLabelText('Month')).toHaveValue(currentMonthIso())
  })

  it('fetches the summary scoped to the current month on mount, never with an empty query', async () => {
    await renderLoaded()

    const summaryCall = vi
      .mocked(globalThis.fetch)
      .mock.calls.find(([url]) => String(url).includes('/api/reports/summary'))
    expect(summaryCall).toBeDefined()

    const params = paramsOf(String(summaryCall?.[0]))
    expect(params.get('month')).toBe(currentMonthIso())
  })

  it('fetches categories and trend together, scoped to the current month, trend requesting 6 months', async () => {
    await renderLoaded()

    const categoriesCall = vi
      .mocked(globalThis.fetch)
      .mock.calls.find(([url]) => String(url).includes('/api/reports/categories'))
    const trendCall = vi
      .mocked(globalThis.fetch)
      .mock.calls.find(([url]) => String(url).includes('/api/reports/trend'))

    expect(categoriesCall).toBeDefined()
    expect(trendCall).toBeDefined()

    expect(paramsOf(String(categoriesCall?.[0])).get('month')).toBe(currentMonthIso())
    const trendParams = paramsOf(String(trendCall?.[0]))
    expect(trendParams.get('month')).toBe(currentMonthIso())
    expect(trendParams.get('months')).toBe('6')
  })

  it('re-fetches both sections with the newly picked month when the month picker changes', async () => {
    await renderLoaded()
    vi.mocked(globalThis.fetch).mockClear()
    mockFetchRouted({ summary: () => jsonResponse(200, OTHER_MONTH_SUMMARY) })

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-05' } })

    await waitFor(() => {
      const summaryCall = vi
        .mocked(globalThis.fetch)
        .mock.calls.find(([url]) => String(url).includes('/api/reports/summary'))
      expect(summaryCall).toBeDefined()
      expect(paramsOf(String(summaryCall?.[0])).get('month')).toBe('2026-05')
    })

    await waitFor(() => {
      const categoriesCall = vi
        .mocked(globalThis.fetch)
        .mock.calls.find(([url]) => String(url).includes('/api/reports/categories'))
      const trendCall = vi
        .mocked(globalThis.fetch)
        .mock.calls.find(([url]) => String(url).includes('/api/reports/trend'))
      expect(categoriesCall).toBeDefined()
      expect(trendCall).toBeDefined()
      expect(paramsOf(String(categoriesCall?.[0])).get('month')).toBe('2026-05')
      expect(paramsOf(String(trendCall?.[0])).get('month')).toBe('2026-05')
    })
  })

  it('shows a loading indicator again after a month change, not only on the initial mount', async () => {
    await renderLoaded()

    let releaseSummary: (() => void) | undefined
    vi.mocked(globalThis.fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/api/reports/summary')) {
        return new Promise<Response>((resolve) => {
          releaseSummary = () => resolve(jsonResponse(200, OTHER_MONTH_SUMMARY))
        })
      }
      if (url.includes('/api/reports/categories')) {
        return Promise.resolve(jsonResponse(200, EMPTY_CATEGORY_REPORT))
      }
      if (url.includes('/api/reports/budget-status')) {
        return Promise.resolve(jsonResponse(200, EMPTY_BUDGET_STATUS))
      }
      return Promise.resolve(jsonResponse(200, ZERO_TREND_REPORT))
    })

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-05' } })

    await waitFor(() => {
      expect(screen.getByText('Loading your report...')).toBeInTheDocument()
    })
    expect(screen.queryByText('Balance')).toBeNull()

    releaseSummary?.()
    await screen.findByText('Balance')
  })

  it('shows a chart-section loading indicator again after a month change', async () => {
    await renderLoaded()

    let releaseCategories: (() => void) | undefined
    vi.mocked(globalThis.fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/api/reports/summary')) {
        return Promise.resolve(jsonResponse(200, OTHER_MONTH_SUMMARY))
      }
      if (url.includes('/api/reports/categories')) {
        return new Promise<Response>((resolve) => {
          releaseCategories = () => resolve(jsonResponse(200, EMPTY_CATEGORY_REPORT))
        })
      }
      if (url.includes('/api/reports/budget-status')) {
        return Promise.resolve(jsonResponse(200, EMPTY_BUDGET_STATUS))
      }
      return new Promise<Response>(() => {})
    })

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-05' } })

    await waitFor(() => {
      expect(screen.getByText('Loading your charts...')).toBeInTheDocument()
    })
    expect(document.querySelector('.dashboard-charts')).toBeNull()

    releaseCategories?.()
  })

  it('shows loading indicators for all three independent sections until everything arrives on initial mount', () => {
    vi.mocked(globalThis.fetch).mockReturnValue(new Promise<Response>(() => {}))

    renderWithRouter(<Reports />)

    expect(screen.getAllByRole('status')).toHaveLength(3)
    expect(screen.queryByText('Balance')).toBeNull()
  })

  it('shows an alert and no stat cards when the summary request fails, independent of the charts section', async () => {
    mockFetchRouted({ summary: () => errorResponse(500, 'Something went wrong on the server.') })
    renderWithRouter(<Reports />)

    await waitFor(() => {
      expect(screen.getByText('Something went wrong on the server.')).toBeInTheDocument()
    })
    expect(screen.queryByText('Balance')).toBeNull()

    await waitFor(() => {
      expect(document.querySelector('.dashboard-charts')).not.toBeNull()
    })
    await waitFor(() => {
      expect(screen.queryByRole('status')).toBeNull()
    })
  })

  it('shows a chart-section alert and no charts when the categories request fails, independent of the summary section', async () => {
    mockFetchRouted({ categories: () => errorResponse(500, 'Charts failed to load.') })
    renderWithRouter(<Reports />)

    await screen.findByText('Balance')
    await waitFor(() => {
      expect(screen.getByText('Charts failed to load.')).toBeInTheDocument()
    })

    expect(document.querySelector('.dashboard-charts')).toBeNull()
    expect(screen.getByText('Balance')).toBeInTheDocument()
  })

  it('aborts the in-flight summary request on unmount', () => {
    let capturedSignal: AbortSignal | undefined
    mockFetch((url, init) => {
      if (url.includes('/api/reports/summary')) {
        capturedSignal = init?.signal ?? undefined
      }
      return jsonResponse(200, FULL_SUMMARY)
    })

    const { unmount } = renderWithRouter(<Reports />)
    unmount()

    expect(capturedSignal?.aborted).toBe(true)
  })

  it('aborts the in-flight categories and trend requests on unmount', () => {
    let categoriesSignal: AbortSignal | undefined
    let trendSignal: AbortSignal | undefined
    vi.mocked(globalThis.fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/api/reports/categories')) {
        categoriesSignal = init?.signal ?? undefined
      }
      if (url.includes('/api/reports/trend')) {
        trendSignal = init?.signal ?? undefined
      }
      return new Promise<Response>(() => {})
    })

    const { unmount } = renderWithRouter(<Reports />)
    unmount()

    expect(categoriesSignal?.aborted).toBe(true)
    expect(trendSignal?.aborted).toBe(true)
  })

  it('aborts the in-flight budget-status request on unmount, independent of the other controllers', () => {
    let budgetStatusSignal: AbortSignal | undefined
    let summarySignal: AbortSignal | undefined
    vi.mocked(globalThis.fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/api/reports/budget-status')) {
        budgetStatusSignal = init?.signal ?? undefined
      }
      if (url.includes('/api/reports/summary')) {
        summarySignal = init?.signal ?? undefined
      }
      return new Promise<Response>(() => {})
    })

    const { unmount } = renderWithRouter(<Reports />)
    expect(budgetStatusSignal?.aborted).toBe(false)
    unmount()

    expect(budgetStatusSignal?.aborted).toBe(true)
    expect(summarySignal?.aborted).toBe(true)
    expect(budgetStatusSignal).not.toBe(summarySignal)
  })

  it('issues a fresh AbortController for the budget-status request on every month change', async () => {
    await renderLoaded()

    const signals: AbortSignal[] = []
    vi.mocked(globalThis.fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/api/reports/budget-status')) {
        if (init?.signal) {
          signals.push(init.signal)
        }
        return Promise.resolve(jsonResponse(200, EMPTY_BUDGET_STATUS))
      }
      if (url.includes('/api/reports/categories')) {
        return Promise.resolve(jsonResponse(200, EMPTY_CATEGORY_REPORT))
      }
      if (url.includes('/api/reports/trend')) {
        return Promise.resolve(jsonResponse(200, ZERO_TREND_REPORT))
      }
      return Promise.resolve(jsonResponse(200, OTHER_MONTH_SUMMARY))
    })

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-05' } })

    await waitFor(() => {
      expect(signals.length).toBeGreaterThan(0)
    })
    expect(signals[0]?.aborted).toBe(false)
  })

  it('renders the stat cards and the trend/donut pair once loaded, using the reused dashboard blocks', async () => {
    await renderLoaded()

    const grid = document.querySelector('.dashboard-charts')
    expect(grid?.querySelector('.trend-card')).not.toBeNull()
    expect(grid?.querySelector('.donut-card')).not.toBeNull()
    expect(screen.getByText(huf(1248500))).toBeInTheDocument()
  })

  it('renders the category legend inside the donut once the charts section loads with real category data', async () => {
    mockFetchRouted({ categories: () => jsonResponse(200, FULL_CATEGORY_REPORT) })
    renderWithRouter(<Reports />)
    await screen.findByText('Balance')
    await waitFor(() => {
      expect(document.querySelector('.dashboard-charts')).not.toBeNull()
    })

    const grid = document.querySelector('.dashboard-charts') as HTMLElement
    expect(grid.textContent).toContain('Housing')
    expect(grid.textContent).toContain('Food')
    expect(grid.textContent).toContain('Transport')
  })

  it('fetches the budget status scoped to the current month on mount, never with an empty query', async () => {
    await renderLoaded()

    const budgetStatusCall = vi
      .mocked(globalThis.fetch)
      .mock.calls.find(([url]) => String(url).includes('/api/reports/budget-status'))
    expect(budgetStatusCall).toBeDefined()
    expect(paramsOf(String(budgetStatusCall?.[0])).get('month')).toBe(currentMonthIso())
  })

  it('re-fetches the budget status with the newly picked month when the month picker changes', async () => {
    await renderLoaded()
    vi.mocked(globalThis.fetch).mockClear()
    mockFetchRouted({})

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-05' } })

    await waitFor(() => {
      const budgetStatusCall = vi
        .mocked(globalThis.fetch)
        .mock.calls.find(([url]) => String(url).includes('/api/reports/budget-status'))
      expect(budgetStatusCall).toBeDefined()
      expect(paramsOf(String(budgetStatusCall?.[0])).get('month')).toBe('2026-05')
    })
  })

  it('shows the budget status loading indicator again after a month change, not only on the initial mount', async () => {
    await renderLoaded()

    let releaseBudgetStatus: (() => void) | undefined
    vi.mocked(globalThis.fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/api/reports/budget-status')) {
        return new Promise<Response>((resolve) => {
          releaseBudgetStatus = () => resolve(jsonResponse(200, FULL_BUDGET_STATUS))
        })
      }
      if (url.includes('/api/reports/categories')) {
        return Promise.resolve(jsonResponse(200, EMPTY_CATEGORY_REPORT))
      }
      if (url.includes('/api/reports/trend')) {
        return Promise.resolve(jsonResponse(200, ZERO_TREND_REPORT))
      }
      return Promise.resolve(jsonResponse(200, OTHER_MONTH_SUMMARY))
    })

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-05' } })

    await waitFor(() => {
      expect(screen.getByText('Loading your budget status...')).toBeInTheDocument()
    })
    expect(screen.queryByText('Budget vs Actual')).toBeNull()

    releaseBudgetStatus?.()
    await screen.findByText('Budget vs Actual')
  })

  it('shows a budget-status alert and no list when the request fails, independent of the summary and charts sections', async () => {
    mockFetchRouted({
      budgetStatus: () => errorResponse(500, 'Budget status failed to load.'),
    })
    renderWithRouter(<Reports />)

    await screen.findByText('Balance')
    await waitFor(() => {
      expect(document.querySelector('.dashboard-charts')).not.toBeNull()
    })
    await waitFor(() => {
      expect(screen.getByText('Budget status failed to load.')).toBeInTheDocument()
    })

    expect(screen.queryByText('Budget vs Actual')).toBeNull()
    expect(screen.getByText('Balance')).toBeInTheDocument()
    expect(document.querySelector('.dashboard-charts')).not.toBeNull()
  })

  it('renders the summary and charts sections normally when only the budget-status request is slow', async () => {
    vi.mocked(globalThis.fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/api/reports/budget-status')) {
        return new Promise<Response>(() => {})
      }
      if (url.includes('/api/reports/categories')) {
        return Promise.resolve(jsonResponse(200, EMPTY_CATEGORY_REPORT))
      }
      if (url.includes('/api/reports/trend')) {
        return Promise.resolve(jsonResponse(200, ZERO_TREND_REPORT))
      }
      return Promise.resolve(jsonResponse(200, FULL_SUMMARY))
    })

    renderWithRouter(<Reports />)

    await screen.findByText('Balance')
    await waitFor(() => {
      expect(document.querySelector('.dashboard-charts')).not.toBeNull()
    })
    expect(screen.getByText('Loading your budget status...')).toBeInTheDocument()
  })

  it('renders each budget row with amounts, a remaining label sourced from the sign of remaining, and preserves response order', async () => {
    mockFetchRouted({ budgetStatus: () => jsonResponse(200, FULL_BUDGET_STATUS) })
    renderWithRouter(<Reports />)

    await screen.findByText('Budget vs Actual')

    const names = screen.getAllByText(/^(Food|Housing)$/)
    expect(names.map((el) => el.textContent)).toEqual(['Food', 'Housing'])

    expect(screen.getByText('12 000 Ft over')).toBeInTheDocument()
    expect(screen.getByText('20 000 Ft left')).toBeInTheDocument()

    const overItem = screen.getByText('12 000 Ft over')
    const underItem = screen.getByText('20 000 Ft left')
    expect(overItem.className).toContain('budget-status-item-remaining--danger')
    expect(overItem.className).not.toContain('budget-status-item-remaining--success')
    expect(underItem.className).toContain('budget-status-item-remaining--success')
    expect(underItem.className).not.toContain('budget-status-item-remaining--danger')
  })

  it('shows the uncapped percentage for an over-budget row while clamping the bar width to 100%', async () => {
    mockFetchRouted({ budgetStatus: () => jsonResponse(200, FULL_BUDGET_STATUS) })
    renderWithRouter(<Reports />)

    await screen.findByText('Budget vs Actual')

    expect(screen.getByText('124.0%')).toBeInTheDocument()
    const fills = document.querySelectorAll('.budget-status-fill')
    const overFill = Array.from(fills).find(
      (el) => (el as HTMLElement).classList.contains('budget-status-fill--danger'),
    ) as HTMLElement
    expect(overFill.style.width).toBe('100%')
  })

  it('marks an over-budget row danger and an under-budget row accent for the bar tone', async () => {
    mockFetchRouted({ budgetStatus: () => jsonResponse(200, FULL_BUDGET_STATUS) })
    renderWithRouter(<Reports />)

    await screen.findByText('Budget vs Actual')

    expect(document.querySelector('.budget-status-fill--danger')).not.toBeNull()
    expect(document.querySelector('.budget-status-fill--accent')).not.toBeNull()
  })

  it('renders a dash without throwing for a row with a null percentageUsed', async () => {
    const NULL_PERCENT_STATUS: BudgetStatusResponse = {
      month: '2026-08',
      totalBudgeted: 30000,
      totalSpent: 0,
      unbudgetedSpending: 0,
      categories: [
        {
          categoryId: 3,
          categoryName: 'Misc',
          budgeted: 30000,
          spent: 0,
          remaining: 30000,
          percentageUsed: null,
        },
      ],
    }
    mockFetchRouted({ budgetStatus: () => jsonResponse(200, NULL_PERCENT_STATUS) })
    renderWithRouter(<Reports />)

    await screen.findByText('Budget vs Actual')

    expect(screen.getByText('Misc')).toBeInTheDocument()
    expect(screen.getByText('—')).toBeInTheDocument()
  })

  it('shows the fully-empty message when there are no budgets and no unbudgeted spending', async () => {
    mockFetchRouted({ budgetStatus: () => jsonResponse(200, EMPTY_BUDGET_STATUS) })
    renderWithRouter(<Reports />)

    await screen.findByText('Budget vs Actual')

    expect(
      screen.getByText('No budgets set for this month, and no spending recorded.'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/Unbudgeted spending/)).not.toBeInTheDocument()
  })

  it('shows the no-budgets message and the unbudgeted spending line when there are no budgets but there is unbudgeted spending', async () => {
    const UNBUDGETED_ONLY: BudgetStatusResponse = {
      month: '2026-08',
      totalBudgeted: 0,
      totalSpent: 15000,
      unbudgetedSpending: 15000,
      categories: [],
    }
    mockFetchRouted({ budgetStatus: () => jsonResponse(200, UNBUDGETED_ONLY) })
    renderWithRouter(<Reports />)

    await screen.findByText('Budget vs Actual')

    expect(screen.getByText('No budgets set for this month.')).toBeInTheDocument()
    expect(
      screen.queryByText('No budgets set for this month, and no spending recorded.'),
    ).not.toBeInTheDocument()
    expect(screen.getByText(`Unbudgeted spending: ${huf(15000)}`)).toBeInTheDocument()
  })

  it('shows the unbudgeted spending line alongside a populated list, and hides it when unbudgeted spending is zero', async () => {
    const WITH_UNBUDGETED: BudgetStatusResponse = {
      ...FULL_BUDGET_STATUS,
      unbudgetedSpending: 5000,
    }
    mockFetchRouted({ budgetStatus: () => jsonResponse(200, WITH_UNBUDGETED) })
    renderWithRouter(<Reports />)

    await screen.findByText('Budget vs Actual')
    expect(screen.getByText(/Unbudgeted spending/)).toBeInTheDocument()
  })

  it('hides the unbudgeted spending line for a populated list when unbudgeted spending is zero', async () => {
    mockFetchRouted({ budgetStatus: () => jsonResponse(200, FULL_BUDGET_STATUS) })
    renderWithRouter(<Reports />)

    await screen.findByText('Budget vs Actual')
    expect(screen.queryByText(/Unbudgeted spending/)).not.toBeInTheDocument()
  })

  it('renders totalBudgeted and totalSpent directly from the response, even when they differ from the sum of the row amounts', async () => {
    const MISMATCHED_TOTALS: BudgetStatusResponse = {
      month: '2026-08',
      totalBudgeted: 150001,
      totalSpent: 142002,
      unbudgetedSpending: 0,
      categories: FULL_BUDGET_STATUS.categories,
    }
    mockFetchRouted({ budgetStatus: () => jsonResponse(200, MISMATCHED_TOTALS) })
    renderWithRouter(<Reports />)

    await screen.findByText('Budget vs Actual')

    expect(screen.getByText(huf(150001))).toBeInTheDocument()
    expect(screen.getByText(huf(142002))).toBeInTheDocument()
    expect(screen.queryByText(huf(150000))).not.toBeInTheDocument()
    expect(screen.queryByText(huf(142000))).not.toBeInTheDocument()
  })

  it('renders the budget-status block below the stat cards and the trend/donut pair once loaded', async () => {
    mockFetchRouted({ budgetStatus: () => jsonResponse(200, FULL_BUDGET_STATUS) })
    renderWithRouter(<Reports />)

    await screen.findByText('Balance')
    await waitFor(() => {
      expect(document.querySelector('.dashboard-charts')).not.toBeNull()
    })
    await screen.findByText('Budget vs Actual')

    const page = document.querySelector('.shell-page') as HTMLElement
    const statGridIndex = Array.from(page.children).findIndex((el) =>
      el.classList.contains('stat-grid'),
    )
    const chartsIndex = Array.from(page.children).findIndex((el) =>
      el.classList.contains('dashboard-charts'),
    )
    const budgetCardIndex = Array.from(page.children).findIndex((el) =>
      el.classList.contains('budget-status-card'),
    )
    expect(statGridIndex).toBeGreaterThanOrEqual(0)
    expect(chartsIndex).toBeGreaterThan(statGridIndex)
    expect(budgetCardIndex).toBeGreaterThan(chartsIndex)
  })
})
