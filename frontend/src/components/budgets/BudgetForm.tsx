import type { ChangeEvent, FormEvent } from 'react'
import FieldError from '../FieldError'
import type { CategoryResponseDto } from '../../api/types'

export interface BudgetFormValues {
  categoryId: '' | number
  month: string
  amount: string
}

export interface BudgetFormProps {
  mode: 'create' | 'edit'
  values: BudgetFormValues
  categories: CategoryResponseDto[]
  formError: string
  fieldErrors: Record<string, string>
  submitting: boolean
  onChange: (values: BudgetFormValues) => void
  onSubmit: (e: FormEvent<HTMLFormElement>) => void
  onCancel: () => void
}

function BudgetForm({
  mode,
  values,
  categories,
  formError,
  fieldErrors,
  submitting,
  onChange,
  onSubmit,
  onCancel,
}: BudgetFormProps) {
  const handleCategoryChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const next = event.target.value
    onChange({ ...values, categoryId: next === '' ? '' : Number(next) })
  }

  const handleMonthChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...values, month: event.target.value })
  }

  const handleAmountChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...values, amount: event.target.value })
  }

  return (
    <div className="budget-form-section">
      <h3>{mode === 'edit' ? 'Edit Budget' : 'Create New Budget'}</h3>

      {formError && (
        <div className="auth-error" role="alert">
          {formError}
        </div>
      )}

      <form onSubmit={onSubmit} className="budget-form" noValidate>
        <div className="form-group">
          <label htmlFor="budget-form-category">Category</label>
          <select
            id="budget-form-category"
            value={values.categoryId}
            onChange={handleCategoryChange}
            aria-invalid={Boolean(fieldErrors.categoryId)}
            required
          >
            <option value="">Select category</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
          <FieldError message={fieldErrors.categoryId} />
        </div>

        <div className="form-group">
          <label htmlFor="budget-form-month">Month</label>
          <input
            type="month"
            id="budget-form-month"
            value={values.month}
            onChange={handleMonthChange}
            aria-invalid={Boolean(fieldErrors.month)}
            required
          />
          <FieldError message={fieldErrors.month} />
        </div>

        <div className="form-group">
          <label htmlFor="budget-form-amount">Amount</label>
          <input
            type="number"
            id="budget-form-amount"
            step="0.01"
            min="0.01"
            value={values.amount}
            onChange={handleAmountChange}
            aria-invalid={Boolean(fieldErrors.amount)}
            required
          />
          <FieldError message={fieldErrors.amount} />
        </div>

        <div className="form-actions">
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'Saving...' : mode === 'edit' ? 'Update Budget' : 'Create Budget'}
          </button>
          <button type="button" className="btn-secondary" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}

export default BudgetForm
