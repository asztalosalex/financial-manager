import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { createBudget, deleteBudget, fetchBudgets, updateBudget } from './budgets'
import { ApiError } from './ApiError'
import { setUnauthorizedHandler } from './client'
import { emptyResponse, errorResponse, jsonResponse } from '../test/helpers'
import type { BudgetResponseDto, CreateBudgetDto, PageResponse } from './types'

const FIRST_PAGE: PageResponse<BudgetResponseDto> = {
  content: [
    {
      id: 11,
      amount: 150000,
      month: '2026-08-01',
      categoryId: 1,
      categoryName: 'Groceries',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 137,
  totalPages: 7,
  first: true,
  last: false,
}

const EMPTY_PAGE: PageResponse<BudgetResponseDto> = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
}

function respondWith(body: unknown, status = 200) {
  vi.mocked(globalThis.fetch).mockImplementation(() => Promise.resolve(jsonResponse(status, body)))
}

function lastCall() {
  const calls = vi.mocked(globalThis.fetch).mock.calls
  return calls[calls.length - 1]
}

function requestedUrl(): string {
  return String(lastCall()[0])
}

function requestedParams(): URLSearchParams {
  const [, search] = requestedUrl().split('?')
  return new URLSearchParams(search ?? '')
}

describe('budgets api', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('requests the bare collection path when no query is given', async () => {
    respondWith(FIRST_PAGE)

    const page = await fetchBudgets()

    expect(requestedUrl()).toBe('/api/budgets')
    const [, init] = lastCall()
    expect(init?.method).toBe('GET')
    expect(init?.credentials).toBe('include')
    expect(page).toEqual(FIRST_PAGE)
  })

  it('returns the page wrapper exactly as it came off the wire', async () => {
    respondWith(FIRST_PAGE)

    const page = await fetchBudgets()

    expect(page.content).toHaveLength(1)
    expect(page.content[0].amount).toBe(150000)
    expect(page.content[0].month).toBe('2026-08-01')
    expect(page.content[0].categoryName).toBe('Groceries')
    expect(page.page).toBe(0)
    expect(page.size).toBe(20)
    expect(page.totalElements).toBe(137)
    expect(page.totalPages).toBe(7)
    expect(page.first).toBe(true)
    expect(page.last).toBe(false)
  })

  it('serializes every supplied paging and filter parameter', async () => {
    respondWith(EMPTY_PAGE)

    await fetchBudgets({
      page: 2,
      size: 50,
      sort: 'amount,asc',
      month: '2026-08',
      categoryId: 3,
    })

    const params = requestedParams()
    expect(params.get('page')).toBe('2')
    expect(params.get('size')).toBe('50')
    expect(params.get('sort')).toBe('amount,asc')
    expect(params.get('month')).toBe('2026-08')
    expect(params.get('categoryId')).toBe('3')
    expect([...params.keys()]).toHaveLength(5)
  })

  it('sends the month filter raw, without appending a day suffix', async () => {
    respondWith(EMPTY_PAGE)

    await fetchBudgets({ month: '2026-08' })

    expect(requestedParams().get('month')).toBe('2026-08')
    expect(requestedParams().get('month')).not.toBe('2026-08-01')
  })

  it('omits absent parameters instead of sending them empty', async () => {
    respondWith(EMPTY_PAGE)

    await fetchBudgets({ size: 50 })

    expect(requestedUrl()).toBe('/api/budgets?size=50')
    expect(requestedUrl()).not.toContain('month=')
    expect(requestedUrl()).not.toContain('categoryId=')
    expect(requestedUrl()).not.toContain('sort=')
  })

  it('sends the first page explicitly rather than dropping it as falsy', async () => {
    respondWith(FIRST_PAGE)

    await fetchBudgets({ page: 0 })

    expect(requestedUrl()).toBe('/api/budgets?page=0')
    expect(requestedParams().get('page')).toBe('0')
  })

  it('drops a blank month filter so the backend never sees an empty bound', async () => {
    respondWith(EMPTY_PAGE)

    await fetchBudgets({ month: '   ', categoryId: 3 })

    expect(requestedUrl()).toBe('/api/budgets?categoryId=3')
  })

  it('forwards the abort signal to the request', async () => {
    respondWith(FIRST_PAGE)
    const controller = new AbortController()

    await fetchBudgets({}, controller.signal)

    const [, init] = lastCall()
    expect(init?.signal).toBe(controller.signal)
  })

  it('raises the rejected paging parameters as an ApiError with the backend field keys', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        errorResponse(400, 'Validation failed', { size: 'size must be between 1 and 100' }, '/api/budgets'),
      ),
    )

    const caught = await fetchBudgets({ size: 500 }).catch((error: unknown) => error)

    expect(caught).toBeInstanceOf(ApiError)
    const apiError = caught as ApiError
    expect(apiError.status).toBe(400)
    expect(apiError.message).toBe('Validation failed')
    expect(apiError.fieldErrors).toEqual({ size: 'size must be between 1 and 100' })
    expect(requestedUrl()).toBe('/api/budgets?size=500')
  })

  it('posts a new budget to the collection path and returns the created copy', async () => {
    const created: BudgetResponseDto = {
      id: 200,
      amount: 150000,
      month: '2026-08-01',
      categoryId: 1,
      categoryName: 'Groceries',
    }
    respondWith(created)
    const payload: CreateBudgetDto = {
      categoryId: 1,
      month: '2026-08-01',
      amount: 150000,
    }

    const result = await createBudget(payload)

    expect(requestedUrl()).toBe('/api/budgets')
    const [, init] = lastCall()
    expect(init?.method).toBe('POST')
    expect(JSON.parse(init?.body as string)).toEqual(payload)
    expect(result).toEqual(created)
  })

  it('puts a full replacement to the budget id path and returns the updated copy', async () => {
    const updated: BudgetResponseDto = {
      id: 11,
      amount: 300000,
      month: '2026-09-01',
      categoryId: 2,
      categoryName: 'Housing',
    }
    respondWith(updated)
    const payload: CreateBudgetDto = {
      categoryId: 2,
      month: '2026-09-01',
      amount: 300000,
    }

    const result = await updateBudget(11, payload)

    expect(requestedUrl()).toBe('/api/budgets/11')
    const [, init] = lastCall()
    expect(init?.method).toBe('PUT')
    expect(JSON.parse(init?.body as string)).toEqual(payload)
    expect(result).toEqual(updated)
  })

  it('deletes a budget at its id path and resolves without a body', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() => Promise.resolve(emptyResponse(204)))

    const result = await deleteBudget(11)

    expect(requestedUrl()).toBe('/api/budgets/11')
    const [, init] = lastCall()
    expect(init?.method).toBe('DELETE')
    expect(result).toBeNull()
  })

  it('raises server-side validation errors from create as an ApiError with field keys', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        errorResponse(400, 'Validation failed', { amount: 'amount must be greater than 0' }, '/api/budgets'),
      ),
    )

    const caught = await createBudget({
      categoryId: 1,
      month: '2026-08-01',
      amount: -5,
    }).catch((error: unknown) => error)

    expect(caught).toBeInstanceOf(ApiError)
    expect((caught as ApiError).fieldErrors).toEqual({ amount: 'amount must be greater than 0' })
  })
})
