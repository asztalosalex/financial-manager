import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useNavigate, useSearchParams } from 'react-router-dom'
import TransactionsTab from './TransactionsTab'
import { clearCookies, emptyResponse, errorResponse, jsonResponse } from '../../test/helpers'
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
  transactions?: (init: RequestInit | undefined) => Response
  categories?: (init: RequestInit | undefined) => Response
}

function mockRoutes(routes: Routes) {
  mockFetch((url, init) => {
    if (url.includes('/api/categories/user')) {
      return (routes.categories ?? (() => jsonResponse(200, CATEGORIES)))(init)
    }
    return (routes.transactions ?? (() => jsonResponse(200, emptyPage())))(init)
  })
}

function formWithin() {
  return within(document.querySelector('.transaction-form-section') as HTMLElement)
}

function submitForm(label: string) {
  return act(async () => {
    fireEvent.click(screen.getByRole('button', { name: label }))
  })
}

function SearchParamsProbe() {
  const [params] = useSearchParams()
  return <div data-testid="search-probe">{params.toString()}</div>
}

function renderTab(initialEntries: string[] = ['/transactions']) {
  return render(
    <MemoryRouter initialEntries={initialEntries}>
      <TransactionsTab />
      <SearchParamsProbe />
    </MemoryRouter>,
  )
}

async function renderLoaded(
  transactionsPage: PageResponse<TransactionResponseDto>,
  initialEntries: string[] = ['/transactions'],
) {
  mockRoutes({ transactions: () => jsonResponse(200, transactionsPage) })
  renderTab(initialEntries)
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

    renderTab()

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

    const { unmount } = renderTab()
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
    renderTab()

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
    renderTab()

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

  it('renders the New Transaction button and per-row edit/delete action icons', async () => {
    await renderLoaded(emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 2, totalPages: 1 }))

    expect(screen.getByRole('button', { name: '+ New Transaction' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Edit Salary · Aug 1, 2026' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete Salary · Aug 1, 2026' })).toBeInTheDocument()
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

    renderTab()

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

  it('opens the create form with empty fields and today as the default date when New Transaction is clicked', async () => {
    await renderLoaded(emptyPage())

    fireEvent.click(screen.getByRole('button', { name: '+ New Transaction' }))

    const form = formWithin()
    expect(form.getByRole('heading', { name: 'Create New Transaction' })).toBeInTheDocument()
    expect(form.getByLabelText('Type')).toHaveValue('')
    expect(form.getByLabelText('Category')).toHaveValue('')
    expect((screen.getByLabelText('Amount') as HTMLInputElement).value).toBe('')
    expect(screen.getByLabelText('Date')).toHaveValue(new Date().toISOString().slice(0, 10))
    expect((screen.getByLabelText('Description') as HTMLInputElement).value).toBe('')
  })

  it('opens the form prefilled with the clicked transaction when its edit icon is clicked', async () => {
    await renderLoaded(emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 2, totalPages: 1 }))

    fireEvent.click(screen.getByRole('button', { name: 'Edit Groceries · Aug 14, 2026' }))

    const form = formWithin()
    expect(form.getByRole('heading', { name: 'Edit Transaction' })).toBeInTheDocument()
    expect(form.getByLabelText('Type')).toHaveValue('EXPENSE')
    expect(form.getByLabelText('Category')).toHaveValue('1')
    expect((screen.getByLabelText('Amount') as HTMLInputElement).value).toBe('12500')
    expect(screen.getByLabelText('Date')).toHaveValue('2026-08-14')
    expect((screen.getByLabelText('Description') as HTMLInputElement).value).toBe('')
  })

  it('rejects a submit with a required field left empty, without calling the API', async () => {
    await renderLoaded(emptyPage())
    fireEvent.click(screen.getByRole('button', { name: '+ New Transaction' }))
    const callsBefore = transactionCalls().length

    await submitForm('Create Transaction')

    expect(transactionCalls()).toHaveLength(callsBefore)
    expect(screen.getByText('Type, category, amount, and date are required')).toHaveClass('auth-error')
    expect(screen.getByRole('button', { name: 'Create Transaction' })).toBeInTheDocument()
  })

  it('rejects a non-positive or non-numeric amount, without calling the API', async () => {
    await renderLoaded(emptyPage())
    fireEvent.click(screen.getByRole('button', { name: '+ New Transaction' }))
    const form = formWithin()
    fireEvent.change(form.getByLabelText('Type'), { target: { value: 'EXPENSE' } })
    fireEvent.change(form.getByLabelText('Category'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '-5' } })
    const callsBefore = transactionCalls().length

    await submitForm('Create Transaction')

    expect(transactionCalls()).toHaveLength(callsBefore)
    expect(screen.getByText('Amount must be a positive number')).toHaveClass('auth-error')
  })

  it('creates a transaction with a typed numeric payload, closes the form, shows success, and resets to page 0 from a later page', async () => {
    await renderLoaded(
      emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 40, totalPages: 2, first: true, last: false }),
    )

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))
    await waitFor(() => {
      expect(lastTransactionParams().get('page')).toBe('1')
    })

    fireEvent.click(screen.getByRole('button', { name: '+ New Transaction' }))
    const form = formWithin()
    fireEvent.change(form.getByLabelText('Type'), { target: { value: 'EXPENSE' } })
    fireEvent.change(form.getByLabelText('Category'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '250.5' } })
    fireEvent.change(screen.getByLabelText('Date'), { target: { value: '2026-08-15' } })

    mockRoutes({
      transactions: (init) =>
        init?.method === 'POST'
          ? jsonResponse(201, {
              id: 999,
              type: 'EXPENSE',
              description: null,
              categoryId: 1,
              categoryName: 'Groceries',
              amount: 250.5,
              date: '2026-08-15',
            })
          : jsonResponse(
              200,
              emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 41, totalPages: 3, first: true, last: false }),
            ),
    })

    await submitForm('Create Transaction')

    const createCall = transactionCalls().find(([, init]) => init?.method === 'POST')
    expect(createCall).toBeDefined()
    expect(JSON.parse((createCall?.[1]?.body as string) ?? '{}')).toEqual({
      type: 'EXPENSE',
      categoryId: 1,
      amount: 250.5,
      date: '2026-08-15',
      description: '',
    })

    expect(screen.getByText('Transaction created successfully')).toHaveClass('auth-success')
    expect(screen.queryByRole('heading', { name: 'Create New Transaction' })).toBeNull()
    await waitFor(() => {
      expect(lastTransactionParams().get('page')).toBe('0')
    })
  })

  it('creates a transaction and reloads without changing page when already on page 0', async () => {
    await renderLoaded(emptyPage())

    fireEvent.click(screen.getByRole('button', { name: '+ New Transaction' }))
    const form = formWithin()
    fireEvent.change(form.getByLabelText('Type'), { target: { value: 'INCOME' } })
    fireEvent.change(form.getByLabelText('Category'), { target: { value: '2' } })
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '1000' } })

    mockRoutes({
      transactions: (init) =>
        init?.method === 'POST'
          ? jsonResponse(201, {
              id: 999,
              type: 'INCOME',
              description: null,
              categoryId: 2,
              categoryName: 'Salary',
              amount: 1000,
              date: new Date().toISOString().slice(0, 10),
            })
          : jsonResponse(200, emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 3, totalPages: 1 })),
    })

    const callsBefore = transactionCalls().length
    await submitForm('Create Transaction')

    const postCalls = transactionCalls().filter(([, init]) => init?.method === 'POST')
    expect(postCalls).toHaveLength(1)
    expect(transactionCalls().length).toBeGreaterThan(callsBefore)
    expect(screen.getByText('Transaction created successfully')).toHaveClass('auth-success')
    await screen.findByText('August salary')
  })

  it('updates a transaction, closes the form, shows success, and reloads the same page', async () => {
    await renderLoaded(emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 2, totalPages: 1 }))

    fireEvent.click(screen.getByRole('button', { name: 'Edit Groceries · Aug 14, 2026' }))
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '13000' } })

    mockRoutes({
      transactions: (init) =>
        init?.method === 'PUT'
          ? jsonResponse(200, { ...SAMPLE_TRANSACTIONS[1], amount: 13000 })
          : jsonResponse(200, emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 2, totalPages: 1 })),
    })

    await submitForm('Update Transaction')

    const putCall = transactionCalls().find(([, init]) => init?.method === 'PUT')
    expect(putCall?.[0]).toBe('/api/transactions/102')
    expect(JSON.parse((putCall?.[1]?.body as string) ?? '{}')).toEqual({
      type: 'EXPENSE',
      categoryId: 1,
      amount: 13000,
      date: '2026-08-14',
      description: '',
    })
    expect(screen.getByText('Transaction updated successfully')).toHaveClass('auth-success')
    expect(screen.queryByRole('heading', { name: 'Edit Transaction' })).toBeNull()
    await waitFor(() => {
      expect(lastTransactionParams().get('page')).toBe('0')
    })
  })

  it('does not call deleteTransaction when the confirmation is dismissed', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    await renderLoaded(emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 2, totalPages: 1 }))
    const callsBefore = transactionCalls().length

    fireEvent.click(screen.getByRole('button', { name: 'Delete Groceries · Aug 14, 2026' }))

    expect(transactionCalls()).toHaveLength(callsBefore)
    expect(screen.getByText('Groceries · Aug 14, 2026')).toBeInTheDocument()
  })

  it('reloads the same page after deleting when more than one row remains on that page', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    mockFetch((url, init) => {
      if (url.includes('/api/categories/user')) {
        return jsonResponse(200, CATEGORIES)
      }
      if (init?.method === 'DELETE') {
        return emptyResponse(204)
      }
      const requestedPage = paramsOf(url).get('page')
      if (requestedPage === '1') {
        return jsonResponse(
          200,
          emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 22, totalPages: 2, first: false, last: true }),
        )
      }
      return jsonResponse(
        200,
        emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 22, totalPages: 2, first: true, last: false }),
      )
    })

    renderTab()
    await screen.findByText('August salary')

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))
    await waitFor(() => {
      expect(lastTransactionParams().get('page')).toBe('1')
    })

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Delete Groceries · Aug 14, 2026' }))
    })

    expect(transactionCalls().some(([, init]) => init?.method === 'DELETE')).toBe(true)
    await waitFor(() => {
      expect(lastTransactionParams().get('page')).toBe('1')
    })
    expect(screen.getByText('Transaction deleted successfully')).toHaveClass('auth-success')
  })

  it('decrements the page when the deleted row was the only row on a page after the first', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    mockFetch((url, init) => {
      if (url.includes('/api/categories/user')) {
        return jsonResponse(200, CATEGORIES)
      }
      if (init?.method === 'DELETE') {
        return emptyResponse(204)
      }
      const requestedPage = paramsOf(url).get('page')
      if (requestedPage === '1') {
        return jsonResponse(
          200,
          emptyPage({
            content: [SAMPLE_TRANSACTIONS[1]],
            totalElements: 21,
            totalPages: 2,
            first: false,
            last: true,
          }),
        )
      }
      return jsonResponse(
        200,
        emptyPage({ content: SAMPLE_TRANSACTIONS, totalElements: 21, totalPages: 2, first: true, last: false }),
      )
    })

    renderTab()
    await screen.findByText('August salary')

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))
    await screen.findByText('Groceries · Aug 14, 2026')

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Delete Groceries · Aug 14, 2026' }))
    })

    expect(transactionCalls().some(([, init]) => init?.method === 'DELETE')).toBe(true)
    await waitFor(() => {
      expect(lastTransactionParams().get('page')).toBe('0')
    })
    expect(screen.getByText('Transaction deleted successfully')).toHaveClass('auth-success')
  })

  it('shows server field errors beside the fields and keeps the form open', async () => {
    await renderLoaded(emptyPage())
    fireEvent.click(screen.getByRole('button', { name: '+ New Transaction' }))
    const form = formWithin()
    fireEvent.change(form.getByLabelText('Type'), { target: { value: 'EXPENSE' } })
    fireEvent.change(form.getByLabelText('Category'), { target: { value: '1' } })
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '10' } })

    mockRoutes({
      transactions: (init) =>
        init?.method === 'POST'
          ? errorResponse(400, 'Validation failed', { date: 'date is required' }, '/api/transactions')
          : jsonResponse(200, emptyPage()),
    })

    await submitForm('Create Transaction')

    expect(screen.getByText('date is required')).toHaveClass('field-error')
    expect(screen.getByText('Validation failed')).toHaveClass('auth-error')
    expect(screen.getByRole('heading', { name: 'Create New Transaction' })).toBeInTheDocument()
  })

  it('closes the form without an API call and clears unsaved changes on Cancel', async () => {
    await renderLoaded(emptyPage())
    fireEvent.click(screen.getByRole('button', { name: '+ New Transaction' }))
    fireEvent.change(formWithin().getByLabelText('Type'), { target: { value: 'EXPENSE' } })
    const callsBefore = transactionCalls().length

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(transactionCalls()).toHaveLength(callsBefore)
    expect(screen.queryByRole('heading', { name: 'Create New Transaction' })).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: '+ New Transaction' }))
    expect(formWithin().getByLabelText('Type')).toHaveValue('')
  })

  it('does not filter the form category select by the chosen type, and does not re-fetch categories when the form opens', async () => {
    await renderLoaded(emptyPage())
    const categoryCallsBefore = vi
      .mocked(globalThis.fetch)
      .mock.calls.filter(([url]) => String(url).includes('/api/categories/user')).length

    fireEvent.click(screen.getByRole('button', { name: '+ New Transaction' }))
    const form = formWithin()
    fireEvent.change(form.getByLabelText('Type'), { target: { value: 'INCOME' } })

    const options = Array.from((form.getByLabelText('Category') as HTMLSelectElement).options).map(
      (option) => option.textContent,
    )
    expect(options).toEqual(['Select category', 'Groceries', 'Salary'])

    const categoryCallsAfter = vi
      .mocked(globalThis.fetch)
      .mock.calls.filter(([url]) => String(url).includes('/api/categories/user')).length
    expect(categoryCallsAfter).toBe(categoryCallsBefore)
  })

  it('auto-opens the create form when the URL has ?new=true on mount', async () => {
    await renderLoaded(emptyPage(), ['/transactions?new=true'])

    const form = formWithin()
    expect(form.getByRole('heading', { name: 'Create New Transaction' })).toBeInTheDocument()
    expect(form.getByLabelText('Type')).toHaveValue('')
    expect(form.getByLabelText('Category')).toHaveValue('')
    expect((screen.getByLabelText('Amount') as HTMLInputElement).value).toBe('')
    expect(screen.getByLabelText('Date')).toHaveValue(new Date().toISOString().slice(0, 10))
    expect((screen.getByLabelText('Description') as HTMLInputElement).value).toBe('')
  })

  it('does not auto-open the form when there is no query param on load', async () => {
    await renderLoaded(emptyPage())

    expect(screen.queryByRole('heading', { name: 'Create New Transaction' })).toBeNull()
    expect(document.querySelector('.transaction-form-section')).toBeNull()
  })

  it('strips the new query param from the URL after auto-opening the form', async () => {
    await renderLoaded(emptyPage(), ['/transactions?new=true'])

    await waitFor(() => {
      expect(screen.getByTestId('search-probe')).toHaveTextContent('')
    })
    expect(screen.getByTestId('search-probe').textContent).not.toContain('new')
  })

  it('replaces the ?new=true history entry instead of pushing, so Back skips it', async () => {
    mockRoutes({})

    function BackButton() {
      const navigate = useNavigate()
      return (
        <button type="button" onClick={() => navigate(-1)}>
          go back
        </button>
      )
    }

    render(
      <MemoryRouter initialEntries={['/dashboard', '/transactions?new=true']} initialIndex={1}>
        <Routes>
          <Route path="/dashboard" element={<div>Dashboard page</div>} />
          <Route
            path="/transactions"
            element={
              <>
                <TransactionsTab />
                <BackButton />
              </>
            }
          />
        </Routes>
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Create New Transaction' })).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: 'go back' }))

    expect(await screen.findByText('Dashboard page')).toBeInTheDocument()
  })

  it('keeps the form closed on unrelated rerenders after the new param is consumed, without looping', async () => {
    const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {})

    await renderLoaded(emptyPage(), ['/transactions?new=true'])
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Create New Transaction' })).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('heading', { name: 'Create New Transaction' })).toBeNull()

    fireEvent.change(screen.getByLabelText('Type'), { target: { value: 'INCOME' } })
    await waitFor(() => {
      expect(lastTransactionParams().get('type')).toBe('INCOME')
    })

    expect(screen.queryByRole('heading', { name: 'Create New Transaction' })).toBeNull()
    const loopErrors = consoleErrorSpy.mock.calls.filter(([message]) =>
      String(message).includes('Maximum update depth exceeded'),
    )
    expect(loopErrors).toHaveLength(0)

    consoleErrorSpy.mockRestore()
  })

  it('does not reopen the form when an unrelated query param appears in the URL', async () => {
    mockRoutes({})

    function AddUnrelatedParam() {
      const [, setUnrelatedSearchParams] = useSearchParams()
      return (
        <button type="button" onClick={() => setUnrelatedSearchParams({ foo: 'bar' })}>
          add unrelated param
        </button>
      )
    }

    render(
      <MemoryRouter initialEntries={['/transactions']}>
        <TransactionsTab />
        <AddUnrelatedParam />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.queryByRole('status')).toBeNull()
    })
    expect(screen.queryByRole('heading', { name: 'Create New Transaction' })).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: 'add unrelated param' }))

    expect(screen.queryByRole('heading', { name: 'Create New Transaction' })).toBeNull()
  })

  it('still opens the create form from its own New Transaction button when there is no query param', async () => {
    await renderLoaded(emptyPage())

    fireEvent.click(screen.getByRole('button', { name: '+ New Transaction' }))

    const form = formWithin()
    expect(form.getByRole('heading', { name: 'Create New Transaction' })).toBeInTheDocument()
  })
})
