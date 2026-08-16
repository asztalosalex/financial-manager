import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import TrendChart from './TrendChart'

const POINTS = [
  {
    monthLabel: 'Mar',
    incomeHeightPx: 100,
    expenseHeightPx: 70,
    incomeLabel: '430 000 Ft',
    expenseLabel: '298 000 Ft',
  },
  {
    monthLabel: 'Apr',
    incomeHeightPx: 90,
    expenseHeightPx: 60,
    incomeLabel: '410 000 Ft',
    expenseLabel: '250 000 Ft',
  },
]

describe('TrendChart', () => {
  it('renders one column group per point, in order, with the month label', () => {
    render(<TrendChart points={POINTS} isEmpty={false} />)

    expect(screen.getByText('Mar')).toBeInTheDocument()
    expect(screen.getByText('Apr')).toBeInTheDocument()
  })

  it('gives each column group an aria-label with the formatted income and expense values', () => {
    render(<TrendChart points={POINTS} isEmpty={false} />)

    expect(
      screen.getByLabelText('Mar: 430 000 Ft income, 298 000 Ft expense'),
    ).toBeInTheDocument()
    expect(
      screen.getByLabelText('Apr: 410 000 Ft income, 250 000 Ft expense'),
    ).toBeInTheDocument()
  })

  it('renders bar heights from the given pixel values', () => {
    const { container } = render(<TrendChart points={POINTS} isEmpty={false} />)

    const incomeBars = container.querySelectorAll('.trend-bar--income')
    const expenseBars = container.querySelectorAll('.trend-bar--expense')

    expect(incomeBars).toHaveLength(2)
    expect(expenseBars).toHaveLength(2)
    expect((incomeBars[0] as HTMLElement).style.height).toBe('100px')
    expect((expenseBars[0] as HTMLElement).style.height).toBe('70px')
  })

  it('shows an empty state and no columns when isEmpty is true', () => {
    const { container } = render(<TrendChart points={[]} isEmpty={true} />)

    expect(container.querySelector('.empty-state')).not.toBeNull()
    expect(container.querySelector('.trend-column-group')).toBeNull()
  })

  it('shows columns and no empty state when isEmpty is false', () => {
    const { container } = render(<TrendChart points={POINTS} isEmpty={false} />)

    expect(container.querySelector('.empty-state')).toBeNull()
    expect(container.querySelectorAll('.trend-column-group')).toHaveLength(2)
  })

  it('renders the static Income and Expense legend regardless of data', () => {
    render(<TrendChart points={POINTS} isEmpty={false} />)

    expect(screen.getByText('Income')).toBeInTheDocument()
    expect(screen.getByText('Expense')).toBeInTheDocument()
  })
})
