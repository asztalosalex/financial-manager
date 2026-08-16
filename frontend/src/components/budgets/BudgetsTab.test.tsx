import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import BudgetsTab from './BudgetsTab'
import { clearCookies, emptyResponse, errorResponse, jsonResponse } from '../../test/helpers'
import type { BudgetResponseDto, CategoryResponseDto, PageResponse } from '../../api/types'

type FetchHandler = (url: string, init: RequestInit | undefined) => Response

function mockFetch(handler: FetchHandler) {
  vi.mocked(globalThis.fetch).mockImplementation((input, init) =>
    Promise.resolve(handler(String(input), init)),
  )
}

function paramsOf(url: string): URLSearchParams {
  return new URL(url, 'http://localhost').searchParams
}

function budgetCalls() {
  return vi.mocked(globalThis.fetch).mock.calls.filter(([url]) => String(url).includes('/api/budgets'))
}

function lastBudgetParams(): URLSearchParams {
  const calls = budgetCalls()
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

const CATEGORIES: CategoryResponseDto[] = [
  { id: 1, name: 'Groceries', description: 'Food' },
  { id: 2, name: 'Housing', description: 'Rent and utilities' },
]

function emptyPage(overrides: Partial<PageResponse<BudgetResponseDto>> = {}): PageResponse<BudgetResponseDto> {
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

const SAMPLE_BUDGETS: BudgetResponseDto[] = [
  {
    id: 101,
    amount: 150000,
    month: '2026-08-01',
    categoryId: 1,
    categoryName: 'Groceries',
  },
  {
    id: 102,
    amount: 300000,
    month: '2026-08-15',
    categoryId: 2,
    categoryName: 'Housing',
  },
]

interface Routes {
  budgets?: (init: RequestInit | undefined) => Response
  categories?: (init: RequestInit | undefined) => Response
}

function mockRoutes(routes: Routes) {
  mockFetch((url, init) => {
    if (url.includes('/api/categories/user')) {
      return (routes.categories ?? (() => jsonResponse(200, CATEGORIES)))(init)
    }
    return (routes.budgets ?? (() => jsonResponse(200, emptyPage())))(init)
  })
}

async function renderLoaded(budgetsPage: PageResponse<BudgetResponseDto>) {
  mockRoutes({ budgets: () => jsonResponse(200, budgetsPage) })
  render(<BudgetsTab />)
  await waitFor(() => {
    expect(screen.queryByRole('status')).toBeNull()
  })
}

function formWithin() {
  return within(document.querySelector('.budget-form-section') as HTMLElement)
}

function submitForm(label: string) {
  return act(async () => {
    fireEvent.click(screen.getByRole('button', { name: label }))
  })
}

describe('BudgetsTab', () => {
  beforeEach(() => {
    clearCookies()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    clearCookies()
  })

  it('shows a loading indicator until budgets arrive, with the filter bar visible and no list or pagination', () => {
    vi.mocked(globalThis.fetch).mockReturnValue(new Promise<Response>(() => {}))

    render(<BudgetsTab />)

    expect(screen.getByRole('status')).toHaveTextContent('Loading budgets...')
    expect(screen.getByLabelText('Month')).toBeInTheDocument()
    expect(screen.queryByRole('list')).toBeNull()
    expect(screen.queryByText(/Page \d+ of/)).toBeNull()
  })

  it('fetches with the fixed page size and sort, and no filter parameters, on first mount', async () => {
    await renderLoaded(emptyPage())

    const params = lastBudgetParams()
    expect(params.get('page')).toBe('0')
    expect(params.get('size')).toBe('20')
    expect(params.get('sort')).toBe('month,desc')
    expect(params.has('month')).toBe(false)
    expect(params.has('categoryId')).toBe(false)
  })

  it('aborts the in-flight budgets request on unmount', () => {
    let capturedSignal: AbortSignal | undefined
    mockFetch((url, init) => {
      if (url.includes('/api/budgets')) {
        capturedSignal = init?.signal ?? undefined
      }
      return jsonResponse(200, emptyPage())
    })

    const { unmount } = render(<BudgetsTab />)
    unmount()

    expect(capturedSignal?.aborted).toBe(true)
  })

  it('aborts the previous in-flight request when a filter change triggers a new one', async () => {
    await renderLoaded(emptyPage())
    const signals: AbortSignal[] = []
    vi.mocked(globalThis.fetch).mockImplementation((input, init) => {
      const url = String(input)
      if (url.includes('/api/categories/user')) {
        return Promise.resolve(jsonResponse(200, CATEGORIES))
      }
      if (init?.signal) {
        signals.push(init.signal)
      }
      return new Promise<Response>(() => {})
    })

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '1' } })

    await waitFor(() => {
      expect(signals).toHaveLength(1)
    })
    expect(signals[0].aborted).toBe(false)

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '2' } })

    await waitFor(() => {
      expect(signals).toHaveLength(2)
    })
    expect(signals[0].aborted).toBe(true)
  })

  it('renders each row with the category name and a full month-and-year label, day discarded', async () => {
    await renderLoaded(emptyPage({ content: SAMPLE_BUDGETS, totalElements: 2, totalPages: 1 }))

    const list = screen.getByRole('list')
    expect(within(list).getByText('Groceries')).toBeInTheDocument()
    expect(within(list).getByText('Housing')).toBeInTheDocument()
    expect(within(list).getAllByText('August 2026')).toHaveLength(2)
    expect(within(list).queryByText(/\b15\b/)).toBeNull()
    expect(within(list).queryByText(/\b1\b/)).toBeNull()
  })

  it('renders the amount unsigned, with the plain amount class rather than income or expense variants', async () => {
    await renderLoaded(emptyPage({ content: SAMPLE_BUDGETS, totalElements: 2, totalPages: 1 }))

    const list = screen.getByRole('list')
    const amount = within(list).getByText('150 000 Ft')
    expect(amount).toHaveClass('budget-row-amount')
    expect(amount.className).not.toMatch(/income|expense/)
  })

  it('shows the no-budgets empty state when there are no budgets and no active filters', async () => {
    await renderLoaded(emptyPage())

    expect(screen.getByText('No budgets yet. Add your first budget to get started.')).toBeInTheDocument()
    expect(screen.queryByText('No budgets match these filters.')).toBeNull()
  })

  it('shows the filtered-empty state with an inline Clear filters link once a filter is applied and the result is empty', async () => {
    await renderLoaded(emptyPage())

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '1' } })

    await waitFor(() => {
      expect(screen.getByText('No budgets match these filters.')).toBeInTheDocument()
    })
    expect(screen.queryByText('No budgets yet. Add your first budget to get started.')).toBeNull()
  })

  it('surfaces the server error message and hides the list and pagination', async () => {
    mockRoutes({ budgets: () => errorResponse(400, 'month must be a valid year-month') })
    render(<BudgetsTab />)

    await waitFor(() => {
      expect(screen.getByText('month must be a valid year-month')).toHaveClass('auth-error')
    })
    expect(screen.queryByRole('list')).toBeNull()
    expect(screen.queryByText(/Page \d+ of/)).toBeNull()
  })

  it('builds the category select options from fetchCategories results', async () => {
    await renderLoaded(emptyPage())

    const select = await screen.findByLabelText('Category')
    const options = Array.from((select as HTMLSelectElement).options).map((option) => option.textContent)
    expect(options).toEqual(['All categories', 'Groceries', 'Housing'])
  })

  it('leaves the category select with only All categories when fetchCategories fails, without affecting the budget list', async () => {
    mockRoutes({
      budgets: () => jsonResponse(200, emptyPage({ content: SAMPLE_BUDGETS, totalElements: 2, totalPages: 1 })),
      categories: () => errorResponse(500, 'Categories are temporarily unavailable'),
    })
    render(<BudgetsTab />)

    await screen.findByText('Groceries')

    const select = screen.getByLabelText('Category') as HTMLSelectElement
    expect(Array.from(select.options).map((option) => option.textContent)).toEqual(['All categories'])
    expect(screen.queryByText('Categories are temporarily unavailable')).toBeNull()
  })

  it('sends the month filter to fetchBudgets raw, without appending a day suffix', async () => {
    await renderLoaded(emptyPage())

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-08' } })

    await waitFor(() => {
      expect(lastBudgetParams().get('month')).toBe('2026-08')
    })
    expect(lastBudgetParams().get('month')).not.toBe('2026-08-01')
  })

  it('resets to page 0 and re-fetches when a filter changes after paging forward', async () => {
    await renderLoaded(
      emptyPage({ content: SAMPLE_BUDGETS, totalElements: 40, totalPages: 2, first: true, last: false }),
    )

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))
    await waitFor(() => {
      expect(lastBudgetParams().get('page')).toBe('1')
    })

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '1' } })

    await waitFor(() => {
      expect(lastBudgetParams().get('page')).toBe('0')
      expect(lastBudgetParams().get('categoryId')).toBe('1')
    })
  })

  it('changes only the page, not the filters, when Next is clicked', async () => {
    await renderLoaded(
      emptyPage({ content: SAMPLE_BUDGETS, totalElements: 40, totalPages: 2, first: true, last: false }),
    )

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-08' } })
    await waitFor(() => {
      expect(lastBudgetParams().get('month')).toBe('2026-08')
    })

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))

    await waitFor(() => {
      expect(lastBudgetParams().get('page')).toBe('1')
      expect(lastBudgetParams().get('month')).toBe('2026-08')
    })
  })

  it('resets filters and page to defaults when Clear filters is clicked', async () => {
    await renderLoaded(
      emptyPage({ content: SAMPLE_BUDGETS, totalElements: 40, totalPages: 2, first: true, last: false }),
    )

    fireEvent.change(screen.getByLabelText('Month'), { target: { value: '2026-08' } })
    await waitFor(() => {
      expect(lastBudgetParams().get('month')).toBe('2026-08')
    })

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))
    await waitFor(() => {
      expect(lastBudgetParams().get('page')).toBe('1')
    })

    fireEvent.click(screen.getByRole('button', { name: 'Clear filters' }))

    await waitFor(() => {
      expect(lastBudgetParams().get('page')).toBe('0')
      expect(lastBudgetParams().has('month')).toBe(false)
    })
  })

  it('renders the pagination bar with the 1-indexed page label once budgets load', async () => {
    await renderLoaded(
      emptyPage({ content: SAMPLE_BUDGETS, totalElements: 137, totalPages: 7, first: true, last: false }),
    )

    expect(screen.getByText('Page 1 of 7 (137 total)')).toBeInTheDocument()
  })

  it('disables Prev but not Next on the first page, wiring first/last without swapping them', async () => {
    await renderLoaded(
      emptyPage({ content: SAMPLE_BUDGETS, totalElements: 40, totalPages: 2, first: true, last: false }),
    )

    expect(screen.getByRole('button', { name: /Prev/ })).toBeDisabled()
    expect(screen.getByRole('button', { name: /Next/ })).not.toBeDisabled()
  })

  it('disables Next but not Prev on the last page, wiring first/last without swapping them', async () => {
    await renderLoaded(
      emptyPage({ content: SAMPLE_BUDGETS, totalElements: 40, totalPages: 2, first: false, last: true }),
    )

    expect(screen.getByRole('button', { name: /Next/ })).toBeDisabled()
    expect(screen.getByRole('button', { name: /Prev/ })).not.toBeDisabled()
  })

  it('sends the selected category id as a number, not a concatenated string or NaN', async () => {
    await renderLoaded(emptyPage())

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '2' } })

    await waitFor(() => {
      expect(lastBudgetParams().get('categoryId')).toBe('2')
    })
    expect(lastBudgetParams().get('categoryId')).not.toBe('22')
    expect(lastBudgetParams().get('categoryId')).not.toBe('NaN')
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

    render(<BudgetsTab />)

    await waitFor(() => {
      expect(deferred).toHaveLength(1)
    })

    fireEvent.change(screen.getByLabelText('Category'), { target: { value: '1' } })

    await waitFor(() => {
      expect(deferred).toHaveLength(2)
    })

    deferred[1].resolve(
      jsonResponse(200, emptyPage({ content: [SAMPLE_BUDGETS[0]], totalElements: 1, totalPages: 1 })),
    )
    await waitFor(() => {
      expect(within(screen.getByRole('list')).getByText('Groceries')).toBeInTheDocument()
    })

    deferred[0].resolve(
      jsonResponse(200, emptyPage({ content: [SAMPLE_BUDGETS[1]], totalElements: 1, totalPages: 1 })),
    )

    const list = screen.getByRole('list')
    await waitFor(() => {
      expect(within(list).getByText('Groceries')).toBeInTheDocument()
    })
    expect(within(list).queryByText('Housing')).toBeNull()
  })

  it('renders the New Budget button and per-row edit/delete action icons', async () => {
    await renderLoaded(emptyPage({ content: SAMPLE_BUDGETS, totalElements: 2, totalPages: 1 }))

    expect(screen.getByRole('button', { name: '+ New Budget' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Edit Groceries, August 2026' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete Groceries, August 2026' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Edit Housing, August 2026' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Delete Housing, August 2026' })).toBeInTheDocument()
  })

  it('names the surface with a single first-level Budgets heading', async () => {
    await renderLoaded(emptyPage())

    const headings = screen.getAllByRole('heading', { level: 1 })
    expect(headings).toHaveLength(1)
    expect(headings[0]).toHaveTextContent('Budgets')
  })

  it('opens the create form with empty fields and no default month when New Budget is clicked', async () => {
    await renderLoaded(emptyPage())

    fireEvent.click(screen.getByRole('button', { name: '+ New Budget' }))

    const form = formWithin()
    expect(form.getByRole('heading', { name: 'Create New Budget' })).toBeInTheDocument()
    expect(form.getByLabelText('Category')).toHaveValue('')
    expect(form.getByLabelText('Month')).toHaveValue('')
    expect((screen.getByLabelText('Amount') as HTMLInputElement).value).toBe('')
  })

  it('opens the form prefilled with the clicked budget when its edit icon is clicked', async () => {
    await renderLoaded(emptyPage({ content: SAMPLE_BUDGETS, totalElements: 2, totalPages: 1 }))

    fireEvent.click(screen.getByRole('button', { name: 'Edit Housing, August 2026' }))

    const form = formWithin()
    expect(form.getByRole('heading', { name: 'Edit Budget' })).toBeInTheDocument()
    expect(form.getByLabelText('Category')).toHaveValue('2')
    expect(form.getByLabelText('Month')).toHaveValue('2026-08')
    expect((screen.getByLabelText('Amount') as HTMLInputElement).value).toBe('300000')
  })

  it('rejects a submit with a required field left empty, without calling the API (M1)', async () => {
    await renderLoaded(emptyPage())
    fireEvent.click(screen.getByRole('button', { name: '+ New Budget' }))
    const callsBefore = budgetCalls().length

    await submitForm('Create Budget')

    expect(budgetCalls()).toHaveLength(callsBefore)
    expect(screen.getByText('Category, month, and amount are required')).toHaveClass('auth-error')
    expect(screen.getByRole('button', { name: 'Create Budget' })).toBeInTheDocument()
  })

  it('rejects a non-positive or non-numeric amount, without calling the API (M2)', async () => {
    await renderLoaded(emptyPage())
    fireEvent.click(screen.getByRole('button', { name: '+ New Budget' }))
    const form = formWithin()
    fireEvent.change(form.getByLabelText('Category'), { target: { value: '1' } })
    fireEvent.change(form.getByLabelText('Month'), { target: { value: '2026-08' } })
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '-5' } })
    const callsBefore = budgetCalls().length

    await submitForm('Create Budget')

    expect(budgetCalls()).toHaveLength(callsBefore)
    expect(screen.getByText('Amount must be a positive number')).toHaveClass('auth-error')
  })

  it('creates a budget with the month suffixed to a full date and a numeric amount (M3)', async () => {
    await renderLoaded(emptyPage())
    fireEvent.click(screen.getByRole('button', { name: '+ New Budget' }))
    const form = formWithin()
    fireEvent.change(form.getByLabelText('Category'), { target: { value: '1' } })
    fireEvent.change(form.getByLabelText('Month'), { target: { value: '2026-08' } })
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '150000' } })

    mockRoutes({
      budgets: (init) =>
        init?.method === 'POST'
          ? jsonResponse(201, {
              id: 999,
              amount: 150000,
              month: '2026-08-01',
              categoryId: 1,
              categoryName: 'Groceries',
            })
          : jsonResponse(200, emptyPage({ content: SAMPLE_BUDGETS, totalElements: 3, totalPages: 1 })),
    })

    await submitForm('Create Budget')

    const createCall = budgetCalls().find(([, init]) => init?.method === 'POST')
    expect(createCall).toBeDefined()
    expect(JSON.parse((createCall?.[1]?.body as string) ?? '{}')).toEqual({
      categoryId: 1,
      month: '2026-08-01',
      amount: 150000,
    })
  })

  it('creates a budget, closes the form, shows success, and resets to page 0 from a later page (M4)', async () => {
    await renderLoaded(
      emptyPage({ content: SAMPLE_BUDGETS, totalElements: 40, totalPages: 2, first: true, last: false }),
    )

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))
    await waitFor(() => {
      expect(lastBudgetParams().get('page')).toBe('1')
    })

    fireEvent.click(screen.getByRole('button', { name: '+ New Budget' }))
    const form = formWithin()
    fireEvent.change(form.getByLabelText('Category'), { target: { value: '1' } })
    fireEvent.change(form.getByLabelText('Month'), { target: { value: '2026-08' } })
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '150000' } })

    mockRoutes({
      budgets: (init) =>
        init?.method === 'POST'
          ? jsonResponse(201, {
              id: 999,
              amount: 150000,
              month: '2026-08-01',
              categoryId: 1,
              categoryName: 'Groceries',
            })
          : jsonResponse(200, emptyPage({ content: SAMPLE_BUDGETS, totalElements: 41, totalPages: 3, first: true, last: false })),
    })

    await submitForm('Create Budget')

    expect(screen.getByText('Budget created successfully')).toHaveClass('auth-success')
    expect(screen.queryByRole('heading', { name: 'Create New Budget' })).toBeNull()
    await waitFor(() => {
      expect(lastBudgetParams().get('page')).toBe('0')
    })
  })

  it('creates a budget and reloads without changing page when already on page 0 (M4)', async () => {
    await renderLoaded(emptyPage())

    fireEvent.click(screen.getByRole('button', { name: '+ New Budget' }))
    const form = formWithin()
    fireEvent.change(form.getByLabelText('Category'), { target: { value: '2' } })
    fireEvent.change(form.getByLabelText('Month'), { target: { value: '2026-09' } })
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '300000' } })

    mockRoutes({
      budgets: (init) =>
        init?.method === 'POST'
          ? jsonResponse(201, {
              id: 999,
              amount: 300000,
              month: '2026-09-01',
              categoryId: 2,
              categoryName: 'Housing',
            })
          : jsonResponse(200, emptyPage({ content: SAMPLE_BUDGETS, totalElements: 3, totalPages: 1 })),
    })

    const callsBefore = budgetCalls().length
    await submitForm('Create Budget')

    const postCalls = budgetCalls().filter(([, init]) => init?.method === 'POST')
    expect(postCalls).toHaveLength(1)
    expect(budgetCalls().length).toBeGreaterThan(callsBefore)
    expect(screen.getByText('Budget created successfully')).toHaveClass('auth-success')
    await waitFor(() => {
      expect(within(screen.getByRole('list')).getByText('Groceries')).toBeInTheDocument()
    })
  })

  it('updates a budget with the month suffixed to a full date, closes the form, shows success, and reloads the same page', async () => {
    await renderLoaded(emptyPage({ content: SAMPLE_BUDGETS, totalElements: 2, totalPages: 1 }))

    fireEvent.click(screen.getByRole('button', { name: 'Edit Housing, August 2026' }))
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '320000' } })

    mockRoutes({
      budgets: (init) =>
        init?.method === 'PUT'
          ? jsonResponse(200, { ...SAMPLE_BUDGETS[1], amount: 320000 })
          : jsonResponse(200, emptyPage({ content: SAMPLE_BUDGETS, totalElements: 2, totalPages: 1 })),
    })

    await submitForm('Update Budget')

    const putCall = budgetCalls().find(([, init]) => init?.method === 'PUT')
    expect(putCall?.[0]).toBe('/api/budgets/102')
    expect(JSON.parse((putCall?.[1]?.body as string) ?? '{}')).toEqual({
      categoryId: 2,
      month: '2026-08-01',
      amount: 320000,
    })
    expect(screen.getByText('Budget updated successfully')).toHaveClass('auth-success')
    expect(screen.queryByRole('heading', { name: 'Edit Budget' })).toBeNull()
    await waitFor(() => {
      expect(lastBudgetParams().get('page')).toBe('0')
    })
  })

  it('does not call deleteBudget when the confirmation is dismissed (M6)', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    await renderLoaded(emptyPage({ content: SAMPLE_BUDGETS, totalElements: 2, totalPages: 1 }))
    const callsBefore = budgetCalls().length

    fireEvent.click(screen.getByRole('button', { name: 'Delete Groceries, August 2026' }))

    expect(budgetCalls()).toHaveLength(callsBefore)
    expect(within(screen.getByRole('list')).getByText('Groceries')).toBeInTheDocument()
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
          emptyPage({ content: SAMPLE_BUDGETS, totalElements: 22, totalPages: 2, first: false, last: true }),
        )
      }
      return jsonResponse(
        200,
        emptyPage({ content: SAMPLE_BUDGETS, totalElements: 22, totalPages: 2, first: true, last: false }),
      )
    })

    render(<BudgetsTab />)
    await waitFor(() => {
      expect(within(screen.getByRole('list')).getByText('Groceries')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))
    await waitFor(() => {
      expect(lastBudgetParams().get('page')).toBe('1')
    })

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Delete Groceries, August 2026' }))
    })

    expect(budgetCalls().some(([, init]) => init?.method === 'DELETE')).toBe(true)
    await waitFor(() => {
      expect(lastBudgetParams().get('page')).toBe('1')
    })
    expect(screen.getByText('Budget deleted successfully')).toHaveClass('auth-success')
  })

  it('decrements the page when the deleted row was the only row on a page after the first (M5)', async () => {
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
            content: [SAMPLE_BUDGETS[1]],
            totalElements: 21,
            totalPages: 2,
            first: false,
            last: true,
          }),
        )
      }
      return jsonResponse(
        200,
        emptyPage({ content: SAMPLE_BUDGETS, totalElements: 21, totalPages: 2, first: true, last: false }),
      )
    })

    render(<BudgetsTab />)
    await waitFor(() => {
      expect(within(screen.getByRole('list')).getByText('Groceries')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /Next/ }))
    await waitFor(() => {
      expect(within(screen.getByRole('list')).getByText('Housing')).toBeInTheDocument()
    })

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Delete Housing, August 2026' }))
    })

    expect(budgetCalls().some(([, init]) => init?.method === 'DELETE')).toBe(true)
    await waitFor(() => {
      expect(lastBudgetParams().get('page')).toBe('0')
    })
    expect(screen.getByText('Budget deleted successfully')).toHaveClass('auth-success')
  })

  it('shows server field errors beside the fields and keeps the form open (M7)', async () => {
    await renderLoaded(emptyPage())
    fireEvent.click(screen.getByRole('button', { name: '+ New Budget' }))
    const form = formWithin()
    fireEvent.change(form.getByLabelText('Category'), { target: { value: '1' } })
    fireEvent.change(form.getByLabelText('Month'), { target: { value: '2026-08' } })
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '10' } })

    mockRoutes({
      budgets: (init) =>
        init?.method === 'POST'
          ? errorResponse(400, 'Validation failed', { month: 'a budget already exists for this month' }, '/api/budgets')
          : jsonResponse(200, emptyPage()),
    })

    await submitForm('Create Budget')

    expect(screen.getByText('a budget already exists for this month')).toHaveClass('field-error')
    expect(screen.getByText('Validation failed')).toHaveClass('auth-error')
    expect(screen.getByRole('heading', { name: 'Create New Budget' })).toBeInTheDocument()
  })

  it('closes the form without an API call and clears unsaved changes on Cancel', async () => {
    await renderLoaded(emptyPage())
    fireEvent.click(screen.getByRole('button', { name: '+ New Budget' }))
    fireEvent.change(formWithin().getByLabelText('Month'), { target: { value: '2026-08' } })
    const callsBefore = budgetCalls().length

    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))

    expect(budgetCalls()).toHaveLength(callsBefore)
    expect(screen.queryByRole('heading', { name: 'Create New Budget' })).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: '+ New Budget' }))
    expect(formWithin().getByLabelText('Month')).toHaveValue('')
  })

  it('does not re-fetch categories when the form opens, and lists every category unfiltered', async () => {
    await renderLoaded(emptyPage())
    const categoryCallsBefore = vi
      .mocked(globalThis.fetch)
      .mock.calls.filter(([url]) => String(url).includes('/api/categories/user')).length

    fireEvent.click(screen.getByRole('button', { name: '+ New Budget' }))
    const form = formWithin()

    const options = Array.from((form.getByLabelText('Category') as HTMLSelectElement).options).map(
      (option) => option.textContent,
    )
    expect(options).toEqual(['Select category', 'Groceries', 'Housing'])

    const categoryCallsAfter = vi
      .mocked(globalThis.fetch)
      .mock.calls.filter(([url]) => String(url).includes('/api/categories/user')).length
    expect(categoryCallsAfter).toBe(categoryCallsBefore)
  })
})
