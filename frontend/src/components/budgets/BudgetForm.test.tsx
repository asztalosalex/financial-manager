import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import type { FormEvent } from 'react'
import BudgetForm, { type BudgetFormValues } from './BudgetForm'
import type { CategoryResponseDto } from '../../api/types'

const CATEGORIES: CategoryResponseDto[] = [
  { id: 1, name: 'Groceries', description: 'Food' },
  { id: 2, name: 'Housing', description: 'Rent and utilities' },
]

const EMPTY_VALUES: BudgetFormValues = {
  categoryId: '',
  month: '',
  amount: '',
}

function renderForm(overrides: Partial<Parameters<typeof BudgetForm>[0]> = {}) {
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
  const utils = render(<BudgetForm {...props} />)
  return { ...utils, onChange, onSubmit, onCancel }
}

describe('BudgetForm', () => {
  it('renders the Select category placeholder followed by every supplied category', () => {
    renderForm()

    const categorySelect = screen.getByLabelText('Category') as HTMLSelectElement
    expect(Array.from(categorySelect.options).map((o) => o.textContent)).toEqual([
      'Select category',
      'Groceries',
      'Housing',
    ])
  })

  it('has no Type field and no Description field', () => {
    renderForm()

    expect(screen.queryByLabelText('Type')).toBeNull()
    expect(screen.queryByLabelText('Description')).toBeNull()
  })

  it('renders Month as a month input and Amount as a number input with min 0.01', () => {
    renderForm()

    expect(screen.getByLabelText('Month')).toHaveAttribute('type', 'month')
    const amount = screen.getByLabelText('Amount')
    expect(amount).toHaveAttribute('type', 'number')
    expect(amount).toHaveAttribute('min', '0.01')
    expect(amount).toHaveAttribute('step', '0.01')
  })

  it('shows Create Budget as the submit label and heading in create mode', () => {
    renderForm({ mode: 'create' })

    expect(screen.getByRole('heading', { name: 'Create New Budget' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Create Budget' })).toBeInTheDocument()
  })

  it('shows Update Budget as the submit label and heading in edit mode', () => {
    renderForm({ mode: 'edit' })

    expect(screen.getByRole('heading', { name: 'Edit Budget' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Update Budget' })).toBeInTheDocument()
  })

  it('reflects the supplied values in each control', () => {
    renderForm({
      values: { categoryId: 2, month: '2026-08', amount: '300000' },
    })

    expect(screen.getByLabelText('Category')).toHaveValue('2')
    expect(screen.getByLabelText('Month')).toHaveValue('2026-08')
    expect(screen.getByLabelText('Amount')).toHaveValue(300000)
  })

  it('calls onChange with a numeric categoryId when the category select changes', () => {
    const { onChange } = renderForm()

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '2' } })

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUES, categoryId: 2 })
  })

  it('calls onChange with the raw YYYY-MM DOM value when the month changes, no suffix appended', () => {
    const { onChange } = renderForm()

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-08' } })

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUES, month: '2026-08' })
  })

  it('calls onChange with the raw amount string when the amount changes', () => {
    const { onChange } = renderForm()

    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '150000' } })

    expect(onChange).toHaveBeenCalledWith({ ...EMPTY_VALUES, amount: '150000' })
  })

  it('calls onSubmit when the form is submitted', () => {
    const { onSubmit } = renderForm()

    fireEvent.submit(screen.getByRole('button', { name: 'Create Budget' }).closest('form') as HTMLFormElement)

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
    expect(screen.getByLabelText('Month')).toHaveAttribute('aria-invalid', 'false')
  })
})
