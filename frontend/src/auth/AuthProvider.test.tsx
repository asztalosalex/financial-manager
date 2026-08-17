import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import AuthProvider from './AuthProvider'
import { useAuth } from './useAuth'
import { api, setUnauthorizedHandler } from '../api/client'
import { emptyResponse, jsonResponse } from '../test/helpers'

const PROFILE = {
  id: 7,
  username: 'alex',
  email: 'alex@example.com',
  createdAt: '2026-01-01T10:00:00',
  lastLogin: '2026-08-01T08:00:00',
}

function LocationProbe() {
  const location = useLocation()
  return <span data-testid="location">{location.pathname}</span>
}

function AuthProbe() {
  const { status, user, logout } = useAuth()
  return (
    <>
      <span data-testid="status">{status}</span>
      <span data-testid="user">{user?.username ?? 'none'}</span>
      <button type="button" onClick={() => logout().catch(() => {})}>
        Log out
      </button>
      <button type="button" onClick={() => void api.get('/api/categories/user').catch(() => {})}>
        Load categories
      </button>
    </>
  )
}

function renderProvider() {
  return render(
    <MemoryRouter initialEntries={['/profile']}>
      <AuthProvider>
        <LocationProbe />
        <AuthProbe />
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('AuthProvider', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('starts in the loading state while the profile probe is in flight', async () => {
    vi.mocked(globalThis.fetch).mockReturnValue(new Promise<Response>(() => {}))

    renderProvider()

    expect(screen.getByTestId('status')).toHaveTextContent('loading')
    expect(screen.getByTestId('user')).toHaveTextContent('none')
  })

  it('probes GET /api/users/profile once on mount', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(200, PROFILE))

    renderProvider()

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))
    expect(vi.mocked(globalThis.fetch).mock.calls[0][0]).toBe('/api/users/profile')
  })

  it('becomes authenticated and exposes the user when the probe returns 200', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(200, PROFILE))

    renderProvider()

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))
    expect(screen.getByTestId('user')).toHaveTextContent('alex')
  })

  it('becomes anonymous when the probe returns 401 and does not redirect', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(emptyResponse(401))

    renderProvider()

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'))
    expect(screen.getByTestId('user')).toHaveTextContent('none')
    expect(screen.getByTestId('location')).toHaveTextContent('/profile')
  })

  it('becomes anonymous when the probe fails with a network error', async () => {
    vi.mocked(globalThis.fetch).mockRejectedValue(new TypeError('Failed to fetch'))

    renderProvider()

    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('anonymous'))
  })

  it('logs out through POST /api/auth/logout, clears the state and redirects home', async () => {
    vi.mocked(globalThis.fetch).mockImplementation((input) => {
      if (String(input) === '/api/auth/logout') {
        return Promise.resolve(emptyResponse(204))
      }
      return Promise.resolve(jsonResponse(200, PROFILE))
    })

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Log out' }))
    })

    const logoutCall = vi
      .mocked(globalThis.fetch)
      .mock.calls.find((call) => String(call[0]) === '/api/auth/logout')
    expect(logoutCall?.[1]?.method).toBe('POST')
    expect(screen.getByTestId('status')).toHaveTextContent('anonymous')
    expect(screen.getByTestId('user')).toHaveTextContent('none')
    expect(screen.getByTestId('location')).toHaveTextContent('/')
  })

  it('clears the state and redirects home even when the logout request keeps failing', async () => {
    vi.mocked(globalThis.fetch).mockImplementation((input) => {
      if (String(input) === '/api/auth/logout') {
        return Promise.reject(new TypeError('Failed to fetch'))
      }
      return Promise.resolve(jsonResponse(200, PROFILE))
    })

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))

    vi.useFakeTimers()
    try {
      fireEvent.click(screen.getByRole('button', { name: 'Log out' }))
      await act(async () => {
        await vi.runAllTimersAsync()
      })
    } finally {
      vi.useRealTimers()
    }

    const logoutCalls = vi
      .mocked(globalThis.fetch)
      .mock.calls.filter((call) => String(call[0]) === '/api/auth/logout')
    expect(logoutCalls.length).toBe(2)
    expect(screen.getByTestId('status')).toHaveTextContent('anonymous')
    expect(screen.getByTestId('user')).toHaveTextContent('none')
    expect(screen.getByTestId('location')).toHaveTextContent('/')
  })

  it('clears the session and redirects to /login when any call returns 401', async () => {
    vi.mocked(globalThis.fetch).mockImplementation((input) => {
      if (String(input) === '/api/categories/user') {
        return Promise.resolve(jsonResponse(401, { status: 401, message: 'Unauthorized' }))
      }
      return Promise.resolve(jsonResponse(200, PROFILE))
    })

    renderProvider()
    await waitFor(() => expect(screen.getByTestId('status')).toHaveTextContent('authenticated'))

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Load categories' }))
    })

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/login'))
    expect(screen.getByTestId('status')).toHaveTextContent('anonymous')
  })
})
