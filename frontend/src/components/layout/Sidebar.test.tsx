import { describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import Sidebar from './Sidebar'
import { AuthContext, type AuthContextValue } from '../../auth/AuthContext'
import type { UserResponseDto } from '../../api/types'

const USER: UserResponseDto = {
  id: 7,
  username: 'alex',
  email: 'alex@example.com',
  createdAt: null,
  lastLogin: null,
}

const NAV_LABELS = ['Overview', 'Transactions', 'Categories', 'Settings']
const NAV_HREFS = ['/dashboard', '/transactions', '/categories', '/settings']

function renderSidebar(path = '/dashboard', overrides: Partial<AuthContextValue> = {}) {
  const auth: AuthContextValue = {
    status: 'authenticated',
    user: USER,
    isAuthenticated: true,
    isLoading: false,
    setUser: vi.fn(),
    refresh: vi.fn(() => Promise.resolve()),
    logout: vi.fn(() => Promise.resolve()),
    clearSession: vi.fn(),
    ...overrides,
  }

  render(
    <MemoryRouter initialEntries={[path]}>
      <AuthContext.Provider value={auth}>
        <Routes>
          <Route path="*" element={<Sidebar />} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )

  return auth
}

function navLinks(): HTMLElement[] {
  const nav = screen.getByRole('navigation', { name: 'Main navigation' })
  return within(nav).getAllByRole('link')
}

describe('Sidebar', () => {
  it('lists the four destinations in the agreed order', () => {
    renderSidebar()

    expect(navLinks().map((link) => link.textContent)).toEqual(NAV_LABELS)
  })

  it('points every nav item at its own route, in the same order', () => {
    renderSidebar()

    expect(navLinks().map((link) => link.getAttribute('href'))).toEqual(NAV_HREFS)
  })

  it('shows no destination that has no page behind it', () => {
    renderSidebar()

    expect(navLinks()).toHaveLength(4)
    expect(screen.queryByRole('link', { name: 'Budget' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Reports' })).not.toBeInTheDocument()
    expect(screen.queryByText('Budget')).not.toBeInTheDocument()
    expect(screen.queryByText('Reports')).not.toBeInTheDocument()
  })

  it('marks the nav item of the current route with aria-current', () => {
    renderSidebar('/transactions')

    expect(screen.getByRole('link', { name: 'Transactions' })).toHaveAttribute('aria-current', 'page')
  })

  it('leaves aria-current off every nav item that is not the current route', () => {
    renderSidebar('/transactions')

    expect(screen.getByRole('link', { name: 'Overview' })).not.toHaveAttribute('aria-current')
    expect(screen.getByRole('link', { name: 'Categories' })).not.toHaveAttribute('aria-current')
    expect(screen.getByRole('link', { name: 'Settings' })).not.toHaveAttribute('aria-current')
  })

  it('moves aria-current with the route', () => {
    renderSidebar('/settings')

    expect(screen.getByRole('link', { name: 'Settings' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Transactions' })).not.toHaveAttribute('aria-current')
  })

  it('marks the active nav item for CSS as well as for assistive technology', () => {
    renderSidebar('/categories')

    expect(screen.getByRole('link', { name: 'Categories' })).toHaveClass('active')
    expect(screen.getByRole('link', { name: 'Overview' })).not.toHaveClass('active')
  })

  it('builds the navigation as a list inside a nav landmark', () => {
    renderSidebar()

    const nav = screen.getByRole('navigation', { name: 'Main navigation' })
    const list = within(nav).getByRole('list')
    expect(list.parentElement).toBe(nav)
    expect(list.tagName).toBe('UL')

    const items = within(list).getAllByRole('listitem')
    expect(items).toHaveLength(4)
    items.forEach((item) => {
      expect(item.tagName).toBe('LI')
      expect(within(item).getByRole('link')).toBeInTheDocument()
    })
  })

  it('uses real anchors and a real button, never a clickable div', () => {
    renderSidebar()

    navLinks().forEach((link) => expect(link.tagName).toBe('A'))

    const logout = screen.getByRole('button', { name: 'Log out' })
    expect(logout.tagName).toBe('BUTTON')
    expect(logout).toHaveAttribute('type', 'button')
  })

  it('shows the signed-in name and e-mail beside a decorative avatar', () => {
    renderSidebar()

    expect(screen.getByText('alex')).toBeInTheDocument()
    expect(screen.getByText('alex@example.com')).toBeInTheDocument()

    const avatar = document.querySelector('.shell-avatar')
    expect(avatar).not.toBeNull()
    expect(avatar).toHaveAttribute('aria-hidden', 'true')
  })

  it('keeps the brand name of the repository, not the one from the design draft', () => {
    renderSidebar()

    expect(screen.getByAltText('Financial Manager Logo')).toBeInTheDocument()
    expect(screen.queryByText('Fintra')).not.toBeInTheDocument()
  })

  it('leaves the data-driven savings widget out of this slice', () => {
    renderSidebar()

    expect(screen.queryByText(/Savings/)).not.toBeInTheDocument()
  })

  it('logs the user out from the sidebar button', async () => {
    const auth = renderSidebar()

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Log out' }))
    })

    expect(auth.logout).toHaveBeenCalledTimes(1)
    expect(auth.clearSession).not.toHaveBeenCalled()
  })

  it('drops the local session when the logout request fails', async () => {
    const auth = renderSidebar('/dashboard', {
      logout: vi.fn(() => Promise.reject(new Error('network'))),
    })

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Log out' }))
    })

    expect(auth.logout).toHaveBeenCalledTimes(1)
    expect(auth.clearSession).toHaveBeenCalledTimes(1)
  })
})
