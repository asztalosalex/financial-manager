import type { ChangeEvent, FormEvent } from 'react'
import FieldError from '../FieldError'
import type { CategoryResponseDto } from '../../api/types'

export interface TransactionFormValues {
  type: '' | 'INCOME' | 'EXPENSE'
  categoryId: '' | number
  amount: string
  date: string
  description: string
}

export interface TransactionFormProps {
  mode: 'create' | 'edit'
  values: TransactionFormValues
  categories: CategoryResponseDto[]
  formError: string
  fieldErrors: Record<string, string>
  submitting: boolean
  onChange: (values: TransactionFormValues) => void
  onSubmit: (e: FormEvent<HTMLFormElement>) => void
  onCancel: () => void
}

function TransactionForm({
  mode,
  values,
  categories,
  formError,
  fieldErrors,
  submitting,
  onChange,
  onSubmit,
  onCancel,
}: TransactionFormProps) {
  const handleTypeChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const next = event.target.value
    onChange({ ...values, type: next === 'INCOME' || next === 'EXPENSE' ? next : '' })
  }

  const handleCategoryChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const next = event.target.value
    onChange({ ...values, categoryId: next === '' ? '' : Number(next) })
  }

  const handleAmountChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...values, amount: event.target.value })
  }

  const handleDateChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...values, date: event.target.value })
  }

  const handleDescriptionChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...values, description: event.target.value })
  }

  return (
    <div className="transaction-form-section">
      <h3>{mode === 'edit' ? 'Edit Transaction' : 'Create New Transaction'}</h3>

      {formError && (
        <div className="auth-error" role="alert">
          {formError}
        </div>
      )}

      <form onSubmit={onSubmit} className="transaction-form" noValidate>
        <div className="form-group">
          <label htmlFor="transaction-form-type">Type</label>
          <select
            id="transaction-form-type"
            value={values.type}
            onChange={handleTypeChange}
            aria-invalid={Boolean(fieldErrors.type)}
            required
          >
            <option value="">Select type</option>
            <option value="INCOME">Income</option>
            <option value="EXPENSE">Expense</option>
          </select>
          <FieldError message={fieldErrors.type} />
        </div>

        <div className="form-group">
          <label htmlFor="transaction-form-category">Category</label>
          <select
            id="transaction-form-category"
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
          <label htmlFor="transaction-form-amount">Amount</label>
          <input
            type="number"
            id="transaction-form-amount"
            step="0.01"
            min="0.01"
            value={values.amount}
            onChange={handleAmountChange}
            aria-invalid={Boolean(fieldErrors.amount)}
            required
          />
          <FieldError message={fieldErrors.amount} />
        </div>

        <div className="form-group">
          <label htmlFor="transaction-form-date">Date</label>
          <input
            type="date"
            id="transaction-form-date"
            value={values.date}
            onChange={handleDateChange}
            aria-invalid={Boolean(fieldErrors.date)}
            required
          />
          <FieldError message={fieldErrors.date} />
        </div>

        <div className="form-group">
          <label htmlFor="transaction-form-description">Description</label>
          <input
            type="text"
            id="transaction-form-description"
            placeholder="Optional note"
            value={values.description}
            onChange={handleDescriptionChange}
            aria-invalid={Boolean(fieldErrors.description)}
          />
          <FieldError message={fieldErrors.description} />
        </div>

        <div className="form-actions">
          <button type="submit" className="btn-primary" disabled={submitting}>
            {submitting ? 'Saving...' : mode === 'edit' ? 'Update Transaction' : 'Create Transaction'}
          </button>
          <button type="button" className="btn-secondary" onClick={onCancel} disabled={submitting}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}

export default TransactionForm
