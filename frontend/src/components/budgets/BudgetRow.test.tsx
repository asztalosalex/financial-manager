import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import BudgetRow, { type BudgetRowItem } from './BudgetRow'
import type { BudgetResponseDto } from '../../api/types'

const SOURCE: BudgetResponseDto = {
  id: 1,
  amount: 150000,
  month: '2026-08-01',
  categoryId: 1,
  categoryName: 'Groceries',
}

const ITEM: BudgetRowItem = {
  id: 1,
  categoryName: 'Groceries',
  monthLabel: 'August 2026',
  amountLabel: '150 000 Ft',
  source: SOURCE,
}

function renderRow(item: BudgetRowItem, onEdit = vi.fn(), onDelete = vi.fn()) {
  return {
    onEdit,
    onDelete,
    ...render(
      <ul>
        <BudgetRow item={item} onEdit={onEdit} onDelete={onDelete} />
      </ul>,
    ),
  }
}

describe('BudgetRow', () => {
  it('renders the category name, month label and amount', () => {
    renderRow(ITEM)

    expect(screen.getByText('Groceries')).toBeInTheDocument()
    expect(screen.getByText('August 2026')).toBeInTheDocument()
    expect(screen.getByText('150 000 Ft')).toBeInTheDocument()
  })

  it('renders as a list item with the row top-border separator class', () => {
    const { container } = renderRow(ITEM)

    const row = container.querySelector('li.budget-row')
    expect(row).not.toBeNull()
  })

  it('renders the amount without a sign', () => {
    renderRow(ITEM)

    const amount = screen.getByText('150 000 Ft')
    expect(amount.textContent).not.toMatch(/^[+−-]/)
  })

  it('gives the amount a plain amount class, not an income or expense variant', () => {
    renderRow(ITEM)

    const amount = screen.getByText('150 000 Ft')
    expect(amount).toHaveClass('budget-row-amount')
    expect(amount.className).not.toMatch(/income|expense/)
  })

  it('renders no icon tile', () => {
    const { container } = renderRow(ITEM)

    expect(container.querySelector('[class*="row-icon"]')).toBeNull()
  })

  it('renders the category name as its own element, distinct from the month', () => {
    renderRow(ITEM)

    const category = screen.getByText('Groceries')
    const month = screen.getByText('August 2026')
    expect(category).not.toBe(month)
    expect(category).toHaveClass('budget-row-category')
    expect(month).toHaveClass('budget-row-month')
  })

  it('renders exactly one edit and one delete action button, labeled with category and month', () => {
    const { container } = renderRow(ITEM)

    const edit = screen.getByRole('button', { name: 'Edit Groceries, August 2026' })
    const del = screen.getByRole('button', { name: 'Delete Groceries, August 2026' })
    expect(edit).toHaveClass('btn-edit')
    expect(del).toHaveClass('btn-delete')
    expect(within(container).getAllByRole('button')).toHaveLength(2)
  })

  it('calls onEdit with the source budget when the edit button is clicked', () => {
    const { onEdit } = renderRow(ITEM)

    fireEvent.click(screen.getByRole('button', { name: 'Edit Groceries, August 2026' }))

    expect(onEdit).toHaveBeenCalledWith(SOURCE)
  })

  it('calls onDelete with the budget id when the delete button is clicked', () => {
    const { onDelete } = renderRow(ITEM)

    fireEvent.click(screen.getByRole('button', { name: 'Delete Groceries, August 2026' }))

    expect(onDelete).toHaveBeenCalledWith(1)
  })

  it('disambiguates same-category rows in different months via the aria-label', () => {
    const other: BudgetRowItem = {
      id: 2,
      categoryName: 'Groceries',
      monthLabel: 'September 2026',
      amountLabel: '160 000 Ft',
      source: { ...SOURCE, id: 2, month: '2026-09-01' },
    }
    render(
      <ul>
        <BudgetRow item={ITEM} onEdit={vi.fn()} onDelete={vi.fn()} />
        <BudgetRow item={other} onEdit={vi.fn()} onDelete={vi.fn()} />
      </ul>,
    )

    expect(screen.getByRole('button', { name: 'Edit Groceries, August 2026' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Edit Groceries, September 2026' })).toBeInTheDocument()
  })
})
