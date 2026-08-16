import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import Transactions from './Transactions'
import { jsonResponse } from '../test/helpers'
import type { PageResponse, TransactionResponseDto } from '../api/types'

const EMPTY_PAGE: PageResponse<TransactionResponseDto> = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
}

describe('Transactions', () => {
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
    render(<Transactions />)

    const headings = screen.getAllByRole('heading', { level: 1 })
    expect(headings).toHaveLength(1)
    expect(headings[0]).toHaveTextContent('Transactions')

    await screen.findByText('No transactions yet. Add your first transaction to get started.')
  })

  it('renders the filter bar and the transaction list surface', async () => {
    render(<Transactions />)

    expect(screen.getByLabelText('Type')).toBeInTheDocument()
    expect(screen.getByLabelText('Category')).toBeInTheDocument()
    expect(screen.getByLabelText('From')).toBeInTheDocument()
    expect(screen.getByLabelText('To')).toBeInTheDocument()

    await screen.findByText('No transactions yet. Add your first transaction to get started.')
  })

  it('renders a New Transaction button next to the page heading', async () => {
    render(<Transactions />)

    await screen.findByText('No transactions yet. Add your first transaction to get started.')
    expect(screen.getByRole('button', { name: '+ New Transaction' })).toBeInTheDocument()
  })
})
