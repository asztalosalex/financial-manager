import { describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Header from './Header'
import { AuthContext, type AuthContextValue } from '../auth/AuthContext'
import type { UserResponseDto } from '../api/types'

const USER: UserResponseDto = {
  id: 7,
  username: 'alex',
  email: 'alex@example.com',
  createdAt: null,
  lastLogin: null,
}

const AUTHENTICATED: Partial<AuthContextValue> = {
  status: 'authenticated',
  user: USER,
  isAuthenticated: true,
  isLoading: false,
}

function createDeferred() {
  let settle: () => void = () => {}
  const promise = new Promise<void>((resolve) => {
    settle = resolve
  })
  return { promise, settle: () => settle() }
}

function renderHeader(overrides: Partial<AuthContextValue> = {}) {
  const auth: AuthContextValue = {
    status: 'anonymous',
    user: null,
    isAuthenticated: false,
    isLoading: false,
    setUser: vi.fn(),
    refresh: vi.fn(() => Promise.resolve()),
    logout: vi.fn(() => Promise.resolve()),
    clearSession: vi.fn(),
    ...overrides,
  }

  render(
    <MemoryRouter>
      <AuthContext.Provider value={auth}>
        <Header />
      </AuthContext.Provider>
    </MemoryRouter>,
  )

  return auth
}

function navMenu(): HTMLElement {
  const menu = document.querySelector('.nav-menu')
  if (menu === null) {
    throw new Error('the navigation menu is not rendered')
  }
  return menu as HTMLElement
}

function toggleButton() {
  return screen.getByRole('button', { name: 'Toggle navigation menu' })
}

describe('Header', () => {
  it('offers the sign-in entry point to anonymous visitors', () => {
    renderHeader()

    expect(screen.getByRole('link', { name: 'Get Started' })).toHaveAttribute('href', '/login')
    expect(screen.queryByRole('link', { name: 'Profile' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Log out' })).toBeNull()
  })

  it('offers neither sign-in nor log out while the session is still unknown', () => {
    renderHeader({ status: 'loading', isLoading: true })

    expect(screen.getByText('Loading...')).toHaveAttribute('aria-live', 'polite')
    expect(screen.queryByRole('link', { name: 'Get Started' })).toBeNull()
    expect(screen.queryByRole('button', { name: 'Log out' })).toBeNull()
  })

  it('offers the profile link and log out action to a signed-in user', () => {
    renderHeader(AUTHENTICATED)

    expect(screen.getByRole('link', { name: 'Profile' })).toHaveAttribute('href', '/profile')
    expect(screen.getByRole('button', { name: 'Log out' })).toBeEnabled()
    expect(screen.queryByRole('link', { name: 'Get Started' })).toBeNull()
  })

  it('calls logout once and blocks the button until the request settles', async () => {
    const pending = createDeferred()
    const logout = vi.fn(() => pending.promise)
    renderHeader({ ...AUTHENTICATED, logout })

    fireEvent.click(screen.getByRole('button', { name: 'Log out' }))

    expect(logout).toHaveBeenCalledTimes(1)
    expect(screen.getByRole('button', { name: 'Logging out...' })).toBeDisabled()

    await act(async () => {
      pending.settle()
    })

    expect(screen.getByRole('button', { name: 'Log out' })).toBeEnabled()
    expect(logout).toHaveBeenCalledTimes(1)
  })

  it('does not drop the session itself when the sign-out request succeeds', async () => {
    const logout = vi.fn(() => Promise.resolve())
    const clearSession = vi.fn()
    renderHeader({ ...AUTHENTICATED, logout, clearSession })

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Log out' }))
    })

    expect(logout).toHaveBeenCalledTimes(1)
    expect(clearSession).not.toHaveBeenCalled()
    expect(screen.getByRole('button', { name: 'Log out' })).toBeEnabled()
  })

  it('drops the session and releases the button when the sign-out request rejects', async () => {
    const logout = vi.fn(() => Promise.reject(new Error('network down')))
    const clearSession = vi.fn()
    renderHeader({ ...AUTHENTICATED, logout, clearSession })

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Log out' }))
    })

    expect(logout).toHaveBeenCalledTimes(1)
    expect(clearSession).toHaveBeenCalledTimes(1)
    expect(screen.queryByRole('button', { name: 'Logging out...' })).toBeNull()
    expect(screen.getByRole('button', { name: 'Log out' })).toBeEnabled()
  })

  it('lets the user retry after a rejected sign-out', async () => {
    let shouldFail = true
    const logout = vi.fn(() =>
      shouldFail ? Promise.reject(new Error('network down')) : Promise.resolve(),
    )
    renderHeader({ ...AUTHENTICATED, logout })

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Log out' }))
    })

    shouldFail = false
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Log out' }))
    })

    expect(logout).toHaveBeenCalledTimes(2)
    expect(screen.getByRole('button', { name: 'Log out' })).toBeEnabled()
  })

  it('opens and closes the mobile menu from the toggle button', () => {
    renderHeader()

    expect(toggleButton()).toHaveAttribute('aria-expanded', 'false')
    expect(navMenu()).not.toHaveClass('open')

    fireEvent.click(toggleButton())
    expect(toggleButton()).toHaveAttribute('aria-expanded', 'true')
    expect(navMenu()).toHaveClass('open')

    fireEvent.click(toggleButton())
    expect(toggleButton()).toHaveAttribute('aria-expanded', 'false')
    expect(navMenu()).not.toHaveClass('open')
  })

  it('closes the open menu when a navigation link is used', () => {
    renderHeader()

    fireEvent.click(toggleButton())
    expect(navMenu()).toHaveClass('open')

    fireEvent.click(screen.getByRole('link', { name: 'Features' }))

    expect(navMenu()).not.toHaveClass('open')
    expect(toggleButton()).toHaveAttribute('aria-expanded', 'false')
  })
})
