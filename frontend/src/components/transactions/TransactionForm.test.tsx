import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import type { FormEvent } from 'react'
import TransactionForm, { type TransactionFormValues } from './TransactionForm'
import type { CategoryResponseDto } from '../../api/types'

const CATEGORIES: CategoryResponseDto[] = [
  { id: 1, name: 'Groceries', description: 'Food' },
  { id: 2, name: 'Salary', description: 'Income' },
]

const EMPTY_VALUES: TransactionFormValues = {
  type: '',
  categoryId: '',
  amount: '',
  date: '2026-08-16',
  description: '',
}

function renderForm(overrides: Partial<Parameters<typeof TransactionForm>[0]> = {}) {
  const onChange = vi.fn()
  const onSubmit = vi.fn((e: FormEvent<HTMLFormElement>) => e.preventDefault())
  const onCancel = vi.fn()
  const props = {
    mode: 'create' as const,
    values: EMPTY_VALUES,
    categories: CATEGORIES,
    formError: '',
    fieldErrors: {},
    submitting: false,
    onChange,
    onSubmit,
    onCancel,
    ...overrides,
  }
  const utils = render(<TransactionForm {...props} />)
  return { ...utils, onChange, onSubmit, onCancel }
}

describe('TransactionForm', () => {
  it('renders Select type/Select category placeholders', () => {
    renderForm({ values: { ...EMPTY_VALUES, type: 'EXPENSE' } })

    const typeSelect = screen.getByLabelText('Type') as HTMLSelectElement
    expect(Array.from(typeSelect.options).map((o) => o.textContent)).toEqual([
      'Select type',
      'Income',
      'Expense',
    ])

    const categorySelect = screen.getByLabelText('Category') as HTMLSelectElement
    expect(Array.from(categorySelect.options).map((o) => o.textContent)).toEqual([
      'Select category',
      'Groceries',
      'Salary',
    ])
  })

  it.each([['' as const], ['INCOME' as const], ['EXPENSE' as const]])(
    'lists every supplied category regardless of the selected type (%s)',
    (type) => {
      renderForm({ values: { ...EMPTY_VALUES, type } })

      const categorySelect = screen.getByLabelText('Category') as HTMLSelectElement
      expect(Array.from(categorySelect.options).map((o) => o.textContent)).toEqual([
        'Select category',
        'Groceries',
        'Salary',
      ])
    },
  )

  it('shows Create Transaction as the submit label and heading in create mode', () => {
    renderForm({ mode: 'create' })

    expect(screen.getByRole('heading', { name: 'Create New Transaction' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Create Transaction' })).toBeInTheDocument()
  })

  it('shows Update Transaction as the submit label and heading in edit mode', () => {
    renderForm({ mode: 'edit' })

    expect(screen.getByRole('heading', { name: 'Edit Transaction' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Update Transaction' })).toBeInTheDocument()
  })

  it('reflects the supplied values in each control', () => {
    renderForm({
      values: {
        type: 'INCOME',
        categoryId: 2,
        amount: '500000',
        date: '2026-08-01',
        description: 'August salary',
      },
    })

    expect(screen.getByLabelText('Type')).toHaveValue('INCOME')
    expect(screen.getByLabelText('Category')).toHaveValue('2')
    expect(screen.getByLabelText('Amount')).toHaveValue(500000)
    expect(screen.getByLabelText('Date')).toHaveValue('2026-08-01')
    expect(screen.getByLabelText('Description')).toHaveValue('August salary')
  })

  it('calls onChange with the updated values object when a field changes', () => {
    const { onChange } = renderForm()

    fireEvent.change(screen.getByLabelText('Type'), { target: { value: 'EXPENSE' } })

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUES, type: 'EXPENSE' })
  })

  it('calls onChange with a numeric categoryId when the category select changes', () => {
    const { onChange } = renderForm()

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '2' } })

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUES, categoryId: 2 })
  })

  it('calls onSubmit when the form is submitted', () => {
    const { onSubmit } = renderForm()

    fireEvent.submit(screen.getByRole('button', { name: 'Create Transaction' }).closest('form') as HTMLFormElement)

    expect(onSubmit).toHaveBeenCalled()
  })

  it('calls onCancel without calling onSubmit when Cancel is clicked', () => {
    const { onCancel, onSubmit } = renderForm()

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(onCancel).toHaveBeenCalled()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('disables both buttons and shows Saving... while submitting', () => {
    renderForm({ submitting: true })

    expect(screen.getByRole('button', { name: 'Saving...' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled()
  })

  it('renders the combined formError banner', () => {
    renderForm({ formError: 'Amount must be a positive number' })

    expect(screen.getByText('Amount must be a positive number')).toHaveClass('auth-error')
  })

  it('renders field-level errors beside their controls and marks them aria-invalid', () => {
    renderForm({ fieldErrors: { amount: 'Amount must be greater than zero' } })

    expect(screen.getByText('Amount must be greater than zero')).toHaveClass('field-error')
    expect(screen.getByLabelText('Amount')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Date')).toHaveAttribute('aria-invalid', 'false')
  })
})
