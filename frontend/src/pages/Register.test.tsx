import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import Register from './Register'
import { setUnauthorizedHandler } from '../api/client'
import { errorResponse, jsonResponse } from '../test/helpers'
import type { RegisterUserDto, UserResponseDto } from '../api/types'

const CREATED_USER: UserResponseDto = {
  id: 7,
  username: 'alex',
  email: 'alex@example.com',
  createdAt: null,
  lastLogin: null,
}

function renderRegister() {
  render(
    <MemoryRouter initialEntries={['/register']}>
      <Routes>
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<div>Login page</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

function submitRegistration({ username, email, password }: RegisterUserDto) {
  fireEvent.change(screen.getByLabelText(/Username/), { target: { value: username } })
  fireEvent.change(screen.getByLabelText(/Email/), { target: { value: email } })
  fireEvent.change(screen.getByLabelText(/Password/), { target: { value: password } })
  return act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))
  })
}

describe('Register', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends the signup request and moves the user to the login page', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(jsonResponse(200, CREATED_USER)),
    )

    renderRegister()
    await submitRegistration({
      username: 'alex',
      email: 'alex@example.com',
      password: 'secret123',
    })

    const [url, init] = vi.mocked(globalThis.fetch).mock.calls[0]
    expect(url).toBe('/api/auth/signup')
    expect(init?.method).toBe('POST')
    expect(init?.credentials).toBe('include')
    expect((init?.headers as Headers).get('Content-Type')).toBe('application/json')
    expect(JSON.parse(init?.body as string)).toEqual({
      username: 'alex',
      email: 'alex@example.com',
      password: 'secret123',
    })
    expect(screen.getByText('Login page')).toBeInTheDocument()
  })

  it('renders 400 field errors beside their fields using the Java property keys', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        errorResponse(400, 'Validation failed', {
          username: 'Username must not be blank',
          email: 'Email must be a well-formed address',
          password: 'Password must be at least 8 characters long',
        }, '/api/auth/signup'),
      ),
    )

    renderRegister()
    await submitRegistration({ username: '', email: 'nope', password: 'short' })

    expect(screen.getByText('Username must not be blank')).toHaveClass('field-error')
    expect(screen.getByText('Email must be a well-formed address')).toHaveClass('field-error')
    expect(screen.getByText('Password must be at least 8 characters long')).toHaveClass('field-error')
    expect(screen.getByLabelText(/Username/)).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText(/Email/)).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText(/Password/)).toHaveAttribute('aria-invalid', 'true')
    expect(screen.queryByText('Login page')).not.toBeInTheDocument()
  })

  it('reads the 409 message from the ErrorResponse body', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        errorResponse(409, 'That username is already registered', undefined, '/api/auth/signup'),
      ),
    )

    renderRegister()
    await submitRegistration({
      username: 'alex',
      email: 'alex@example.com',
      password: 'secret123',
    })

    expect(screen.getByText('That username is already registered')).toHaveClass('auth-error')
    expect(screen.queryByText('That value is already taken.')).not.toBeInTheDocument()
    expect(screen.queryByText('Login page')).not.toBeInTheDocument()
  })
})
