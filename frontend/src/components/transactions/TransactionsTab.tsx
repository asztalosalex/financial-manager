import { useEffect, useState } from 'react'
import { fetchTransactions } from '../../api/transactions'
import { fetchCategories } from '../../api/categories'
import { isAbortError, toFormError } from '../../api/ApiError'
import Pagination from '../Pagination'
import TransactionFilters, { type TransactionFiltersValue } from './TransactionFilters'
import TransactionRow, { type TransactionRowItem } from './TransactionRow'
import { formatCurrencyHuf, formatTransactionListDate } from '../../lib/format'
import type { CategoryResponseDto, PageResponse, TransactionResponseDto } from '../../api/types'

const EMPTY_FILTERS: TransactionFiltersValue = { type: '', categoryId: '', from: '', to: '' }

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

  useEffect(() => {
    const controller = new AbortController()

    async function load() {
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
          controller.signal,
        )
        setTransactionsPage(data)
        setError('')
        setLoading(false)
      } catch (err) {
        if (isAbortError(err)) {
          return
        }
        setError(toFormError(err).message)
        setLoading(false)
      }
    }

    void load()
    return () => controller.abort()
  }, [filters, page])

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

  const activeFilters = hasActiveFilters(filters)

  return (
    <>
      <TransactionFilters
        value={filters}
        categories={categories}
        onChange={handleFiltersChange}
        onClear={handleClearFilters}
      />

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
                <TransactionRow key={item.id} item={item} />
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
