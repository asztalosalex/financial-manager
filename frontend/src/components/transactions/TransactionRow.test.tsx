import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import TransactionRow, { type TransactionRowItem } from './TransactionRow'

const INCOME_ITEM: TransactionRowItem = {
  id: 1,
  isIncome: true,
  description: 'Salary',
  categoryLabel: 'Salary · Aug 14, 2026',
  amountLabel: '+500 000 Ft',
}

const EXPENSE_ITEM: TransactionRowItem = {
  id: 2,
  isIncome: false,
  description: 'Groceries',
  categoryLabel: 'Food · Aug 14, 2026',
  amountLabel: '−12 500 Ft',
}

function renderRow(item: TransactionRowItem) {
  return render(
    <ul>
      <TransactionRow item={item} />
    </ul>,
  )
}

describe('TransactionRow', () => {
  it('renders the description, category label and amount', () => {
    renderRow(INCOME_ITEM)

    expect(screen.getByText('Salary')).toBeInTheDocument()
    expect(screen.getByText('Salary · Aug 14, 2026')).toBeInTheDocument()
    expect(screen.getByText('+500 000 Ft')).toBeInTheDocument()
  })

  it('gives an income row the income amount class and an income icon tile', () => {
    const { container } = renderRow(INCOME_ITEM)

    expect(screen.getByText('+500 000 Ft')).toHaveClass('transaction-row-amount--income')
    expect(screen.getByText('+500 000 Ft')).not.toHaveClass('transaction-row-amount--expense')
    expect(container.querySelector('.transaction-row-icon--income')).not.toBeNull()
    expect(container.querySelector('.transaction-row-icon--expense')).toBeNull()
  })

  it('gives an expense row the expense amount class and an expense icon tile', () => {
    const { container } = renderRow(EXPENSE_ITEM)

    expect(screen.getByText('−12 500 Ft')).toHaveClass('transaction-row-amount--expense')
    expect(screen.getByText('−12 500 Ft')).not.toHaveClass('transaction-row-amount--income')
    expect(container.querySelector('.transaction-row-icon--expense')).not.toBeNull()
    expect(container.querySelector('.transaction-row-icon--income')).toBeNull()
  })

  it('renders as a list item with the row top-border separator class', () => {
    const { container } = renderRow(EXPENSE_ITEM)

    const row = container.querySelector('li.transaction-row')
    expect(row).not.toBeNull()
  })

  it('renders no edit or delete action buttons on the row', () => {
    renderRow(EXPENSE_ITEM)

    expect(screen.queryByRole('button')).toBeNull()
  })
})
