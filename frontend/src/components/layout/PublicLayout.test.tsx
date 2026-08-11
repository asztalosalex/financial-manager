import { describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import PublicLayout from './PublicLayout'
import { AuthContext, type AuthContextValue } from '../../auth/AuthContext'

function renderPublicLayout() {
  const auth: AuthContextValue = {
    status: 'anonymous',
    user: null,
    isAuthenticated: false,
    isLoading: false,
    setUser: vi.fn(),
    refresh: vi.fn(() => Promise.resolve()),
    logout: vi.fn(() => Promise.resolve()),
    clearSession: vi.fn(),
  }

  render(
    <MemoryRouter initialEntries={['/']}>
      <AuthContext.Provider value={auth}>
        <Routes>
          <Route element={<PublicLayout />}>
            <Route path="/" element={<p>Routed public content</p>} />
          </Route>
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

describe('PublicLayout', () => {
  it('keeps the Header above the routed page', () => {
    renderPublicLayout()

    expect(screen.getByRole('button', { name: 'Toggle navigation menu' })).toBeInTheDocument()
    expect(document.querySelector('.navbar')).not.toBeNull()
  })

  it('keeps the Footer below the routed page', () => {
    renderPublicLayout()

    expect(screen.getByRole('contentinfo')).toBeInTheDocument()
  })

  it('puts the routed page inside the main landmark', () => {
    renderPublicLayout()

    const main = screen.getByRole('main')
    expect(within(main).getByText('Routed public content')).toBeInTheDocument()
  })

  it('carries no sidebar navigation', () => {
    renderPublicLayout()

    expect(screen.queryByRole('navigation', { name: 'Main navigation' })).not.toBeInTheDocument()
  })
})
