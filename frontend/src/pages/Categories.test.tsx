import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import Categories from './Categories'
import { setUnauthorizedHandler } from '../api/client'
import { jsonResponse } from '../test/helpers'

describe('Categories', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
    vi.mocked(globalThis.fetch).mockImplementation(() => Promise.resolve(jsonResponse(200, [])))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('names the page with a single first level heading', async () => {
    render(<Categories />)

    const headings = screen.getAllByRole('heading', { level: 1 })
    expect(headings).toHaveLength(1)
    expect(headings[0]).toHaveTextContent('Categories')

    await screen.findByRole('heading', { level: 3, name: 'Your Categories' })
  })

  it('hands the surface over to the category component unchanged', async () => {
    render(<Categories />)

    await screen.findByRole('heading', { level: 3, name: 'Your Categories' })
    expect(screen.getByRole('button', { name: 'Add New Category' })).toBeInTheDocument()
    expect(vi.mocked(globalThis.fetch).mock.calls[0]?.[0]).toBe('/api/categories/user')
  })
})
