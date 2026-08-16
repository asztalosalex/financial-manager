import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import BudgetFilters, { type BudgetFiltersValue } from './BudgetFilters'
import type { CategoryResponseDto } from '../../api/types'

const EMPTY_VALUE: BudgetFiltersValue = { month: '', categoryId: '' }

const CATEGORIES: CategoryResponseDto[] = [
  { id: 1, name: 'Groceries', description: 'Food' },
  { id: 2, name: 'Housing', description: 'Rent and utilities' },
]

describe('BudgetFilters', () => {
  it('renders exactly a Month and a Category control, with no Type filter', () => {
    render(<BudgetFilters value={EMPTY_VALUE} categories={CATEGORIES} onChange={vi.fn()} onClear={vi.fn()} />)

    expect(screen.getByLabelText('Month')).toBeInTheDocument()
    expect(screen.getByLabelText('Category')).toBeInTheDocument()
    expect(screen.queryByLabelText('Type')).toBeNull()
  })

  it('renders the month field as a native month input', () => {
    render(<BudgetFilters value={EMPTY_VALUE} categories={CATEGORIES} onChange={vi.fn()} onClear={vi.fn()} />)

    expect(screen.getByLabelText('Month')).toHaveAttribute('type', 'month')
  })

  it('builds the category select options from the given categories, with All categories first', () => {
    render(<BudgetFilters value={EMPTY_VALUE} categories={CATEGORIES} onChange={vi.fn()} onClear={vi.fn()} />)

    const select = screen.getByLabelText('Category') as HTMLSelectElement
    const optionLabels = Array.from(select.options).map((option) => option.textContent)
    expect(optionLabels).toEqual(['All categories', 'Groceries', 'Housing'])
  })

  it('renders only the All categories option when no categories are given', () => {
    render(<BudgetFilters value={EMPTY_VALUE} categories={[]} onChange={vi.fn()} onClear={vi.fn()} />)

    const select = screen.getByLabelText('Category') as HTMLSelectElement
    expect(Array.from(select.options).map((option) => option.textContent)).toEqual(['All categories'])
  })

  it('calls onChange with the raw YYYY-MM value when the month input changes, with no apply button anywhere', () => {
    const onChange = vi.fn()
    render(<BudgetFilters value={EMPTY_VALUE} categories={CATEGORIES} onChange={onChange} onClear={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-08' } })

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUE, month: '2026-08' })
    expect(screen.queryByRole('button', { name: /apply/i })).toBeNull()
  })

  it('calls onChange with the numeric category id when a category is selected', () => {
    const onChange = vi.fn()
    render(<BudgetFilters value={EMPTY_VALUE} categories={CATEGORIES} onChange={onChange} onClear={vi.fn()} />)

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '2' } })

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUE, categoryId: 2 })
  })

  it('calls onChange with an empty categoryId when All categories is re-selected', () => {
    const onChange = vi.fn()
    render(
      <BudgetFilters
        value={{ ...EMPTY_VALUE, categoryId: 2 }}
        categories={CATEGORIES}
        onChange={onChange}
        onClear={vi.fn()}
      />,
    )

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '' } })

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUE, categoryId: '' })
  })

  it('hides Clear filters when no filter is active', () => {
    render(<BudgetFilters value={EMPTY_VALUE} categories={CATEGORIES} onChange={vi.fn()} onClear={vi.fn()} />)

    expect(screen.queryByRole('button', { name: 'Clear filters' })).toBeNull()
  })

  it.each([
    ['month', { ...EMPTY_VALUE, month: '2026-08' }],
    ['categoryId', { ...EMPTY_VALUE, categoryId: 1 }],
  ])('shows Clear filters when only %s is active', (_name, value) => {
    render(<BudgetFilters value={value} categories={CATEGORIES} onChange={vi.fn()} onClear={vi.fn()} />)

    expect(screen.getByRole('button', { name: 'Clear filters' })).toBeInTheDocument()
  })

  it('calls onClear, not onChange, when Clear filters is clicked', () => {
    const onClear = vi.fn()
    const onChange = vi.fn()
    render(
      <BudgetFilters
        value={{ ...EMPTY_VALUE, month: '2026-08' }}
        categories={CATEGORIES}
        onChange={onChange}
        onClear={onClear}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }))

    expect(onClear).toHaveBeenCalledOnce()
    expect(onChange).not.toHaveBeenCalled()
  })
})
