import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import Transactions from './Transactions'

describe('Transactions', () => {
  it('names the page with a single first level heading', () => {
    render(<Transactions />)

    const headings = screen.getAllByRole('heading', { level: 1 })
    expect(headings).toHaveLength(1)
    expect(headings[0]).toHaveTextContent('Transactions')
  })

  it('carries the placeholder text over from the former tab, word for word', () => {
    render(<Transactions />)

    expect(screen.getByRole('heading', { level: 2, name: 'Income & Expenses' })).toBeInTheDocument()
    expect(screen.getByText('No income or expense data is available yet.')).toBeInTheDocument()
    expect(
      screen.getByText(/Transaction tracking and the monthly income, expense and balance totals/),
    ).toBeInTheDocument()
  })
})
