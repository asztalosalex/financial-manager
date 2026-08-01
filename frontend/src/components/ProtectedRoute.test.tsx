import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './ProtectedRoute'
import AuthProvider from '../auth/AuthProvider'
import { setUnauthorizedHandler } from '../api/client'
import { emptyResponse, jsonResponse } from '../test/helpers'

const PROFILE = {
  id: 7,
  username: 'alex',
  email: 'alex@example.com',
  createdAt: null,
  lastLogin: null,
}

function renderProtected() {
  return render(
    <MemoryRouter initialEntries={['/profile']}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<div>Login page</div>} />
          <Route element={<ProtectedRoute />}>
            <Route path="/profile" element={<div>Secret profile</div>} />
          </Route>
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('shows a loading placeholder while the session is unknown', () => {
    vi.mocked(globalThis.fetch).mockReturnValue(new Promise<Response>(() => {}))

    renderProtected()

    expect(screen.getByRole('status')).toBeInTheDocument()
    expect(screen.queryByText('Secret profile')).not.toBeInTheDocument()
    expect(screen.queryByText('Login page')).not.toBeInTheDocument()
  })

  it('never flashes the protected content before the session is known', () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(200, PROFILE))

    renderProtected()

    expect(screen.queryByText('Secret profile')).not.toBeInTheDocument()
  })

  it('renders the protected content once the session is authenticated', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(200, PROFILE))

    renderProtected()

    await waitFor(() => expect(screen.getByText('Secret profile')).toBeInTheDocument())
    expect(screen.queryByText('Login page')).not.toBeInTheDocument()
  })

  it('redirects to /login when there is no session', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(emptyResponse(401))

    renderProtected()

    await waitFor(() => expect(screen.getByText('Login page')).toBeInTheDocument())
    expect(screen.queryByText('Secret profile')).not.toBeInTheDocument()
  })
})
