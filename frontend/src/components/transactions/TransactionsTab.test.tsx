import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import TransactionsTab from './TransactionsTab'
import { clearCookies, errorResponse, jsonResponse } from '../../test/helpers'
import type { CategoryResponseDto, PageResponse, TransactionResponseDto } from '../../api/types'

type FetchHandler = (url: string, init: RequestInit | undefined) => Response

function mockFetch(handler: FetchHandler) {
  vi.mocked(globalThis.fetch).mockImplementation((input, init) =>
    Promise.resolve(handler(String(input), init)),
  )
}

function paramsOf(url: string): URLSearchParams {
  return new URL(url, 'http://localhost').searchParams
}

function transactionCalls() {
  return vi
    .mocked(globalThis.fetch)
    .mock.calls.filter(([url]) => String(url).includes('/api/transactions'))
}

function lastTransactionParams(): URLSearchParams {
  const calls = transactionCalls()
  return paramsOf(String(calls[calls.length - 1][0]))
}

interface DeferredResponse {
  promise: Promise<Response>
  resolve: (response: Response) => void
}

function deferredSignalAwareResponse(signal: AbortSignal | null | undefined): DeferredResponse {
  let resolveFn: (response: Response) => void = () => {}
  const promise = new Promise<Response>((resolve, reject) => {
    resolveFn = resolve
    if (signal) {
      if (signal.aborted) {
        reject(new DOMException('Aborted', 'AbortError'))
        return
      }
      signal.addEventListener('abort', () => {
        reject(new DOMException('Aborted', 'AbortError'))
      })
    }
  })
  return { promise, resolve: resolveFn }
}

function huf(value: number): string {
  return `${value.toLocaleString('hu-HU')} Ft`.replace(/\u00A0/g, ' ')
}

const CATEGORIES: CategoryResponseDto[] = [
  { id: 1, name: 'Groceries', description: 'Food' },
  { id: 2, name: 'Salary', description: 'Income' },
]

function emptyPage(overrides: Partial<PageResponse<TransactionResponseDto>> = {}): PageResponse<TransactionResponseDto> {
  return {
    content: [],
    page: 0,
    size: 20,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
    ...overrides,
  }
}

const SAMPLE_TRANSACTIONS: TransactionResponseDto[] = [
  {
    id: 101,
    type: 'INCOME',
    description: 'August salary',
    categoryId: 2,
    categoryName: 'Salary',
    amount: 500000,
    date: '2026-08-01',
  },
  {
    id: 102,
    type: 'EXPENSE',
    description: null,
    categoryId: 1,
    categoryName: 'Groceries',
    amount: 12500,
    date: '2026-08-14',
  },
]

interface Routes {
  transactions?: () => Response
  categories?: () => Response
}

function mockRoutes(routes: Routes) {
  mockFetch((url) => {
    if (url.includes('/api/categories/user')) {
      return (routes.categories ?? (() => jsonResponse(200, CATEGORIES)))()
    }
    return (routes.transactions ?? (() => jsonResponse(200, emptyPage())))()
  })
}

async function renderLoaded(transactionsPage: PageResponse<TransactionResponseDto>) {
  mockRoutes({ transactions: () => jsonResponse(200, transactionsPage) })
  render(<TransactionsTab />)
  await waitFor(() => {
    expect(screen.queryByRole('status')).toBeNull()
  })
}

describe('TransactionsTab', () => {
  beforeEach(() => {
    clearCookies()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    clearCookies()
  })

  it('shows a loading indicator until transactions arrive, with the filter bar visible and no list or pagination', () => {
    vi.mocked(globalThis.fetch).mockReturnValue(new Promise<Response>(() => {}))

    render(<TransactionsTab />)

    expect(screen.getByRole('status')).toHaveTextContent('Loading transactions...')
    expect(screen.getByLabelText('Type')).toBeInTheDocument()
    expect(screen.queryByRole('list')).toBeNull()
    expect(screen.queryByText(/Page \d+ of/)).toBeNull()
  })

  it('fetches with the fixed page size and sort, and no filter parameters, on first mount', async () => {
    await renderLoaded(emptyPage())

    const params = lastTransactionParams()
    expect(params.get('page')).toBe('0')
    expect(params.get('size')).toBe('20')
    expect(params.get('sort')).toBe('date,desc')
    expect(params.has('from')).toBe(false)
    expect(params.has('to')).toBe(false)
    expect(params.has('categoryId')).toBe(false)
    expect(params.has('type')).toBe(false)
  })

  it('aborts the in-flight transactions request on unmount', () => {
    let capturedSignal: AbortSignal | undefined
    mockFetch((url, init) => {
      if (url.includes('/api/transactions')) {
        capturedSignal = init?.signal ?? undefined
      }
      return jsonResponse(200, emptyPage())
    })

    const { unmount } = render(<TransactionsTab />)
    unmount()

    expect(capturedSignal?.aborted).toBe(true)
  })

  it('renders a row falling back to the category name when description is null', async () => {
    await renderLoaded(emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 2, totalPages: 1 }))

    const list = screen.getByRole('list')
    expect(within(list).getByText('August salary')).toBeInTheDocument()
    expect(within(list).getByText('Groceries')).toBeInTheDocument()
  })

  it('shows a plus sign and success color for income, a minus sign and text color for expense', async () => {
    await renderLoaded(emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 2, totalPages: 1 }))

    const incomeAmount = screen.getByText(`+${huf(500000)}`)
    expect(incomeAmount).toHaveClass('transaction-row-amount--income')

    const expenseAmount = screen.getByText(`−${huf(12500)}`)
    expect(expenseAmount).toHaveClass('transaction-row-amount--expense')
  })

  it('shows the no-transactions empty state when there are no transactions and no active filters', async () => {
    await renderLoaded(emptyPage())

    expect(
      screen.getByText('No transactions yet. Add your first transaction to get started.'),
    ).toBeInTheDocument()
    expect(screen.queryByText('No transactions match these filters.')).toBeNull()
  })

  it('shows the filtered-empty state with an inline Clear filters link once a filter is applied and the result is empty', async () => {
    await renderLoaded(emptyPage())

    fireEvent.change(screen.getByLabelText('Type'), { target: { value: 'INCOME' } })

    await waitFor(() => {
      expect(screen.getByText('No transactions match these filters.')).toBeInTheDocument()
    })
    expect(
      screen.queryByText('No transactions yet. Add your first transaction to get started.'),
    ).toBeNull()
  })

  it('surfaces the server error message and hides the list and pagination', async () => {
    mockRoutes({ transactions: () => errorResponse(400, 'from must not be after to') })
    render(<TransactionsTab />)

    await waitFor(() => {
      expect(screen.getByText('from must not be after to')).toHaveClass('auth-error')
    })
    expect(screen.queryByRole('list')).toBeNull()
    expect(screen.queryByText(/Page \d+ of/)).toBeNull()
  })

  it('builds the category select options from fetchCategories results', async () => {
    await renderLoaded(emptyPage())

    const select = await screen.findByLabelText('Category')
    const options = Array.from((select as HTMLSelectElement).options).map((option) => option.textContent)
    expect(options).toEqual(['All categories', 'Groceries', 'Salary'])
  })

  it('leaves the category select with only All categories when fetchCategories fails, without affecting the transaction list', async () => {
    mockRoutes({
      transactions: () => jsonResponse(200, emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 2, totalPages: 1 })),
      categories: () => errorResponse(500, 'Categories are temporarily unavailable'),
    })
    render(<TransactionsTab />)

    await screen.findByText('August salary')

    const select = screen.getByLabelText('Category') as HTMLSelectElement
    expect(Array.from(select.options).map((option) => option.textContent)).toEqual(['All categories'])
    expect(screen.queryByText('Categories are temporarily unavailable')).toBeNull()
  })

  it('resets to page 0 and re-fetches when a filter changes after paging forward', async () => {
    await renderLoaded(
      emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 40, totalPages: 2, first: true, last: false }),
    )

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))
    await waitFor(() => {
      expect(lastTransactionParams().get('page')).toBe('1')
    })

    fireEvent.change(screen.getByLabelText('Type'), { target: { value: 'EXPENSE' } })

    await waitFor(() => {
      expect(lastTransactionParams().get('page')).toBe('0')
      expect(lastTransactionParams().get('type')).toBe('EXPENSE')
    })
  })

  it('changes only the page, not the filters, when Next is clicked', async () => {
    await renderLoaded(
      emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 40, totalPages: 2, first: true, last: false }),
    )

    fireEvent.change(screen.getByLabelText('Type'), { target: { value: 'INCOME' } })
    await waitFor(() => {
      expect(lastTransactionParams().get('type')).toBe('INCOME')
    })

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))

    await waitFor(() => {
      expect(lastTransactionParams().get('page')).toBe('1')
      expect(lastTransactionParams().get('type')).toBe('INCOME')
    })
  })

  it('resets filters and page to defaults when Clear filters is clicked', async () => {
    await renderLoaded(
      emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 40, totalPages: 2, first: true, last: false }),
    )

    fireEvent.change(screen.getByLabelText('Type'), { target: { value: 'INCOME' } })
    await waitFor(() => {
      expect(lastTransactionParams().get('type')).toBe('INCOME')
    })

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))
    await waitFor(() => {
      expect(lastTransactionParams().get('page')).toBe('1')
    })

    fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }))

    await waitFor(() => {
      expect(lastTransactionParams().get('page')).toBe('0')
      expect(lastTransactionParams().has('type')).toBe(false)
    })
  })

  it('renders the pagination bar with the 1-indexed page label once transactions load', async () => {
    await renderLoaded(
      emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 137, totalPages: 7, first: true, last: false }),
    )

    expect(screen.getByText('Page 1 of 7 (137 total)')).toBeInTheDocument()
  })

  it('renders no New Transaction button and no per-row action icons', async () => {
    await renderLoaded(emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 2, totalPages: 1 }))

    expect(screen.queryByRole('button', { name: /New Transaction/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /Edit/i })).toBeNull()
    expect(screen.queryByRole('button', { name: /Delete/i })).toBeNull()
  })

  it('sends the selected category id as a number, not a concatenated string or NaN', async () => {
    await renderLoaded(emptyPage())

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '2' } })

    await waitFor(() => {
      expect(lastTransactionParams().get('categoryId')).toBe('2')
    })
    expect(lastTransactionParams().get('categoryId')).not.toBe('22')
    expect(lastTransactionParams().get('categoryId')).not.toBe('NaN')
  })

  it('discards a stale in-flight response when a newer request resolves out of order', async () => {
    const deferred: DeferredResponse[] = []

    vi.mocked(globalThis.fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/api/categories/user')) {
        return Promise.resolve(jsonResponse(200, CATEGORIES))
      }
      const entry = deferredSignalAwareResponse(init?.signal)
      deferred.push(entry)
      return entry.promise
    })

    render(<TransactionsTab />)

    await waitFor(() => {
      expect(deferred).toHaveLength(1)
    })

    fireEvent.change(screen.getByLabelText('Type'), { target: { value: 'INCOME' } })

    await waitFor(() => {
      expect(deferred).toHaveLength(2)
    })

    deferred[1].resolve(
      jsonResponse(
        200,
        emptyPage({ content: [SAMPLE_TRANSACTIONS[0]], totalElements: 1, totalPages: 1 }),
      ),
    )
    await screen.findByText('August salary')

    deferred[0].resolve(
      jsonResponse(
        200,
        emptyPage({ content: [SAMPLE_TRANSACTIONS[1]], totalElements: 1, totalPages: 1 }),
      ),
    )

    const list = screen.getByRole('list')
    await waitFor(() => {
      expect(within(list).getByText('August salary')).toBeInTheDocument()
    })
    expect(within(list).queryByText('Groceries')).toBeNull()
  })
})
