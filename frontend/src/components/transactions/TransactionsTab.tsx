import { useCallback, useEffect, useState, type FormEvent } from 'react'
import { createTransaction, deleteTransaction, fetchTransactions, updateTransaction } from '../../api/transactions'
import { fetchCategories } from '../../api/categories'
import { isAbortError, toFormError } from '../../api/ApiError'
import Pagination from '../Pagination'
import TransactionFilters, { type TransactionFiltersValue } from './TransactionFilters'
import TransactionRow, { type TransactionRowItem } from './TransactionRow'
import TransactionForm, { type TransactionFormValues } from './TransactionForm'
import { formatCurrencyHuf, formatTransactionListDate } from '../../lib/format'
import type {
  CategoryResponseDto,
  CreateTransactionDto,
  PageResponse,
  TransactionResponseDto,
} from '../../api/types'

const EMPTY_FILTERS: TransactionFiltersValue = { type: '', categoryId: '', from: '', to: '' }

function todayIsoDate(): string {
  return new Date().toISOString().slice(0, 10)
}

function defaultFormValues(): TransactionFormValues {
  return { type: '', categoryId: '', amount: '', date: todayIsoDate(), description: '' }
}

function hasActiveFilters(value: TransactionFiltersValue): boolean {
  return value.type !== '' || value.categoryId !== '' || value.from !== '' || value.to !== ''
}

function buildTransactionRowItems(page: PageResponse<TransactionResponseDto>): TransactionRowItem[] {
  return page.content.map((transaction) => {
    const isIncome = transaction.type === 'INCOME'
    const description =
      transaction.description !== null && transaction.description.length > 0
        ? transaction.description
        : transaction.categoryName
    const dateLabel = formatTransactionListDate(transaction.date)
    const sign = isIncome ? '+' : '−'
    return {
      id: transaction.id,
      isIncome,
      description,
      categoryLabel: `${transaction.categoryName} · ${dateLabel}`,
      amountLabel: `${sign}${formatCurrencyHuf(transaction.amount)}`,
      source: transaction,
    }
  })
}

function TransactionsTab() {
  const [filters, setFilters] = useState<TransactionFiltersValue>(EMPTY_FILTERS)
  const [page, setPage] = useState(0)
  const [transactionsPage, setTransactionsPage] = useState<PageResponse<TransactionResponseDto> | null>(
    null,
  )
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [categories, setCategories] = useState<CategoryResponseDto[]>([])

  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [formValues, setFormValues] = useState<TransactionFormValues>(defaultFormValues())
  const [formError, setFormError] = useState('')
  const [formFieldErrors, setFormFieldErrors] = useState<Record<string, string>>({})
  const [formSuccess, setFormSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const reload = useCallback(
    async (signal?: AbortSignal) => {
      setLoading(true)
      try {
        const data = await fetchTransactions(
          {
            page,
            size: 20,
            sort: 'date,desc',
            from: filters.from || undefined,
            to: filters.to || undefined,
            categoryId: filters.categoryId === '' ? undefined : filters.categoryId,
            type: filters.type === '' ? undefined : filters.type,
          },
          signal,
        )
        setTransactionsPage(data)
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

  const handleFiltersChange = (next: TransactionFiltersValue) => {
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

  const handleEditClick = (transaction: TransactionResponseDto) => {
    setEditingId(transaction.id)
    setFormValues({
      type: transaction.type,
      categoryId: transaction.categoryId,
      amount: String(transaction.amount),
      date: transaction.date,
      description: transaction.description ?? '',
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

    const { type, categoryId, amount, date, description } = formValues

    if (type === '' || categoryId === '' || amount.trim() === '' || date === '') {
      setFormError('Type, category, amount, and date are required')
      return
    }

    const parsedAmount = Number(amount)
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setFormError('Amount must be a positive number')
      return
    }

    const payload: CreateTransactionDto = {
      type,
      categoryId,
      amount: parsedAmount,
      date,
      description,
    }

    setSubmitting(true)
    try {
      if (editingId !== null) {
        await updateTransaction(editingId, payload)
        setShowForm(false)
        setEditingId(null)
        setFormSuccess('Transaction updated successfully')
        await reload()
      } else {
        await createTransaction(payload)
        setShowForm(false)
        setFormSuccess('Transaction created successfully')
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
    if (!window.confirm('Are you sure you want to delete this transaction?')) {
      return
    }

    setFormError('')
    setFormSuccess('')
    try {
      await deleteTransaction(id)
      setFormSuccess('Transaction deleted successfully')
      const isOnlyRowOnPage = transactionsPage?.content.length === 1
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
        <h1>Transactions</h1>
        <button type="button" className="btn-primary" onClick={handleOpenCreate}>
          + New Transaction
        </button>
      </div>

      <TransactionFilters
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
        <TransactionForm
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
          Loading transactions...
        </div>
      )}

      {!loading && error && (
        <div className="auth-error" role="alert">
          {error}
        </div>
      )}

      {!loading && !error && transactionsPage && (
        <>
          {transactionsPage.totalElements === 0 ? (
            <div className="empty-state">
              {activeFilters ? (
                <p>
                  No transactions match these filters.{' '}
                  <button type="button" className="link-button" onClick={handleClearFilters}>
                    Clear filters
                  </button>
                </p>
              ) : (
                <p>No transactions yet. Add your first transaction to get started.</p>
              )}
            </div>
          ) : (
            <ul className="transactions-list" role="list">
              {buildTransactionRowItems(transactionsPage).map((item) => (
                <TransactionRow
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
            page={transactionsPage.page}
            totalPages={transactionsPage.totalPages}
            totalElements={transactionsPage.totalElements}
            first={transactionsPage.first}
            last={transactionsPage.last}
            onPageChange={setPage}
          />
        </>
      )}
    </>
  )
}

export default TransactionsTab
