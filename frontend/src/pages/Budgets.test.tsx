import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import Budgets from './Budgets'
import { jsonResponse } from '../test/helpers'
import type { BudgetResponseDto, PageResponse } from '../api/types'

const EMPTY_PAGE: PageResponse<BudgetResponseDto> = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
}

describe('Budgets', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn())
    vi.mocked(globalThis.fetch).mockImplementation((input) => {
      const url = String(input)
      if (url.includes('/api/categories/user')) {
        return Promise.resolve(jsonResponse(200, []))
      }
      return Promise.resolve(jsonResponse(200, EMPTY_PAGE))
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('names the page with a single first level heading', async () => {
    render(<Budgets />)

    const headings = screen.getAllByRole('heading', { level: 1 })
    expect(headings).toHaveLength(1)
    expect(headings[0]).toHaveTextContent('Budgets')

    await screen.findByText('No budgets yet. Add your first budget to get started.')
  })

  it('renders the filter bar and the budget list surface', async () => {
    render(<Budgets />)

    expect(screen.getByLabelText('Month')).toBeInTheDocument()
    expect(screen.getByLabelText('Category')).toBeInTheDocument()

    await screen.findByText('No budgets yet. Add your first budget to get started.')
  })

  it('renders the New Budget button', async () => {
    render(<Budgets />)

    await screen.findByText('No budgets yet. Add your first budget to get started.')
    expect(screen.getByRole('button', { name: '+ New Budget' })).toBeInTheDocument()
  })
})
