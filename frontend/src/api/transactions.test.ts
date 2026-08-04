import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchTransactions } from './transactions'
import { ApiError } from './ApiError'
import { setUnauthorizedHandler } from './client'
import { errorResponse, jsonResponse } from '../test/helpers'
import type { PageResponse, TransactionResponseDto } from './types'

const FIRST_PAGE: PageResponse<TransactionResponseDto> = {
  content: [
    {
      id: 11,
      type: 'EXPENSE',
      description: 'Weekly shop',
      categoryId: 1,
      categoryName: 'Groceries',
      amount: 42.5,
      date: '2026-07-30',
    },
  ],
  page: 0,
  size: 20,
  totalElements: 137,
  totalPages: 7,
  first: true,
  last: false,
}

const EMPTY_PAGE: PageResponse<TransactionResponseDto> = {
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

describe('transactions api', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('requests the bare collection path when no query is given', async () => {
    respondWith(FIRST_PAGE)

    const page = await fetchTransactions()

    expect(requestedUrl()).toBe('/api/transactions')
    const [, init] = lastCall()
    expect(init?.method).toBe('GET')
    expect(init?.credentials).toBe('include')
    expect(page).toEqual(FIRST_PAGE)
  })

  it('returns the page wrapper exactly as it came off the wire', async () => {
    respondWith(FIRST_PAGE)

    const page = await fetchTransactions()

    expect(page.content).toHaveLength(1)
    expect(page.content[0].amount).toBe(42.5)
    expect(page.content[0].type).toBe('EXPENSE')
    expect(page.page).toBe(0)
    expect(page.size).toBe(20)
    expect(page.totalElements).toBe(137)
    expect(page.totalPages).toBe(7)
    expect(page.first).toBe(true)
    expect(page.last).toBe(false)
  })

  it('serializes every supplied paging and filter parameter', async () => {
    respondWith(EMPTY_PAGE)

    await fetchTransactions({
      page: 2,
      size: 50,
      sort: 'amount,asc',
      from: '2026-01-01',
      to: '2026-06-30',
      categoryId: 3,
      type: 'EXPENSE',
    })

    const params = requestedParams()
    expect(params.get('page')).toBe('2')
    expect(params.get('size')).toBe('50')
    expect(params.get('sort')).toBe('amount,asc')
    expect(params.get('from')).toBe('2026-01-01')
    expect(params.get('to')).toBe('2026-06-30')
    expect(params.get('categoryId')).toBe('3')
    expect(params.get('type')).toBe('EXPENSE')
    expect([...params.keys()]).toHaveLength(7)
  })

  it('omits absent parameters instead of sending them empty', async () => {
    respondWith(EMPTY_PAGE)

    await fetchTransactions({ size: 50 })

    expect(requestedUrl()).toBe('/api/transactions?size=50')
    expect(requestedUrl()).not.toContain('from=')
    expect(requestedUrl()).not.toContain('to=')
    expect(requestedUrl()).not.toContain('categoryId=')
    expect(requestedUrl()).not.toContain('type=')
    expect(requestedUrl()).not.toContain('sort=')
  })

  it('sends the first page explicitly rather than dropping it as falsy', async () => {
    respondWith(FIRST_PAGE)

    await fetchTransactions({ page: 0 })

    expect(requestedUrl()).toBe('/api/transactions?page=0')
    expect(requestedParams().get('page')).toBe('0')
  })

  it('drops blank date filters so the backend never sees an empty bound', async () => {
    respondWith(EMPTY_PAGE)

    await fetchTransactions({ from: '   ', to: '', categoryId: 3 })

    expect(requestedUrl()).toBe('/api/transactions?categoryId=3')
  })

  it('forwards the abort signal to the request', async () => {
    respondWith(FIRST_PAGE)
    const controller = new AbortController()

    await fetchTransactions({}, controller.signal)

    const [, init] = lastCall()
    expect(init?.signal).toBe(controller.signal)
  })

  it('raises the rejected paging parameters as an ApiError with the backend field keys', async () => {
    vi.mocked(globalThis.fetch).mockImplementation(() =>
      Promise.resolve(
        errorResponse(
          400,
          'Validation failed',
          { size: 'size must be between 1 and 100' },
          '/api/transactions',
        ),
      ),
    )

    const caught = await fetchTransactions({ size: 500 }).catch((error: unknown) => error)

    expect(caught).toBeInstanceOf(ApiError)
    const apiError = caught as ApiError
    expect(apiError.status).toBe(400)
    expect(apiError.message).toBe('Validation failed')
    expect(apiError.fieldErrors).toEqual({ size: 'size must be between 1 and 100' })
    expect(requestedUrl()).toBe('/api/transactions?size=500')
  })
})
