import type { ChangeEvent } from 'react'
import type { CategoryResponseDto } from '../../api/types'

export interface BudgetFiltersValue {
  month: string
  categoryId: '' | number
}

export interface BudgetFiltersProps {
  value: BudgetFiltersValue
  categories: CategoryResponseDto[]
  onChange: (value: BudgetFiltersValue) => void
  onClear: () => void
}

function hasActiveFilters(value: BudgetFiltersValue): boolean {
  return value.month !== '' || value.categoryId !== ''
}

function BudgetFilters({ value, categories, onChange, onClear }: BudgetFiltersProps) {
  const handleMonthChange = (event: ChangeEvent<HTMLInputElement>) => {
    onChange({ ...value, month: event.target.value })
  }

  const handleCategoryChange = (event: ChangeEvent<HTMLSelectElement>) => {
    const next = event.target.value
    onChange({ ...value, categoryId: next === '' ? '' : Number(next) })
  }

  return (
    <div className="budget-filters">
      <div className="budget-filters-field">
        <label htmlFor="budget-filter-month">Month</label>
        <input id="budget-filter-month" type="month" value={value.month} onChange={handleMonthChange} />
      </div>

      <div className="budget-filters-field">
        <label htmlFor="budget-filter-category">Category</label>
        <select id="budget-filter-category" value={value.categoryId} onChange={handleCategoryChange}>
          <option value="">All categories</option>
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
      </div>

      {hasActiveFilters(value) && (
        <button type="button" className="link-button budget-filters-clear" onClick={onClear}>
          Clear filters
        </button>
      )}
    </div>
  )
}

export default BudgetFilters
