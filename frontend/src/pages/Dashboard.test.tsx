import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import Dashboard from './Dashboard'

describe('Dashboard', () => {
  it('names the page with a single first level heading', () => {
    render(<Dashboard />)

    const headings = screen.getAllByRole('heading', { level: 1 })
    expect(headings).toHaveLength(1)
    expect(headings[0]).toHaveTextContent('Overview')
  })

  it('says in words that there is nothing to show yet', () => {
    render(<Dashboard />)

    expect(screen.getByText('There is nothing to show here yet.')).toBeInTheDocument()
  })

  it('explains why the page is empty instead of showing zeroes', () => {
    const { container } = render(<Dashboard />)

    expect(screen.getByText(/not connected to the server yet/)).toBeInTheDocument()
    expect(container.querySelector('.empty-state')).not.toBeNull()
    expect(container.textContent).not.toMatch(/\d/)
  })
})
