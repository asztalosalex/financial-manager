import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import Home from './Home'
import { setUnauthorizedHandler } from '../api/client'
import { errorResponse, jsonResponse } from '../test/helpers'

function renderHome() {
  render(
    <MemoryRouter initialEntries={['/']}>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/register" element={<div>Register page</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

function clickLink(name: string) {
  return act(async () => {
    fireEvent.click(screen.getByRole('link', { name }))
  })
}

describe('Home', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('routes the hero call to action without a full page load', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() => Promise.resolve(jsonResponse(200, 42)))

    renderHome()
    await clickLink('Start Managing')

    expect(screen.getByText('Register page')).toBeInTheDocument()
  })

  it('routes the closing call to action without a full page load', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() => Promise.resolve(jsonResponse(200, 42)))

    renderHome()
    await clickLink('Get Started Today')

    expect(screen.getByText('Register page')).toBeInTheDocument()
  })

  it('keeps the in-page anchor as a plain link', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() => Promise.resolve(jsonResponse(200, 42)))

    renderHome()
    await act(async () => {})

    expect(screen.getByRole('link', { name: 'Learn More' })).toHaveAttribute('href', '#features')
  })

  it('shows the live user count next to the unshipped-feature placeholders', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() => Promise.resolve(jsonResponse(200, 42)))

    renderHome()

    expect(await screen.findAllByText('42')).toHaveLength(2)
    expect(vi.mocked(globalThis.fetch).mock.calls[0][0]).toBe('/api/users/count')
    expect(screen.getAllByText('—')).toHaveLength(2)
  })

  it('degrades to an unavailable label when the count endpoint fails', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(errorResponse(500, 'Something went wrong', undefined, '/api/users/count')),
    )

    renderHome()

    expect(await screen.findAllByText('Unavailable')).toHaveLength(2)
    expect(screen.getAllByText('—')).toHaveLength(2)
  })
})
