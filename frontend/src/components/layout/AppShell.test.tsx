import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import AppShell from './AppShell'
import { AuthContext, type AuthContextValue } from '../../auth/AuthContext'
import type { UserResponseDto } from '../../api/types'
import { resizeViewportTo, stubViewportWidth } from '../../test/matchMedia'

const USER: UserResponseDto = {
  id: 7,
  username: 'alex',
  email: 'alex@example.com',
  createdAt: null,
  lastLogin: null,
}

const TOGGLE_LABEL = 'Toggle navigation sidebar'
const PANEL_ID = 'shell-sidebar-panel'

interface ShellOptions {
  width?: number
  path?: string
}

function renderShell({ width, path = '/dashboard' }: ShellOptions = {}) {
  if (width !== undefined) {
    stubViewportWidth(width)
  }

  const auth: AuthContextValue = {
    status: 'authenticated',
    user: USER,
    isAuthenticated: true,
    isLoading: false,
    setUser: vi.fn(),
    refresh: vi.fn(() => Promise.resolve()),
    logout: vi.fn(() => Promise.resolve()),
    clearSession: vi.fn(),
  }

  render(
    <MemoryRouter initialEntries={[path]}>
      <AuthContext.Provider value={auth}>
        <Routes>
          <Route element={<AppShell />}>
            <Route path="/dashboard" element={<p>Routed page content</p>} />
            <Route path="/transactions" element={<p>Transactions page content</p>} />
          </Route>
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

function toggleButton(): HTMLElement {
  return screen.getByRole('button', { name: TOGGLE_LABEL })
}

function panel(): HTMLElement {
  const element = document.getElementById(PANEL_ID)
  if (element === null) {
    throw new Error(`no element carries the panel id ${PANEL_ID}`)
  }
  return element
}

function layout(): HTMLElement {
  const element = document.querySelector('.shell-layout')
  if (element === null) {
    throw new Error('no element carries the shell-layout class')
  }
  return element as HTMLElement
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('AppShell', () => {
  it('puts the routed page inside the main landmark', () => {
    renderShell()

    const main = screen.getByRole('main')
    expect(within(main).getByText('Routed page content')).toBeInTheDocument()
  })

  it('renders the sidebar navigation beside the main column', () => {
    renderShell()

    expect(screen.getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument()
    expect(screen.getByRole('main')).toBeInTheDocument()
  })

  it('carries no Header and no Footer', () => {
    renderShell()

    expect(screen.queryByRole('contentinfo')).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Toggle navigation menu' })).not.toBeInTheDocument()
    expect(document.querySelector('.navbar')).toBeNull()
    expect(document.querySelector('.footer')).toBeNull()
  })
})

describe('AppShell mobile navigation panel', () => {
  it('offers a real button that controls the sidebar panel', () => {
    renderShell({ width: 800 })

    const toggle = toggleButton()
    expect(toggle.tagName).toBe('BUTTON')
    expect(toggle).toHaveAttribute('type', 'button')
    expect(toggle).toHaveAttribute('aria-controls', PANEL_ID)
    expect(panel()).toHaveClass('shell-sidebar')
    expect(within(panel()).getByRole('navigation', { name: 'Main navigation' })).toBeInTheDocument()
  })

  it('reports the panel state through aria-expanded', () => {
    renderShell({ width: 800 })

    expect(toggleButton()).toHaveAttribute('aria-expanded', 'false')

    fireEvent.click(toggleButton())
    expect(toggleButton()).toHaveAttribute('aria-expanded', 'true')

    fireEvent.click(toggleButton())
    expect(toggleButton()).toHaveAttribute('aria-expanded', 'false')
  })

  it('starts closed and keeps the closed panel out of the tab order', () => {
    renderShell({ width: 800 })

    expect(toggleButton()).toHaveAttribute('aria-expanded', 'false')
    expect(panel()).toHaveAttribute('inert')
    expect(layout()).not.toHaveClass('nav-open')
  })

  it('puts the opened panel back into the tab order', () => {
    renderShell({ width: 800 })

    fireEvent.click(toggleButton())

    expect(panel()).not.toHaveAttribute('inert')
    expect(layout()).toHaveClass('nav-open')
  })

  it('closes the open panel on Escape', () => {
    renderShell({ width: 800 })

    fireEvent.click(toggleButton())
    expect(toggleButton()).toHaveAttribute('aria-expanded', 'true')

    fireEvent.keyDown(document, { key: 'Escape' })

    expect(toggleButton()).toHaveAttribute('aria-expanded', 'false')
    expect(panel()).toHaveAttribute('inert')
  })

  it('leaves the panel open for keys other than Escape', () => {
    renderShell({ width: 800 })

    fireEvent.click(toggleButton())
    fireEvent.keyDown(document, { key: 'Enter' })

    expect(toggleButton()).toHaveAttribute('aria-expanded', 'true')
    expect(panel()).not.toHaveAttribute('inert')
  })

  it('closes the panel when a nav item navigates to another route', () => {
    renderShell({ width: 800 })

    fireEvent.click(toggleButton())
    expect(toggleButton()).toHaveAttribute('aria-expanded', 'true')

    fireEvent.click(within(panel()).getByRole('link', { name: 'Transactions' }))

    expect(screen.getByText('Transactions page content')).toBeInTheDocument()
    expect(toggleButton()).toHaveAttribute('aria-expanded', 'false')
    expect(panel()).toHaveAttribute('inert')
    expect(layout()).not.toHaveClass('nav-open')
  })

  it('keeps the toggle and the open panel reachable by focus', () => {
    renderShell({ width: 800 })

    const toggle = toggleButton()
    toggle.focus()
    expect(document.activeElement).toBe(toggle)

    fireEvent.click(toggle)

    const link = within(panel()).getByRole('link', { name: 'Transactions' })
    link.focus()
    expect(document.activeElement).toBe(link)

    const logout = within(panel()).getByRole('button', { name: 'Log out' })
    logout.focus()
    expect(document.activeElement).toBe(logout)
  })

  it('leaves the toggle out of the desktop layout and never collapses the panel there', () => {
    renderShell({ width: 1280 })

    expect(screen.queryByRole('button', { name: TOGGLE_LABEL })).not.toBeInTheDocument()
    expect(panel()).not.toHaveAttribute('inert')
    expect(layout()).not.toHaveClass('nav-open')
  })

  it('does not let the open panel state leak into the desktop layout', () => {
    renderShell({ width: 800 })

    fireEvent.click(toggleButton())
    expect(toggleButton()).toHaveAttribute('aria-expanded', 'true')

    act(() => {
      resizeViewportTo(1280)
    })

    expect(screen.queryByRole('button', { name: TOGGLE_LABEL })).not.toBeInTheDocument()
    expect(panel()).not.toHaveAttribute('inert')
    expect(layout()).not.toHaveClass('nav-open')

    act(() => {
      resizeViewportTo(800)
    })

    expect(toggleButton()).toHaveAttribute('aria-expanded', 'false')
    expect(panel()).toHaveAttribute('inert')
  })
})
