import type { ReactElement } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import Dashboard from './Dashboard'
import { clearCookies, errorResponse, jsonResponse } from '../test/helpers'
import { formatHeaderDate } from '../lib/format'
import type {
  CategoryReportResponse,
  PageResponse,
  ReportsSummaryResponse,
  TransactionResponseDto,
  TrendReportResponse,
} from '../api/types'

type FetchHandler = (url: string, init: RequestInit | undefined) => Response

function renderWithRouter(ui: ReactElement) {
  return render(<MemoryRouter>{ui}</MemoryRouter>)
}

function paramsOf(url: string): URLSearchParams {
  return new URL(url, 'http://localhost').searchParams
}

function mockFetch(handler: FetchHandler) {
  vi.mocked(globalThis.fetch).mockImplementation((input, init) =>
    Promise.resolve(handler(String(input), init)),
  )
}

function huf(value: number): string {
  return `${value.toLocaleString('hu-HU')} Ft`.replace(/\u00A0/g, ' ')
}

function hufRaw(value: number): string {
  return `${value.toLocaleString('hu-HU')} Ft`
}

const FULL_SUMMARY: ReportsSummaryResponse = {
  month: '2026-08',
  previousMonth: '2026-07',
  balance: { current: 1248500, previous: 1198000, deltaPercent: 4.2 },
  income: { current: 500000, previous: 489000, deltaPercent: 2.1 },
  expense: { current: 300000, previous: 276000, deltaPercent: 8.6 },
  savingsRate: { current: 40, previous: 41.3, deltaPoints: -1.3 },
}

const ZERO_MONTH_SUMMARY: ReportsSummaryResponse = {
  month: '2026-08',
  previousMonth: '2026-07',
  balance: { current: 0, previous: 0, deltaPercent: null },
  income: { current: 0, previous: 0, deltaPercent: null },
  expense: { current: 0, previous: 0, deltaPercent: null },
  savingsRate: { current: null, previous: null, deltaPoints: null },
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

const FULL_TREND_REPORT: TrendReportResponse = {
  month: '2026-08',
  months: 6,
  points: [
    { month: '2026-03', income: 430000, expense: 298000 },
    { month: '2026-04', income: 410000, expense: 250000 },
    { month: '2026-05', income: 420000, expense: 260000 },
    { month: '2026-06', income: 440000, expense: 280000 },
    { month: '2026-07', income: 450000, expense: 300000 },
    { month: '2026-08', income: 500000, expense: 300000 },
  ],
}

const EMPTY_TRANSACTIONS_PAGE: PageResponse<TransactionResponseDto> = {
  content: [],
  page: 0,
  size: 5,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
}

const FULL_TRANSACTIONS_PAGE: PageResponse<TransactionResponseDto> = {
  content: [
    {
      id: 101,
      type: 'INCOME',
      description: 'August salary',
      categoryId: 1,
      categoryName: 'Salary',
      amount: 500000,
      date: '2020-01-10',
    },
    {
      id: 102,
      type: 'EXPENSE',
      description: null,
      categoryId: 2,
      categoryName: 'Groceries',
      amount: 8200,
      date: '2020-01-09',
    },
  ],
  page: 0,
  size: 5,
  totalElements: 2,
  totalPages: 1,
  first: true,
  last: true,
}

interface FetchRoutes {
  summary?: () => Response
  categories?: () => Response
  trend?: () => Response
  transactions?: () => Response
}

function mockFetchRouted(routes: FetchRoutes) {
  mockFetch((url) => {
    if (url.includes('/api/reports/categories')) {
      return (routes.categories ?? (() => jsonResponse(200, EMPTY_CATEGORY_REPORT)))()
    }
    if (url.includes('/api/reports/trend')) {
      return (routes.trend ?? (() => jsonResponse(200, ZERO_TREND_REPORT)))()
    }
    if (url.includes('/api/transactions')) {
      return (routes.transactions ?? (() => jsonResponse(200, EMPTY_TRANSACTIONS_PAGE)))()
    }
    return (routes.summary ?? (() => jsonResponse(200, FULL_SUMMARY)))()
  })
}

async function renderLoaded(summary: ReportsSummaryResponse) {
  mockFetchRouted({ summary: () => jsonResponse(200, summary) })
  renderWithRouter(<Dashboard />)
  await screen.findByText('Balance')
}

async function renderWithCharts(categories: CategoryReportResponse, trend: TrendReportResponse) {
  mockFetchRouted({
    categories: () => jsonResponse(200, categories),
    trend: () => jsonResponse(200, trend),
  })
  renderWithRouter(<Dashboard />)
  await screen.findByText('Balance')
  await waitFor(() => {
    expect(document.querySelector('.dashboard-charts')).not.toBeNull()
  })
}

async function renderWithTransactions(page: PageResponse<TransactionResponseDto>) {
  mockFetchRouted({ transactions: () => jsonResponse(200, page) })
  renderWithRouter(<Dashboard />)
  await screen.findByText('Balance')
  await waitFor(() => {
    expect(document.querySelector('.recent-transactions-card')).not.toBeNull()
  })
}

describe('Dashboard', () => {
  beforeEach(() => {
    clearCookies()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    clearCookies()
  })

  it('names the page with a single first level heading', async () => {
    await renderLoaded(FULL_SUMMARY)

    const headings = screen.getAllByRole('heading', { level: 1 })
    expect(headings).toHaveLength(1)
    expect(headings[0]).toHaveTextContent('Overview')
  })

  it('shows the current date in the header, formatted the same way as formatHeaderDate', async () => {
    await renderLoaded(FULL_SUMMARY)

    expect(screen.getByText(formatHeaderDate(new Date()))).toBeInTheDocument()
  })

  it('shows loading indicators for all three independent sections until everything arrives', () => {
    vi.mocked(globalThis.fetch).mockReturnValue(new Promise<Response>(() => {}))

    renderWithRouter(<Dashboard />)

    expect(screen.getAllByRole('status')).toHaveLength(3)
    expect(screen.queryByText('Balance')).toBeNull()
  })

  it('fetches the reports summary on mount', async () => {
    await renderLoaded(FULL_SUMMARY)

    const summaryCall = vi
      .mocked(globalThis.fetch)
      .mock.calls.find(([url]) => String(url).includes('/api/reports/summary'))
    expect(summaryCall).toBeDefined()
  })

  it('renders the four stat cards in order with formatted values', async () => {
    await renderLoaded(FULL_SUMMARY)

    const labels = screen.getAllByText(/^(Balance|Monthly Income|Monthly Expense|Savings Rate)$/)
    expect(labels.map((el) => el.textContent)).toEqual([
      'Balance',
      'Monthly Income',
      'Monthly Expense',
      'Savings Rate',
    ])

    expect(screen.getByText(huf(1248500))).toBeInTheDocument()
    expect(screen.getByText(huf(500000))).toBeInTheDocument()
    expect(screen.getByText(huf(300000))).toBeInTheDocument()
    expect(screen.getByText('40.0%')).toBeInTheDocument()
  })

  it('binds the correct icon variant to each stat card', async () => {
    await renderLoaded(FULL_SUMMARY)

    function statCardFor(labelText: string): HTMLElement {
      const label = screen.getByText(labelText)
      const card = label.closest('.stat-card')
      if (card === null) {
        throw new Error(`Expected an ancestor .stat-card for label "${labelText}"`)
      }
      return card as HTMLElement
    }

    expect(statCardFor('Balance').querySelector('.stat-card-icon--accent')).not.toBeNull()

    expect(statCardFor('Monthly Income').querySelector('.stat-card-icon--success')).not.toBeNull()
    expect(statCardFor('Monthly Income').querySelector('.stat-card-icon--danger')).toBeNull()

    expect(statCardFor('Monthly Expense').querySelector('.stat-card-icon--danger')).not.toBeNull()
    expect(statCardFor('Monthly Expense').querySelector('.stat-card-icon--success')).toBeNull()

    expect(statCardFor('Savings Rate').querySelector('.stat-card-icon--accent')).not.toBeNull()
  })

  it('colors the monthly expense delta as negative even though the percentage itself is positive', async () => {
    await renderLoaded(FULL_SUMMARY)

    const expenseDelta = screen.getByText('+8.6% vs last month')
    expect(expenseDelta).toHaveClass('stat-card-delta--negative')
  })

  it('colors the balance delta as positive when the balance grows', async () => {
    await renderLoaded(FULL_SUMMARY)

    const balanceDelta = screen.getByText('+4.2% vs last month')
    expect(balanceDelta).toHaveClass('stat-card-delta--positive')
  })

  it('shows an em dash for savings rate when the month has zero income, not 0% or NaN%', async () => {
    await renderLoaded(ZERO_MONTH_SUMMARY)

    expect(screen.getAllByText('—').length).toBeGreaterThan(0)
    expect(screen.queryByText('0.0%')).toBeNull()
    expect(screen.queryByText(/NaN/)).toBeNull()
  })

  it('renders no delta row for any stat card when its delta is null', async () => {
    await renderLoaded(ZERO_MONTH_SUMMARY)

    expect(document.querySelectorAll('.stat-card-delta')).toHaveLength(0)
  })

  it('shows an alert and no stat cards when the summary request fails, independent of the charts section', async () => {
    mockFetchRouted({ summary: () => errorResponse(500, 'Something went wrong on the server.') })
    renderWithRouter(<Dashboard />)

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

  it('aborts the in-flight summary request on unmount', () => {
    let capturedSignal: AbortSignal | undefined
    mockFetch((url, init) => {
      if (url.includes('/api/reports/summary')) {
        capturedSignal = init?.signal ?? undefined
      }
      return new Response(JSON.stringify(FULL_SUMMARY), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      })
    })

    const { unmount } = renderWithRouter(<Dashboard />)
    unmount()

    expect(capturedSignal?.aborted).toBe(true)
  })

  it('fetches categories and trend (6 months) on mount, independently of the summary fetch', async () => {
    await renderWithCharts(FULL_CATEGORY_REPORT, FULL_TREND_REPORT)

    const categoriesCall = vi
      .mocked(globalThis.fetch)
      .mock.calls.find(([url]) => String(url).includes('/api/reports/categories'))
    const trendCall = vi
      .mocked(globalThis.fetch)
      .mock.calls.find(([url]) => String(url).includes('/api/reports/trend'))

    expect(categoriesCall).toBeDefined()
    expect(trendCall).toBeDefined()
    expect(String(trendCall?.[0])).toContain('months=6')
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

    const { unmount } = renderWithRouter(<Dashboard />)
    unmount()

    expect(categoriesSignal?.aborted).toBe(true)
    expect(trendSignal?.aborted).toBe(true)
  })

  it('shows the dashboard-charts grid with the trend chart and donut once categories and trend load', async () => {
    await renderWithCharts(FULL_CATEGORY_REPORT, FULL_TREND_REPORT)

    const grid = document.querySelector('.dashboard-charts')
    expect(grid).not.toBeNull()
    expect(grid?.querySelector('.trend-card')).not.toBeNull()
    expect(grid?.querySelector('.donut-card')).not.toBeNull()
  })

  it('renders the category breakdown inside the dashboard-bottom grid, outside the dashboard-charts grid', async () => {
    await renderWithCharts(FULL_CATEGORY_REPORT, FULL_TREND_REPORT)

    const chartsGrid = document.querySelector('.dashboard-charts')
    const bottomGrid = document.querySelector('.dashboard-bottom')
    const breakdown = document.querySelector('.breakdown-card')
    expect(breakdown).not.toBeNull()
    expect(chartsGrid?.contains(breakdown)).toBe(false)
    expect(bottomGrid?.contains(breakdown)).toBe(true)
  })

  it('formats the trend column aria-labels using the formatted income and expense amounts', async () => {
    await renderWithCharts(FULL_CATEGORY_REPORT, FULL_TREND_REPORT)

    expect(
      screen.getByLabelText(`Mar: ${huf(430000)} income, ${huf(298000)} expense`),
    ).toBeInTheDocument()
  })

  it('shows every category in the breakdown card with its formatted amount', async () => {
    await renderWithCharts(FULL_CATEGORY_REPORT, FULL_TREND_REPORT)

    const breakdown = document.querySelector('.breakdown-card') as HTMLElement
    expect(breakdown.textContent).toContain('Housing')
    expect(breakdown.textContent).toContain(hufRaw(150000))
    expect(breakdown.textContent).toContain('Food')
    expect(breakdown.textContent).toContain('Transport')
  })

  it('shows empty states in the trend chart, donut, and breakdown when there is no category or trend data', async () => {
    mockFetchRouted({
      categories: () => jsonResponse(200, EMPTY_CATEGORY_REPORT),
      trend: () => jsonResponse(200, ZERO_TREND_REPORT),
    })
    renderWithRouter(<Dashboard />)
    await screen.findByText('Balance')

    await waitFor(() => {
      expect(document.querySelectorAll('.dashboard-charts .empty-state')).toHaveLength(2)
    })
    const breakdown = document.querySelector('.breakdown-card') as HTMLElement
    expect(breakdown.querySelector('.empty-state')).not.toBeNull()
  })

  it('shows a chart-section alert and no charts when the categories request fails, independent of the summary section', async () => {
    mockFetchRouted({ categories: () => errorResponse(500, 'Charts failed to load.') })
    renderWithRouter(<Dashboard />)

    await screen.findByText('Balance')
    await waitFor(() => {
      expect(screen.getByText('Charts failed to load.')).toBeInTheDocument()
    })

    expect(document.querySelector('.dashboard-charts')).toBeNull()
    expect(screen.getByText('Balance')).toBeInTheDocument()
  })

  it('shows a chart-section loading indicator while the summary and transactions sections have already loaded', async () => {
    vi.mocked(globalThis.fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/api/reports/summary')) {
        return Promise.resolve(jsonResponse(200, FULL_SUMMARY))
      }
      if (url.includes('/api/transactions')) {
        return Promise.resolve(jsonResponse(200, EMPTY_TRANSACTIONS_PAGE))
      }
      return new Promise<Response>(() => {})
    })

    renderWithRouter(<Dashboard />)

    await screen.findByText('Balance')
    await screen.findByText('No transactions yet.')
    expect(screen.getByRole('status')).toBeInTheDocument()
    expect(document.querySelector('.dashboard-charts')).toBeNull()
  })

  it('fetches the recent transactions on mount, requesting 5 rows sorted by date descending', async () => {
    await renderWithTransactions(FULL_TRANSACTIONS_PAGE)

    const transactionsCall = vi
      .mocked(globalThis.fetch)
      .mock.calls.find(([url]) => String(url).includes('/api/transactions'))
    expect(transactionsCall).toBeDefined()

    const params = paramsOf(String(transactionsCall?.[0]))
    expect(params.get('page')).toBe('0')
    expect(params.get('size')).toBe('5')
    expect(params.get('sort')).toBe('date,desc')
  })

  it('aborts the in-flight transactions request on unmount', () => {
    let transactionsSignal: AbortSignal | undefined
    vi.mocked(globalThis.fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/api/transactions')) {
        transactionsSignal = init?.signal ?? undefined
      }
      return new Promise<Response>(() => {})
    })

    const { unmount } = renderWithRouter(<Dashboard />)
    unmount()

    expect(transactionsSignal?.aborted).toBe(true)
  })

  it('renders the recent transactions card in the dashboard-bottom grid, independently of the charts section', async () => {
    await renderWithTransactions(FULL_TRANSACTIONS_PAGE)

    const bottomGrid = document.querySelector('.dashboard-bottom')
    const recent = document.querySelector('.recent-transactions-card')
    expect(recent).not.toBeNull()
    expect(bottomGrid?.contains(recent)).toBe(true)
  })

  it('shows the income row with a plus sign and the success color, and the expense row with a minus sign and the text color, not danger', async () => {
    await renderWithTransactions(FULL_TRANSACTIONS_PAGE)

    const incomeAmount = screen.getByText(`+${huf(500000)}`)
    expect(incomeAmount).toHaveClass('recent-transaction-amount--income')
    expect(incomeAmount).not.toHaveClass('recent-transaction-amount--expense')

    const expenseAmount = screen.getByText(`−${huf(8200)}`)
    expect(expenseAmount).toHaveClass('recent-transaction-amount--expense')
    expect(expenseAmount).not.toHaveClass('recent-transaction-amount--income')
  })

  it('falls back to the category name when a transaction has no description', async () => {
    await renderWithTransactions(FULL_TRANSACTIONS_PAGE)

    expect(screen.getByText('Groceries')).toBeInTheDocument()
    expect(screen.getByText('August salary')).toBeInTheDocument()
  })

  it('labels each transaction row with its category and formatted date', async () => {
    await renderWithTransactions(FULL_TRANSACTIONS_PAGE)

    expect(screen.getByText('Salary · Jan 10, 2020')).toBeInTheDocument()
    expect(screen.getByText('Groceries · Jan 9, 2020')).toBeInTheDocument()
  })

  it('shows an empty state in the recent transactions card when there are no transactions', async () => {
    await renderWithTransactions(EMPTY_TRANSACTIONS_PAGE)

    const recent = document.querySelector('.recent-transactions-card') as HTMLElement
    expect(recent.querySelector('.empty-state')).not.toBeNull()
    expect(recent.querySelector('.recent-transactions-list')).toBeNull()
  })

  it('shows a loading indicator in the transactions column while transactions are pending, independent of the other two sections', async () => {
    vi.mocked(globalThis.fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/api/transactions')) {
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

    renderWithRouter(<Dashboard />)

    await screen.findByText('Balance')
    await waitFor(() => {
      expect(document.querySelector('.breakdown-card')).not.toBeNull()
    })

    expect(screen.getByRole('status')).toBeInTheDocument()
    expect(document.querySelector('.recent-transactions-card')).toBeNull()
  })

  it('shows an alert in the transactions column when the transactions request fails, independent of the other two sections', async () => {
    mockFetchRouted({ transactions: () => errorResponse(500, 'Recent transactions failed to load.') })
    renderWithRouter(<Dashboard />)

    await screen.findByText('Balance')
    await waitFor(() => {
      expect(document.querySelector('.breakdown-card')).not.toBeNull()
    })
    expect(screen.getByText('Recent transactions failed to load.')).toBeInTheDocument()
    expect(document.querySelector('.recent-transactions-card')).toBeNull()
  })

  it('links View all to the transactions route using a real react-router link', async () => {
    await renderWithTransactions(FULL_TRANSACTIONS_PAGE)

    const link = screen.getByRole('link', { name: 'View all' })
    expect(link).toHaveAttribute('href', '/transactions')
  })

  it('navigates to /transactions?new=true when + New Transaction is clicked', async () => {
    mockFetchRouted({})

    function LocationProbe() {
      const location = useLocation()
      return <div data-testid="location-probe">{location.pathname + location.search}</div>
    }

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/transactions" element={<LocationProbe />} />
        </Routes>
      </MemoryRouter>,
    )
    await screen.findByText('Balance')

    fireEvent.click(screen.getByRole('button', { name: '+ New Transaction' }))

    expect(await screen.findByTestId('location-probe')).toHaveTextContent(
      '/transactions?new=true',
    )
  })

  it('wraps the title and date in a column separate from the New Transaction button', async () => {
    await renderLoaded(FULL_SUMMARY)

    const header = document.querySelector('.shell-page-header') as HTMLElement
    const heading = screen.getByRole('heading', { level: 1 })
    const button = screen.getByRole('button', { name: '+ New Transaction' })

    expect(button.parentElement).toBe(header)
    expect(heading.parentElement).not.toBe(header)
    expect(heading.parentElement?.parentElement).toBe(header)
    expect(heading.parentElement).not.toBe(button.parentElement)
  })
})
