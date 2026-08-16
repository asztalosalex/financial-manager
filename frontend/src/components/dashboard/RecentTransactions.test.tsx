import type { ReactElement } from 'react'
import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import RecentTransactions, { type RecentTransactionItem } from './RecentTransactions'

const ITEMS: RecentTransactionItem[] = [
  {
    id: 1,
    isIncome: true,
    description: 'Salary',
    categoryLabel: 'Salary · Today',
    amountLabel: '+500 000 Ft',
  },
  {
    id: 2,
    isIncome: false,
    description: 'Groceries',
    categoryLabel: 'Food · Aug 25',
    amountLabel: '−8 200 Ft',
  },
]

function renderWithRouter(ui: ReactElement) {
  return render(<MemoryRouter>{ui}</MemoryRouter>)
}

describe('RecentTransactions', () => {
  it('renders every item, in the given order, with its description, category label, and amount', () => {
    renderWithRouter(
      <RecentTransactions items={ITEMS} isEmpty={false} viewAllHref="/transactions" />,
    )

    const descriptions = screen.getAllByText(/^(Salary|Groceries)$/)
    expect(descriptions.map((el) => el.textContent)).toEqual(['Salary', 'Groceries'])
    expect(screen.getByText('Salary · Today')).toBeInTheDocument()
    expect(screen.getByText('Food · Aug 25')).toBeInTheDocument()
    expect(screen.getByText('+500 000 Ft')).toBeInTheDocument()
    expect(screen.getByText('−8 200 Ft')).toBeInTheDocument()
  })

  it('gives the income row the income amount class and the expense row the expense amount class', () => {
    renderWithRouter(
      <RecentTransactions items={ITEMS} isEmpty={false} viewAllHref="/transactions" />,
    )

    expect(screen.getByText('+500 000 Ft')).toHaveClass('recent-transaction-amount--income')
    expect(screen.getByText('+500 000 Ft')).not.toHaveClass('recent-transaction-amount--expense')

    expect(screen.getByText('−8 200 Ft')).toHaveClass('recent-transaction-amount--expense')
    expect(screen.getByText('−8 200 Ft')).not.toHaveClass('recent-transaction-amount--income')
  })

  it('gives the income row the income icon tile and the expense row the expense icon tile', () => {
    const { container } = renderWithRouter(
      <RecentTransactions items={ITEMS} isEmpty={false} viewAllHref="/transactions" />,
    )

    const rows = container.querySelectorAll('.recent-transaction-item')
    expect(rows).toHaveLength(2)
    expect(rows[0].querySelector('.recent-transaction-icon--income')).not.toBeNull()
    expect(rows[0].querySelector('.recent-transaction-icon--expense')).toBeNull()
    expect(rows[1].querySelector('.recent-transaction-icon--expense')).not.toBeNull()
    expect(rows[1].querySelector('.recent-transaction-icon--income')).toBeNull()
  })

  it('shows an empty state and no list when isEmpty is true', () => {
    const { container } = renderWithRouter(
      <RecentTransactions items={[]} isEmpty={true} viewAllHref="/transactions" />,
    )

    expect(container.querySelector('.empty-state')).not.toBeNull()
    expect(container.querySelector('.recent-transactions-list')).toBeNull()
  })

  it('shows the list and no empty state when isEmpty is false', () => {
    const { container } = renderWithRouter(
      <RecentTransactions items={ITEMS} isEmpty={false} viewAllHref="/transactions" />,
    )

    expect(container.querySelector('.empty-state')).toBeNull()
    expect(container.querySelector('.recent-transactions-list')).not.toBeNull()
  })

  it('exposes the list to assistive tech even though it will be visually unstyled', () => {
    renderWithRouter(
      <RecentTransactions items={ITEMS} isEmpty={false} viewAllHref="/transactions" />,
    )

    expect(screen.getByRole('list')).toBeInTheDocument()
  })

  it('renders View all as a real react-router link to the given href, not a button or a plain anchor', () => {
    renderWithRouter(
      <RecentTransactions items={ITEMS} isEmpty={false} viewAllHref="/transactions" />,
    )

    const link = screen.getByRole('link', { name: 'View all' })
    expect(link).toHaveAttribute('href', '/transactions')
    expect(screen.queryByRole('button', { name: 'View all' })).toBeNull()
  })

  it('does not collapse rows, even when there are more items than the usual 5', () => {
    const many: RecentTransactionItem[] = Array.from({ length: 8 }, (_, i) => ({
      id: i,
      isIncome: i % 2 === 0,
      description: `Item ${i}`,
      categoryLabel: `Cat · Aug ${i + 1}`,
      amountLabel: '1 000 Ft',
    }))

    const { container } = renderWithRouter(
      <RecentTransactions items={many} isEmpty={false} viewAllHref="/transactions" />,
    )

    expect(container.querySelectorAll('.recent-transaction-item')).toHaveLength(8)
  })
})
