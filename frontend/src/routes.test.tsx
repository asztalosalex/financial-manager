import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { RouterProvider, createMemoryRouter } from 'react-router-dom'
import { routes } from './routes'
import { setUnauthorizedHandler } from './api/client'
import { emptyResponse, jsonResponse } from './test/helpers'
import type {
  CategoryReportResponse,
  ReportsSummaryResponse,
  TrendReportResponse,
  UserResponseDto,
} from './api/types'

const USER: UserResponseDto = {
  id: 7,
  username: 'alex',
  email: 'alex@example.com',
  createdAt: null,
  lastLogin: null,
}

const REPORTS_SUMMARY: ReportsSummaryResponse = {
  month: '2026-08',
  previousMonth: '2026-07',
  balance: { current: 0, previous: 0, deltaPercent: null },
  income: { current: 0, previous: 0, deltaPercent: null },
  expense: { current: 0, previous: 0, deltaPercent: null },
  savingsRate: { current: null, previous: null, deltaPoints: null },
}

const REPORTS_CATEGORIES: CategoryReportResponse = {
  month: '2026-08',
  total: 0,
  categories: [],
}

const REPORTS_TREND: TrendReportResponse = {
  month: '2026-08',
  months: 6,
  points: [],
}

function backendResponse(url: string): Response {
  if (url.startsWith('/api/reports/summary')) {
    return jsonResponse(200, REPORTS_SUMMARY)
  }
  if (url.startsWith('/api/reports/categories')) {
    return jsonResponse(200, REPORTS_CATEGORIES)
  }
  if (url.startsWith('/api/reports/trend')) {
    return jsonResponse(200, REPORTS_TREND)
  }
  if (url.startsWith('/api/categories')) {
    return jsonResponse(200, [])
  }
  if (url.startsWith('/api/transactions')) {
    return jsonResponse(200, {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    })
  }
  if (url.startsWith('/api/budgets')) {
    return jsonResponse(200, {
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
      first: true,
      last: true,
    })
  }
  if (url === '/api/users/count') {
    return jsonResponse(200, 42)
  }
  return jsonResponse(200, USER)
}

function signedIn(): void {
  vi.mocked(globalThis.fetch).mockImplementation((input) =>
    Promise.resolve(backendResponse(String(input))),
  )
}

function signedOut(): void {
  vi.mocked(globalThis.fetch).mockImplementation((input) => {
    const url = String(input)
    if (url === '/api/users/profile') {
      return Promise.resolve(emptyResponse(401))
    }
    return Promise.resolve(backendResponse(url))
  })
}

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(<RouterProvider router={router} />)
  return router
}

function headerIsRendered(): boolean {
  return screen.queryByRole('button', { name: 'Toggle navigation menu' }) !== null
}

function footerIsRendered(): boolean {
  return screen.queryByRole('contentinfo') !== null
}

describe('route structure', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends an unauthenticated visitor from /dashboard to the login page', async () => {
    signedOut()

    const router = renderAt('/dashboard')

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(screen.queryByRole('heading', { level: 1, name: 'Overview' })).not.toBeInTheDocument()
  })

  it('sends an unauthenticated visitor from /transactions to the login page', async () => {
    signedOut()

    const router = renderAt('/transactions')

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(screen.queryByRole('heading', { level: 1, name: 'Transactions' })).not.toBeInTheDocument()
  })

  it('sends an unauthenticated visitor from /categories to the login page', async () => {
    signedOut()

    const router = renderAt('/categories')

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(screen.queryByRole('heading', { level: 1, name: 'Categories' })).not.toBeInTheDocument()
  })

  it('sends an unauthenticated visitor from /settings to the login page', async () => {
    signedOut()

    const router = renderAt('/settings')

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(screen.queryByRole('heading', { level: 1, name: 'Settings' })).not.toBeInTheDocument()
  })

  it('sends an unauthenticated visitor from /budgets to the login page', async () => {
    signedOut()

    const router = renderAt('/budgets')

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(screen.queryByRole('heading', { level: 1, name: 'Budgets' })).not.toBeInTheDocument()
  })

  it('sends an unauthenticated visitor from /reports to the login page', async () => {
    signedOut()

    const router = renderAt('/reports')

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(screen.queryByRole('heading', { level: 1, name: 'Reports' })).not.toBeInTheDocument()
  })

  it('renders the overview page for a signed-in visitor', async () => {
    signedIn()

    renderAt('/dashboard')

    expect(await screen.findByRole('heading', { level: 1, name: 'Overview' })).toBeInTheDocument()
  })

  it('renders the transactions page for a signed-in visitor', async () => {
    signedIn()

    renderAt('/transactions')

    expect(await screen.findByRole('heading', { level: 1, name: 'Transactions' })).toBeInTheDocument()
    expect(
      await screen.findByText('No transactions yet. Add your first transaction to get started.'),
    ).toBeInTheDocument()
  })

  it('renders the categories page for a signed-in visitor', async () => {
    signedIn()

    renderAt('/categories')

    expect(await screen.findByRole('heading', { level: 1, name: 'Categories' })).toBeInTheDocument()
    await screen.findByRole('heading', { level: 3, name: 'Your Categories' })
  })

  it('renders the budgets page for a signed-in visitor', async () => {
    signedIn()

    renderAt('/budgets')

    expect(await screen.findByRole('heading', { level: 1, name: 'Budgets' })).toBeInTheDocument()
    expect(
      await screen.findByText('No budgets yet. Add your first budget to get started.'),
    ).toBeInTheDocument()
  })

  it('renders the reports page for a signed-in visitor, inside the protected app shell', async () => {
    signedIn()

    renderAt('/reports')

    expect(await screen.findByRole('heading', { level: 1, name: 'Reports' })).toBeInTheDocument()
    expect(headerIsRendered()).toBe(false)
    expect(footerIsRendered()).toBe(false)
    expect(screen.getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument()
  })

  it('renders the settings page for a signed-in visitor', async () => {
    signedIn()

    renderAt('/settings')

    expect(await screen.findByRole('heading', { level: 1, name: 'Settings' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Edit Profile' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete Account' })).toBeInTheDocument()
  })

  it('leaves the Header and the Footer out of the protected overview page', async () => {
    signedIn()

    renderAt('/dashboard')
    await screen.findByRole('heading', { level: 1, name: 'Overview' })

    expect(headerIsRendered()).toBe(false)
    expect(footerIsRendered()).toBe(false)
    expect(document.querySelector('.navbar')).toBeNull()
    expect(document.querySelector('.footer')).toBeNull()
  })

  it('leaves the Header and the Footer out of the protected settings page', async () => {
    signedIn()

    renderAt('/settings')
    await screen.findByRole('heading', { level: 1, name: 'Settings' })

    expect(headerIsRendered()).toBe(false)
    expect(footerIsRendered()).toBe(false)
    expect(document.querySelector('.navbar')).toBeNull()
    expect(document.querySelector('.footer')).toBeNull()
  })

  it('keeps the Header and the Footer on the public landing page', async () => {
    signedOut()

    renderAt('/')

    await waitFor(() => expect(headerIsRendered()).toBe(true))
    expect(footerIsRendered()).toBe(true)
    expect(document.querySelector('.navbar')).not.toBeNull()
    expect(screen.queryByRole('navigation', { name: 'Main navigation' })).not.toBeInTheDocument()
  })

  it('keeps the Header and the Footer on the login page', async () => {
    signedOut()

    renderAt('/login')

    await waitFor(() => expect(headerIsRendered()).toBe(true))
    expect(footerIsRendered()).toBe(true)
    expect(screen.queryByRole('navigation', { name: 'Main navigation' })).not.toBeInTheDocument()
  })

  it('keeps the Header and the Footer on the register page', async () => {
    signedOut()

    renderAt('/register')

    await waitFor(() => expect(headerIsRendered()).toBe(true))
    expect(footerIsRendered()).toBe(true)
    expect(screen.queryByRole('navigation', { name: 'Main navigation' })).not.toBeInTheDocument()
  })

  it('forwards the old /profile address to /settings instead of dropping it', async () => {
    signedIn()

    const router = renderAt('/profile')

    await waitFor(() => expect(router.state.location.pathname).toBe('/settings'))
    expect(await screen.findByRole('heading', { level: 1, name: 'Settings' })).toBeInTheDocument()
  })

  it('keeps the forwarded /profile address behind the session check', async () => {
    signedOut()

    const router = renderAt('/profile')

    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(screen.queryByRole('button', { name: 'Edit Profile' })).not.toBeInTheDocument()
  })

  it('drives the active surface from the route instead of tab buttons', async () => {
    signedIn()

    renderAt('/categories')
    await screen.findByRole('heading', { level: 1, name: 'Categories' })

    const nav = screen.getByRole('navigation', { name: 'Main navigation' })
    expect(within(nav).getAllByRole('link')).toHaveLength(6)
    expect(within(nav).queryAllByRole('button')).toHaveLength(0)
    expect(screen.queryByRole('button', { name: /Profile Data/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Income & Expenses/ })).not.toBeInTheDocument()
  })

  it('renders the not-found page for a signed-out visitor at an unknown address', async () => {
    signedOut()

    const router = renderAt('/does-not-exist')

    expect(await screen.findByText('404')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Page not found' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/does-not-exist')
    expect(screen.queryByRole('heading', { level: 2, name: 'Log in' })).not.toBeInTheDocument()
  })

  it('renders the not-found page for a signed-in visitor at an unknown address, outside the app shell', async () => {
    signedIn()

    const router = renderAt('/does-not-exist')

    expect(await screen.findByText('404')).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/does-not-exist')
    expect(screen.queryByRole('navigation', { name: 'Main navigation' })).not.toBeInTheDocument()
    expect(document.querySelector('.shell-layout')).toBeNull()
    expect(headerIsRendered()).toBe(false)
    expect(footerIsRendered()).toBe(false)
  })

  it('links back to the homepage from the not-found page', async () => {
    signedOut()

    renderAt('/does-not-exist')

    const link = await screen.findByRole('link', { name: 'Back to homepage' })
    expect(link).toHaveAttribute('href', '/')
  })

  it('swaps the main content but keeps the shell when a nav item is clicked', async () => {
    signedIn()

    const router = renderAt('/dashboard')
    await screen.findByRole('heading', { level: 1, name: 'Overview' })

    fireEvent.click(screen.getByRole('link', { name: 'Settings' }))

    await waitFor(() => expect(router.state.location.pathname).toBe('/settings'))
    expect(await screen.findByRole('heading', { level: 1, name: 'Settings' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { level: 1, name: 'Overview' })).not.toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument()
    expect(headerIsRendered()).toBe(false)
  })
})
