import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { createBudget, deleteBudget, fetchBudgets, updateBudget } from '../../api/budgets'
import { fetchCategories } from '../../api/categories'
import { isAbortError, toFormError } from '../../api/ApiError'
import Pagination from '../Pagination'
import BudgetFilters, { type BudgetFiltersValue } from './BudgetFilters'
import BudgetRow, { type BudgetRowItem } from './BudgetRow'
import BudgetForm, { type BudgetFormValues } from './BudgetForm'
import { formatBudgetMonthLabel, formatCurrencyHuf } from '../../lib/format'
import type {
  BudgetResponseDto,
  CategoryResponseDto,
  CreateBudgetDto,
  PageResponse,
} from '../../api/types'

const EMPTY_FILTERS: BudgetFiltersValue = { month: '', categoryId: '' }

function defaultFormValues(): BudgetFormValues {
  return { categoryId: '', month: '', amount: '' }
}

function hasActiveFilters(value: BudgetFiltersValue): boolean {
  return value.month !== '' || value.categoryId !== ''
}

function buildBudgetRowItems(page: PageResponse<BudgetResponseDto>): BudgetRowItem[] {
  return page.content.map((budget) => ({
    id: budget.id,
    categoryName: budget.categoryName,
    monthLabel: formatBudgetMonthLabel(budget.month),
    amountLabel: formatCurrencyHuf(budget.amount),
    source: budget,
  }))
}

function BudgetsTab() {
  const [filters, setFilters] = useState<BudgetFiltersValue>(EMPTY_FILTERS)
  const [page, setPage] = useState(0)
  const [budgetsPage, setBudgetsPage] = useState<PageResponse<BudgetResponseDto> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [categories, setCategories] = useState<CategoryResponseDto[]>([])

  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formValues, setFormValues] = useState<BudgetFormValues>(defaultFormValues())
  const [formError, setFormError] = useState('')
  const [formFieldErrors, setFormFieldErrors] = useState<Record<string, string>>({})
  const [formSuccess, setFormSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const reload = useCallback(
    async (signal?: AbortSignal) => {
      setLoading(true)
      try {
        const data = await fetchBudgets(
          {
            page,
            size: 20,
            sort: 'month,desc',
            month: filters.month || undefined,
            categoryId: filters.categoryId === '' ? undefined : filters.categoryId,
          },
          signal,
        )
        setBudgetsPage(data)
        setError('')
      } catch (err) {
        if (isAbortError(err)) {
          return
        }
        setError(toFormError(err).message)
      } finally {
        setLoading(false)
      }
    },
    [filters, page],
  )

  useEffect(() => {
    const controller = new AbortController()
    void reload(controller.signal)
    return () => controller.abort()
  }, [reload])

  useEffect(() => {
    const controller = new AbortController()

    async function loadCategories() {
      try {
        const data = await fetchCategories(controller.signal)
        setCategories(data)
      } catch (err) {
        if (isAbortError(err)) {
          return
        }
      }
    }

    void loadCategories()
    return () => controller.abort()
  }, [])

  const handleFiltersChange = (next: BudgetFiltersValue) => {
    setFilters(next)
    setPage(0)
  }

  const handleClearFilters = () => {
    setFilters(EMPTY_FILTERS)
    setPage(0)
  }

  const handleOpenCreate = () => {
    setEditingId(null)
    setFormValues(defaultFormValues())
    setFormError('')
    setFormFieldErrors({})
    setShowForm(true)
  }

  const handleEditClick = (budget: BudgetResponseDto) => {
    setEditingId(budget.id)
    setFormValues({
      categoryId: budget.categoryId,
      month: budget.month.slice(0, 7),
      amount: String(budget.amount),
    })
    setFormError('')
    setFormFieldErrors({})
    setShowForm(true)
  }

  const handleCancel = () => {
    setShowForm(false)
    setEditingId(null)
    setFormValues(defaultFormValues())
    setFormError('')
    setFormFieldErrors({})
  }

  const handleFormSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setFormError('')
    setFormFieldErrors({})
    setFormSuccess('')

    const { categoryId, month, amount } = formValues

    if (categoryId === '' || month === '' || amount.trim() === '') {
      setFormError('Category, month, and amount are required')
      return
    }

    const parsedAmount = Number(amount)
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setFormError('Amount must be a positive number')
      return
    }

    const payload: CreateBudgetDto = {
      categoryId,
      month: `${month}-01`,
      amount: parsedAmount,
    }

    setSubmitting(true)
    try {
      if (editingId !== null) {
        await updateBudget(editingId, payload)
        setShowForm(false)
        setEditingId(null)
        setFormSuccess('Budget updated successfully')
        await reload()
      } else {
        await createBudget(payload)
        setShowForm(false)
        setFormSuccess('Budget created successfully')
        if (page === 0) {
          await reload()
        } else {
          setPage(0)
        }
      }
    } catch (err) {
      const parsed = toFormError(err)
      setFormError(parsed.message)
      setFormFieldErrors(parsed.fieldErrors)
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: number) => {
    if (!window.confirm('Are you sure you want to delete this budget?')) {
      return
    }

    setFormError('')
    setFormSuccess('')
    try {
      await deleteBudget(id)
      setFormSuccess('Budget deleted successfully')
      const isOnlyRowOnPage = budgetsPage?.content.length === 1
      if (isOnlyRowOnPage && page > 0) {
        setPage(page - 1)
      } else {
        await reload()
      }
    } catch (err) {
      setFormError(toFormError(err).message)
    }
  }

  const activeFilters = hasActiveFilters(filters)

  return (
    <>
      <div className="shell-page-header">
        <h1>Budgets</h1>
        <button type="button" className="btn-primary" onClick={handleOpenCreate}>
          + New Budget
        </button>
      </div>

      <BudgetFilters
        value={filters}
        categories={categories}
        onChange={handleFiltersChange}
        onClear={handleClearFilters}
      />

      {formSuccess && (
        <div className="auth-success" role="status">
          {formSuccess}
        </div>
      )}

      {showForm && (
        <BudgetForm
          mode={editingId === null ? 'create' : 'edit'}
          values={formValues}
          categories={categories}
          formError={formError}
          fieldErrors={formFieldErrors}
          submitting={submitting}
          onChange={setFormValues}
          onSubmit={(e) => {
            void handleFormSubmit(e)
          }}
          onCancel={handleCancel}
        />
      )}

      {loading && (
        <div className="loading" role="status">
          Loading budgets...
        </div>
      )}

      {!loading && error && (
        <div className="auth-error" role="alert">
          {error}
        </div>
      )}

      {!loading && !error && budgetsPage && (
        <>
          {budgetsPage.totalElements === 0 ? (
            <div className="empty-state">
              {activeFilters ? (
                <p>
                  No budgets match these filters.{' '}
                  <button type="button" className="link-button" onClick={handleClearFilters}>
                    Clear filters
                  </button>
                </p>
              ) : (
                <p>No budgets yet. Add your first budget to get started.</p>
              )}
            </div>
          ) : (
            <ul className="budgets-list" role="list">
              {buildBudgetRowItems(budgetsPage).map((item) => (
                <BudgetRow
                  key={item.id}
                  item={item}
                  onEdit={handleEditClick}
                  onDelete={(id) => {
                    void handleDelete(id)
                  }}
                />
              ))}
            </ul>
          )}

          <Pagination
            page={budgetsPage.page}
            totalPages={budgetsPage.totalPages}
            totalElements={budgetsPage.totalElements}
            first={budgetsPage.first}
            last={budgetsPage.last}
            onPageChange={setPage}
          />
        </>
      )}
    </>
  )
}

export default BudgetsTab
