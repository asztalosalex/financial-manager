import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import Profile from './Profile'
import { AuthContext, type AuthContextValue } from '../auth/AuthContext'
import { setUnauthorizedHandler } from '../api/client'
import { emptyResponse, errorResponse, jsonResponse } from '../test/helpers'
import type { UpdateProfileDto, UserResponseDto } from '../api/types'

const USER: UserResponseDto = {
  id: 7,
  username: 'alex',
  email: 'alex@example.com',
  createdAt: '2026-01-05T10:00:00.000Z',
  lastLogin: '2026-08-01T08:30:00.000Z',
}

function renderProfile() {
  const auth = {
    status: 'authenticated',
    user: USER,
    isAuthenticated: true,
    isLoading: false,
    setUser: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
    clearSession: vi.fn(),
  } satisfies AuthContextValue

  render(
    <MemoryRouter initialEntries={['/profile']}>
      <AuthContext.Provider value={auth}>
        <Routes>
          <Route path="/" element={<div>Landing page</div>} />
          <Route path="/profile" element={<Profile />} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )

  return auth
}

function submitProfileEdit({ username, email }: UpdateProfileDto) {
  fireEvent.click(screen.getByRole('button', { name: 'Edit Profile' }))
  fireEvent.change(screen.getByLabelText('Username:'), { target: { value: username } })
  fireEvent.change(screen.getByLabelText('Email:'), { target: { value: email } })
  return act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }))
  })
}

function profileEditForm(): HTMLFormElement {
  const form = screen.getByLabelText('Email:').closest('form')
  if (form === null) {
    throw new Error('the profile edit form is not rendered')
  }
  return form
}

function clickDeleteAccount() {
  return act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Delete Account' }))
  })
}

function lastRequest() {
  const calls = vi.mocked(globalThis.fetch).mock.calls
  return calls[calls.length - 1]
}

describe('Profile', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('promotes the updated user into the session after a 200 save', async () => {
    const updated: UserResponseDto = {
      ...USER,
      username: 'alexandra',
      email: 'alexandra@example.com',
    }
    vi.mocked(globalThis.fetch).mockImplementation(() => Promise.resolve(jsonResponse(200, updated)))

    const auth = renderProfile()
    await submitProfileEdit({ username: 'alexandra', email: 'alexandra@example.com' })

    const [url, init] = lastRequest()
    expect(url).toBe('/api/users/7')
    expect(init?.method).toBe('PUT')
    expect((init?.headers as Headers).get('Content-Type')).toBe('application/json')
    expect(init?.credentials).toBe('include')
    expect(JSON.parse(init?.body as string)).toEqual({
      username: 'alexandra',
      email: 'alexandra@example.com',
    })
    expect(auth.setUser).toHaveBeenCalledTimes(1)
    expect(auth.setUser).toHaveBeenCalledWith(updated)
    expect(screen.getByText('Profile updated successfully.')).toHaveClass('auth-success')
  })

  it('saves on form submit and stops the browser from leaving the page', async () => {
    const updated: UserResponseDto = { ...USER, email: 'submitted@example.com' }
    vi.mocked(globalThis.fetch).mockImplementation(() => Promise.resolve(jsonResponse(200, updated)))

    const auth = renderProfile()
    fireEvent.click(screen.getByRole('button', { name: 'Edit Profile' }))
    fireEvent.change(screen.getByLabelText('Email:'), {
      target: { value: 'submitted@example.com' },
    })

    const submitEvent = new Event('submit', { bubbles: true, cancelable: true })
    await act(async () => {
      profileEditForm().dispatchEvent(submitEvent)
    })

    expect(submitEvent.defaultPrevented).toBe(true)
    expect(globalThis.fetch).toHaveBeenCalledTimes(1)
    const [url, init] = lastRequest()
    expect(url).toBe('/api/users/7')
    expect(init?.method).toBe('PUT')
    expect(JSON.parse(init?.body as string)).toEqual({
      username: 'alex',
      email: 'submitted@example.com',
    })
    expect(auth.setUser).toHaveBeenCalledWith(updated)
    expect(screen.getByText('Profile updated successfully.')).toBeInTheDocument()
  })

  it('places 400 field errors beside their fields using the Java property keys', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        errorResponse(400, 'Validation failed', {
          username: 'Username must not be blank',
          email: 'Email must be a well-formed address',
        }, '/api/users/7'),
      ),
    )

    const auth = renderProfile()
    await submitProfileEdit({ username: '', email: 'not-an-email' })

    expect(screen.getByText('Username must not be blank')).toHaveClass('field-error')
    expect(screen.getByText('Email must be a well-formed address')).toHaveClass('field-error')
    expect(screen.getByLabelText('Username:')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Email:')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Validation failed')).toHaveClass('auth-error')
    expect(auth.setUser).not.toHaveBeenCalled()
    expect(screen.getByRole('button', { name: 'Save Changes' })).toBeInTheDocument()
  })

  it('shows the conflict message carried by the ErrorResponse body, not a generic default', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(errorResponse(409, 'That email is already registered', undefined, '/api/users/7')),
    )

    const auth = renderProfile()
    await submitProfileEdit({ username: 'alex', email: 'taken@example.com' })

    expect(screen.getByText('That email is already registered')).toHaveClass('auth-error')
    expect(screen.queryByText('That value is already taken.')).not.toBeInTheDocument()
    expect(auth.setUser).not.toHaveBeenCalled()
  })

  it('retries a failed save without exhausting the response body', async () => {
    let attempt = 0
    vi.mocked(globalThis.fetch).mockImplementation(() => {
      attempt += 1
      return Promise.resolve(
        attempt === 1
          ? errorResponse(409, 'That email is already registered', undefined, '/api/users/7')
          : jsonResponse(200, { ...USER, email: 'free@example.com' }),
      )
    })

    const auth = renderProfile()
    await submitProfileEdit({ username: 'alex', email: 'taken@example.com' })
    expect(screen.getByText('That email is already registered')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Email:'), { target: { value: 'free@example.com' } })
    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }))
    })

    expect(vi.mocked(globalThis.fetch)).toHaveBeenCalledTimes(2)
    expect(auth.setUser).toHaveBeenCalledWith({ ...USER, email: 'free@example.com' })
    expect(screen.getByText('Profile updated successfully.')).toBeInTheDocument()
  })

  it('clears the session and leaves the page after a 204 account delete', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(globalThis.fetch).mockImplementation(() => Promise.resolve(emptyResponse(204)))

    const auth = renderProfile()
    await clickDeleteAccount()

    const [url, init] = lastRequest()
    expect(url).toBe('/api/users/7')
    expect(init?.method).toBe('DELETE')
    expect(auth.clearSession).toHaveBeenCalledTimes(1)
    await waitFor(() => expect(screen.getByText('Landing page')).toBeInTheDocument())
  })

  it('keeps the session when the account delete fails', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(errorResponse(404, 'User not found', undefined, '/api/users/7')),
    )

    const auth = renderProfile()
    await clickDeleteAccount()

    expect(screen.getByText('User not found')).toHaveClass('auth-error')
    expect(auth.clearSession).not.toHaveBeenCalled()
    expect(screen.queryByText('Landing page')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete Account' })).toBeEnabled()
  })

  it('does not call the API when the delete confirmation is dismissed', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)

    const auth = renderProfile()
    await clickDeleteAccount()

    expect(globalThis.fetch).not.toHaveBeenCalled()
    expect(auth.clearSession).not.toHaveBeenCalled()
  })
})
