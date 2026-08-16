import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import TransactionFilters, { type TransactionFiltersValue } from './TransactionFilters'
import type { CategoryResponseDto } from '../../api/types'

const EMPTY_VALUE: TransactionFiltersValue = { type: '', categoryId: '', from: '', to: '' }

const CATEGORIES: CategoryResponseDto[] = [
  { id: 1, name: 'Groceries', description: 'Food' },
  { id: 2, name: 'Salary', description: 'Income' },
]

describe('TransactionFilters', () => {
  it('builds the category select options from the given categories, with All categories first', () => {
    render(
      <TransactionFilters value={EMPTY_VALUE} categories={CATEGORIES} onChange={vi.fn()} onClear={vi.fn()} />,
    )

    const select = screen.getByLabelText('Category') as HTMLSelectElement
    const optionLabels = Array.from(select.options).map((option) => option.textContent)
    expect(optionLabels).toEqual(['All categories', 'Groceries', 'Salary'])
  })

  it('renders only the All categories option when no categories are given', () => {
    render(<TransactionFilters value={EMPTY_VALUE} categories={[]} onChange={vi.fn()} onClear={vi.fn()} />)

    const select = screen.getByLabelText('Category') as HTMLSelectElement
    expect(Array.from(select.options).map((option) => option.textContent)).toEqual(['All categories'])
  })

  it('does not filter the category options by the selected type', () => {
    render(
      <TransactionFilters
        value={{ ...EMPTY_VALUE, type: 'INCOME' }}
        categories={CATEGORIES}
        onChange={vi.fn()}
        onClear={vi.fn()}
      />,
    )

    const select = screen.getByLabelText('Category') as HTMLSelectElement
    expect(Array.from(select.options).map((option) => option.textContent)).toEqual([
      'All categories',
      'Groceries',
      'Salary',
    ])
  })

  it('calls onChange immediately when the type select changes, with no apply button anywhere', () => {
    const onChange = vi.fn()
    render(
      <TransactionFilters value={EMPTY_VALUE} categories={CATEGORIES} onChange={onChange} onClear={vi.fn()} />,
    )

    fireEvent.change(screen.getByLabelText('Type'), { target: { value: 'INCOME' } })

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUE, type: 'INCOME' })
    expect(screen.queryByRole('button', { name: /apply/i })).toBeNull()
  })

  it('calls onChange with the numeric category id when a category is selected', () => {
    const onChange = vi.fn()
    render(
      <TransactionFilters value={EMPTY_VALUE} categories={CATEGORIES} onChange={onChange} onClear={vi.fn()} />,
    )

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '2' } })

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUE, categoryId: 2 })
  })

  it('calls onChange with an empty categoryId when All categories is re-selected', () => {
    const onChange = vi.fn()
    render(
      <TransactionFilters
        value={{ ...EMPTY_VALUE, categoryId: 2 }}
        categories={CATEGORIES}
        onChange={onChange}
        onClear={vi.fn()}
      />,
    )

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '' } })

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUE, categoryId: '' })
  })

  it('calls onChange with the raw date string when From or To changes', () => {
    const onChange = vi.fn()
    render(
      <TransactionFilters value={EMPTY_VALUE} categories={CATEGORIES} onChange={onChange} onClear={vi.fn()} />,
    )

    fireEvent.change(screen.getByLabelText('From'), { target: { value: '2026-01-01' } })
    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUE, from: '2026-01-01' })

    fireEvent.change(screen.getByLabelText('To'), { target: { value: '2026-06-30' } })
    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUE, to: '2026-06-30' })
  })

  it('hides Clear filters when no filter is active', () => {
    render(
      <TransactionFilters value={EMPTY_VALUE} categories={CATEGORIES} onChange={vi.fn()} onClear={vi.fn()} />,
    )

    expect(screen.queryByRole('button', { name: 'Clear filters' })).toBeNull()
  })

  it.each([
    ['type', { ...EMPTY_VALUE, type: 'EXPENSE' as const }],
    ['categoryId', { ...EMPTY_VALUE, categoryId: 1 }],
    ['from', { ...EMPTY_VALUE, from: '2026-01-01' }],
    ['to', { ...EMPTY_VALUE, to: '2026-01-31' }],
  ])('shows Clear filters when only %s is active', (_name, value) => {
    render(
      <TransactionFilters value={value} categories={CATEGORIES} onChange={vi.fn()} onClear={vi.fn()} />,
    )

    expect(screen.getByRole('button', { name: 'Clear filters' })).toBeInTheDocument()
  })

  it('calls onClear, not onChange, when Clear filters is clicked', () => {
    const onClear = vi.fn()
    const onChange = vi.fn()
    render(
      <TransactionFilters
        value={{ ...EMPTY_VALUE, type: 'EXPENSE' }}
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
