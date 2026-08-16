import type { ChangeEvent } from 'react'
import type { CategoryResponseDto } from '../../api/types'

export interface TransactionFiltersValue {
  type: '' | 'INCOME' | 'EXPENSE'
  categoryId: '' | number
  from: string
  to: string
}

export interface TransactionFiltersProps {
  value: TransactionFiltersValue
  categories: CategoryResponseDto[]
  onChange: (value: TransactionFiltersValue) => void
  onClear: () => void
}

function hasActiveFilters(value: TransactionFiltersValue): boolean {
  return value.type !== '' || value.categoryId !== '' || value.from !== '' || value.to !== ''
}

function TransactionFilters({ value, categories, onChange, onClear }: TransactionFiltersProps) {
  const handleTypeChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const next = event.target.value
    onChange({ ...value, type: next === 'INCOME' || next === 'EXPENSE' ? next : '' })
  }

  const handleCategoryChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const next = event.target.value
    onChange({ ...value, categoryId: next === '' ? '' : Number(next) })
  }

  const handleFromChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...value, from: event.target.value })
  }

  const handleToChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...value, to: event.target.value })
  }

  return (
    <div className="transaction-filters">
      <div className="transaction-filters-field">
        <label htmlFor="transaction-filter-type">Type</label>
        <select id="transaction-filter-type" value={value.type} onChange={handleTypeChange}>
          <option value="">All types</option>
          <option value="INCOME">Income</option>
          <option value="EXPENSE">Expense</option>
        </select>
      </div>

      <div className="transaction-filters-field">
        <label htmlFor="transaction-filter-category">Category</label>
        <select id="transaction-filter-category" value={value.categoryId} onChange={handleCategoryChange}>
          <option value="">All categories</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
      </div>

      <div className="transaction-filters-field">
        <label htmlFor="transaction-filter-from">From</label>
        <input id="transaction-filter-from" type="date" value={value.from} onChange={handleFromChange} />
      </div>

      <div className="transaction-filters-field">
        <label htmlFor="transaction-filter-to">To</label>
        <input id="transaction-filter-to" type="date" value={value.to} onChange={handleToChange} />
      </div>

      {hasActiveFilters(value) && (
        <button type="button" className="link-button transaction-filters-clear" onClick={onClear}>
          Clear filters
        </button>
      )}
    </div>
  )
}

export default TransactionFilters
