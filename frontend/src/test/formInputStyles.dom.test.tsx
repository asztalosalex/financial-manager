import { readSourceFile } from './nodeFileAccess'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import Login from '../pages/Login'
import Settings from '../pages/Settings'
import CategoriesTab from '../components/CategoriesTab'
import { AuthContext, type AuthContextValue } from '../auth/AuthContext'
import { setUnauthorizedHandler } from '../api/client'
import { clearCookies, jsonResponse } from './helpers'
import type { UserResponseDto } from '../api/types'

const NEUTRAL_STRONG_RGB = 'rgb(100, 116, 139)'
const ACCENT_RGB = 'rgb(79, 70, 229)'

function extractTokens(cssText: string): Map<string, string> {
  const rootBlockMatch = cssText.match(/:root\s*\{([^}]*)\}/)
  if (!rootBlockMatch) {
    throw new Error('no :root block found in index.css')
  }
  const tokens = new Map<string, string>()
  const declarationRegex = /--([a-zA-Z0-9-]+):\s*([^;]+);/g
  let match: RegExpExecArray | null
  while ((match = declarationRegex.exec(rootBlockMatch[1])) !== null) {
    tokens.set(match[1], match[2].trim())
  }
  return tokens
}

function resolveCssVariables(cssText: string, tokens: Map<string, string>): string {
  let resolved = cssText
  for (let pass = 0; pass < 3; pass += 1) {
    resolved = resolved.replace(/var\(--([a-zA-Z0-9-]+)\)/g, (whole, name: string) => {
      const value = tokens.get(name)
      return value ?? whole
    })
  }
  return resolved
}

const indexCssText = await readSourceFile('../index.css', import.meta.url)
const appCssText = await readSourceFile('../App.css', import.meta.url)
const tokens = extractTokens(indexCssText)
const jsdomFriendlyCss = resolveCssVariables(indexCssText + '\n' + appCssText, tokens)

it('the token map used to resolve var() references was actually populated from index.css', () => {
  expect(tokens.get('neutral-strong')).toBe('#64748B')
  expect(tokens.get('accent')).toBe('#4F46E5')
  expect(tokens.get('accent-soft')).toBe('#EEF2FF')
})

it('documents that jsdom does not resolve CSS custom properties in computed style at all', () => {
  const style = document.createElement('style')
  style.textContent = ':root { --probe: rgb(1, 2, 3); } .probe-target { border: 1px solid var(--probe); }'
  document.head.appendChild(style)
  const probe = document.createElement('div')
  probe.className = 'probe-target'
  document.body.appendChild(probe)
  const computed = getComputedStyle(probe).borderTopColor
  expect(computed).not.toBe('rgb(1, 2, 3)')
  probe.remove()
  style.remove()
})

let styleTag: HTMLStyleElement

beforeEach(() => {
  styleTag = document.createElement('style')
  styleTag.setAttribute('data-test-injected-app-css', 'true')
  styleTag.textContent = jsdomFriendlyCss
  document.head.appendChild(styleTag)
  setUnauthorizedHandler(null)
  vi.stubGlobal('fetch', vi.fn())
})

afterEach(() => {
  styleTag.remove()
  vi.unstubAllGlobals()
  clearCookies()
})

function renderLogin() {
  const auth = {
    status: 'anonymous',
    user: null,
    isAuthenticated: false,
    isLoading: false,
    setUser: vi.fn(),
    refresh: vi.fn(),
    logout: vi.fn(),
    clearSession: vi.fn(),
  } satisfies AuthContextValue

  render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthContext.Provider value={auth}>
        <Routes>
          <Route path="/login" element={<Login />} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )
}

const USER: UserResponseDto = {
  id: 7,
  username: 'alex',
  email: 'alex@example.com',
  createdAt: '2026-01-05T10:00:00.000Z',
  lastLogin: '2026-08-01T08:30:00.000Z',
}

function renderSettingsEditing() {
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
    <MemoryRouter initialEntries={['/settings']}>
      <AuthContext.Provider value={auth}>
        <Routes>
          <Route path="/settings" element={<Settings />} />
        </Routes>
      </AuthContext.Provider>
    </MemoryRouter>,
  )

  fireEvent.click(screen.getByRole('button', { name: 'Edit Profile' }))
}

async function renderCategoriesWithFormOpen() {
  vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(200, []))
  render(<CategoriesTab />)
  await screen.findByRole('heading', { level: 3, name: 'Your Categories' })
  fireEvent.click(screen.getByRole('button', { name: 'Add New Category' }))
}

describe('rendered form controls pick up the fixed contrast border at rest (var() pre-resolved for jsdom)', () => {
  it('an auth-form input (Login email) has the neutral-strong border, not the old low-contrast one', () => {
    renderLogin()
    const email = screen.getByLabelText(/Email/)
    const cs = getComputedStyle(email)
    expect(cs.borderTopColor).toBe(NEUTRAL_STRONG_RGB)
  })

  it('a settings form-input (profile email) has the neutral-strong border', () => {
    renderSettingsEditing()
    const email = screen.getByLabelText('Email:')
    const cs = getComputedStyle(email)
    expect(cs.borderTopColor).toBe(NEUTRAL_STRONG_RGB)
  })

  it('a category-form input (category name) has the neutral-strong border', async () => {
    await renderCategoriesWithFormOpen()
    const name = screen.getByLabelText('Category Name:')
    const cs = getComputedStyle(name)
    expect(cs.borderTopColor).toBe(NEUTRAL_STRONG_RGB)
  })

  it('a category-form textarea (description) has the neutral-strong border', async () => {
    await renderCategoriesWithFormOpen()
    const description = screen.getByLabelText('Description:')
    const cs = getComputedStyle(description)
    expect(cs.borderTopColor).toBe(NEUTRAL_STRONG_RGB)
  })
})

describe('rendered form controls pick up the accent focus treatment on :focus (var() pre-resolved for jsdom)', () => {
  it('an auth-form input shows the accent border-color and accent-soft box-shadow once focused', () => {
    renderLogin()
    const password = screen.getByLabelText(/Password/)
    act(() => {
      password.focus()
    })
    const cs = getComputedStyle(password)
    expect(cs.borderTopColor).toBe(ACCENT_RGB)
    expect(cs.boxShadow.toLowerCase()).toContain('#eef2ff')
    expect(cs.boxShadow).toContain('3px')
  })

  it('a category-form textarea shows the accent border-color once focused', async () => {
    await renderCategoriesWithFormOpen()
    const description = screen.getByLabelText('Description:')
    act(() => {
      description.focus()
    })
    const cs = getComputedStyle(description)
    expect(cs.borderTopColor).toBe(ACCENT_RGB)
  })

  it('a settings form-input shows the accent border-color once focused', () => {
    renderSettingsEditing()
    const username = screen.getByLabelText('Username:')
    act(() => {
      username.focus()
    })
    const cs = getComputedStyle(username)
    expect(cs.borderTopColor).toBe(ACCENT_RGB)
  })
})

describe('honest boundary check on :focus-visible in this jsdom environment', () => {
  it('jsdom reports an element as matching :focus-visible for any focus() call, mouse or keyboard alike', () => {
    renderLogin()
    const email = screen.getByLabelText(/Email/)
    act(() => {
      email.focus()
    })
    expect(email.matches(':focus-visible')).toBe(true)
  })

  it('jsdom does not apply the :focus-visible outline declaration in computed style, so it cannot be used as positive proof here', () => {
    renderLogin()
    const email = screen.getByLabelText(/Email/)
    act(() => {
      email.focus()
    })
    const cs = getComputedStyle(email)
    expect(cs.outlineStyle).not.toBe('solid')
  })
})
