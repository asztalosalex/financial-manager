import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import Login from './Login'
import { AuthContext, type AuthContextValue } from '../auth/AuthContext'
import { setUnauthorizedHandler } from '../api/client'
import { errorResponse, jsonResponse } from '../test/helpers'
import type { LoginUserDto } from '../api/types'

function renderLogin() {
  const auth = {
    status: 'anonymous',
    user: null,
    isAuthenticated: false,
    isLoading: false,
    setUser: vi.fn(),
    refresh: vi.fn().mockResolvedValue(undefined),
    logout: vi.fn(),
    clearSession: vi.fn(),
  } satisfies AuthContextValue

  render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthContext.Provider value={auth}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/profile" element={<div>Profile page</div>} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )

  return auth
}

function submitLogin({ email, password }: LoginUserDto) {
  fireEvent.change(screen.getByLabelText(/Email/), { target: { value: email } })
  fireEvent.change(screen.getByLabelText(/Password/), { target: { value: password } })
  return act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Log in' }))
  })
}

describe('Login', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('refreshes the session and redirects after a successful login', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(jsonResponse(200, { expiresIn: 3600, message: 'success' })),
    )

    const auth = renderLogin()
    await submitLogin({ email: 'alex@example.com', password: 'secret123' })

    const [url, init] = vi.mocked(globalThis.fetch).mock.calls[0]
    expect(url).toBe('/api/auth/login')
    expect(init?.method).toBe('POST')
    expect(init?.credentials).toBe('include')
    expect((init?.headers as Headers).get('Content-Type')).toBe('application/json')
    expect(JSON.parse(init?.body as string)).toEqual({
      email: 'alex@example.com',
      password: 'secret123',
    })
    expect(auth.refresh).toHaveBeenCalledTimes(1)
    expect(screen.getByText('Profile page')).toBeInTheDocument()
  })

  it('translates the 401 invalid_credentials code into display text', async () => {
    const unauthorizedHandler = vi.fn()
    setUnauthorizedHandler(unauthorizedHandler)
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(jsonResponse(401, { expiresIn: null, message: 'invalid_credentials' })),
    )

    const auth = renderLogin()
    await submitLogin({ email: 'alex@example.com', password: 'wrong' })

    expect(screen.getByText('Incorrect email or password.')).toHaveClass('auth-error')
    expect(screen.queryByText('invalid_credentials')).not.toBeInTheDocument()
    expect(screen.queryByText('Your session has expired. Please log in again.')).not.toBeInTheDocument()
    expect(unauthorizedHandler).not.toHaveBeenCalled()
    expect(auth.refresh).not.toHaveBeenCalled()
    expect(screen.queryByText('Profile page')).not.toBeInTheDocument()
  })

  it('lets a corrected password succeed after a rejected attempt', async () => {
    let attempt = 0
    vi.mocked(globalThis.fetch).mockImplementation(() => {
      attempt += 1
      return Promise.resolve(
        attempt === 1
          ? jsonResponse(401, { expiresIn: null, message: 'invalid_credentials' })
          : jsonResponse(200, { expiresIn: 3600, message: 'success' }),
      )
    })

    const auth = renderLogin()
    await submitLogin({ email: 'alex@example.com', password: 'wrong' })
    expect(screen.getByText('Incorrect email or password.')).toBeInTheDocument()

    await submitLogin({ email: 'alex@example.com', password: 'secret123' })

    expect(vi.mocked(globalThis.fetch)).toHaveBeenCalledTimes(2)
    expect(auth.refresh).toHaveBeenCalledTimes(1)
    expect(screen.getByText('Profile page')).toBeInTheDocument()
  })

  it('renders 400 field errors beside their fields using the Java property keys', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        errorResponse(400, 'Validation failed', {
          email: 'Email must be a well-formed address',
          password: 'Password must not be blank',
        }, '/api/auth/login'),
      ),
    )

    const auth = renderLogin()
    await submitLogin({ email: 'not-an-email', password: '' })

    expect(screen.getByText('Email must be a well-formed address')).toHaveClass('field-error')
    expect(screen.getByText('Password must not be blank')).toHaveClass('field-error')
    expect(screen.getByLabelText(/Email/)).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText(/Password/)).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Validation failed')).toHaveClass('auth-error')
    expect(auth.refresh).not.toHaveBeenCalled()
  })
})
