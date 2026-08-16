import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import TransactionRow, { type TransactionRowItem } from './TransactionRow'
import type { TransactionResponseDto } from '../../api/types'

const INCOME_SOURCE: TransactionResponseDto = {
  id: 1,
  type: 'INCOME',
  description: 'Salary',
  categoryId: 2,
  categoryName: 'Salary',
  amount: 500000,
  date: '2026-08-14',
}

const EXPENSE_SOURCE: TransactionResponseDto = {
  id: 2,
  type: 'EXPENSE',
  description: 'Groceries',
  categoryId: 1,
  categoryName: 'Food',
  amount: 12500,
  date: '2026-08-14',
}

const INCOME_ITEM: TransactionRowItem = {
  id: 1,
  isIncome: true,
  description: 'Salary',
  categoryLabel: 'Salary · Aug 14, 2026',
  amountLabel: '+500 000 Ft',
  source: INCOME_SOURCE,
}

const EXPENSE_ITEM: TransactionRowItem = {
  id: 2,
  isIncome: false,
  description: 'Groceries',
  categoryLabel: 'Food · Aug 14, 2026',
  amountLabel: '−12 500 Ft',
  source: EXPENSE_SOURCE,
}

function renderRow(item: TransactionRowItem, onEdit = vi.fn(), onDelete = vi.fn()) {
  const utils = render(
    <ul>
      <TransactionRow item={item} onEdit={onEdit} onDelete={onDelete} />
    </ul>,
  )
  return { ...utils, onEdit, onDelete }
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

  it('renders an edit and a delete action button with a category-label-qualified aria-label', () => {
    renderRow(EXPENSE_ITEM)

    expect(screen.getByRole('button', { name: 'Edit Food · Aug 14, 2026' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete Food · Aug 14, 2026' })).toBeInTheDocument()
  })

  it('calls onEdit with the row source transaction when the edit button is clicked', () => {
    const { onEdit } = renderRow(EXPENSE_ITEM)

    fireEvent.click(screen.getByRole('button', { name: 'Edit Food · Aug 14, 2026' }))

    expect(onEdit).toHaveBeenCalledWith(EXPENSE_SOURCE)
  })

  it('calls onDelete with the row id when the delete button is clicked', () => {
    const { onDelete } = renderRow(EXPENSE_ITEM)

    fireEvent.click(screen.getByRole('button', { name: 'Delete Food · Aug 14, 2026' }))

    expect(onDelete).toHaveBeenCalledWith(2)
  })
})
