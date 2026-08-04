import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import CategoriesTab from './CategoriesTab'
import { setUnauthorizedHandler } from '../api/client'
import { clearCookies, emptyResponse, errorResponse, jsonResponse } from '../test/helpers'
import type { CategoryResponseDto } from '../api/types'

const GROCERIES: CategoryResponseDto = {
  id: 1,
  name: 'Groceries',
  description: 'Weekly food shopping',
}

const TRAVEL: CategoryResponseDto = {
  id: 2,
  name: 'Travel',
  description: 'Trips and commuting',
}

type FetchHandler = (url: string, init: RequestInit | undefined) => Response

function mockFetch(handler: FetchHandler) {
  vi.mocked(globalThis.fetch).mockImplementation((input, init) =>
    Promise.resolve(handler(String(input), init)),
  )
}

function fetchCalls() {
  return vi.mocked(globalThis.fetch).mock.calls
}

function lastRequest() {
  const calls = fetchCalls()
  return calls[calls.length - 1]
}

async function renderLoaded(handler: FetchHandler) {
  mockFetch(handler)
  render(<CategoriesTab />)
  await screen.findByRole('heading', { level: 3, name: 'Your Categories' })
}

function openCreateForm() {
  fireEvent.click(screen.getByRole('button', { name: 'Add New Category' }))
}

function fillForm(name: string, description: string) {
  fireEvent.change(screen.getByLabelText('Category Name:'), { target: { value: name } })
  fireEvent.change(screen.getByLabelText('Description:'), { target: { value: description } })
}

function submit(label: string) {
  return act(async () => {
    fireEvent.click(screen.getByRole('button', { name: label }))
  })
}

function categoryTitles() {
  return screen.queryAllByRole('heading', { level: 4 }).map((heading) => heading.textContent)
}

describe('CategoriesTab', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    clearCookies()
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    clearCookies()
  })

  it('shows a loading placeholder until the category list arrives', () => {
    vi.mocked(globalThis.fetch).mockReturnValue(new Promise<Response>(() => {}))

    render(<CategoriesTab />)

    expect(screen.getByRole('status')).toHaveTextContent('Loading categories...')
    expect(screen.queryByRole('button', { name: 'Add New Category' })).toBeNull()
  })

  it('lists the categories from the user-scoped endpoint', async () => {
    await renderLoaded(() => jsonResponse(200, [GROCERIES, TRAVEL]))

    const [url, init] = fetchCalls()[0]
    expect(url).toBe('/api/categories/user')
    expect(init?.method).toBe('GET')
    expect(init?.credentials).toBe('include')
    expect(categoryTitles()).toEqual(['Groceries', 'Travel'])
    expect(screen.getByText('Weekly food shopping')).toBeInTheDocument()
    expect(screen.queryByText(/No categories found/)).toBeNull()
  })

  it('shows the empty state when the user has no categories yet', async () => {
    await renderLoaded(() => jsonResponse(200, []))

    expect(
      screen.getByText('No categories found. Create your first category to get started!'),
    ).toBeInTheDocument()
    expect(categoryTitles()).toEqual([])
  })

  it('surfaces the server message when the list cannot be loaded', async () => {
    await renderLoaded(() =>
      errorResponse(500, 'Categories are temporarily unavailable', undefined, '/api/categories/user'),
    )

    expect(screen.getByText('Categories are temporarily unavailable')).toHaveClass('auth-error')
    expect(screen.queryByText('Loading categories...')).toBeNull()
  })

  it('creates a category, sends the CSRF token and appends the server copy to the list', async () => {
    document.cookie = 'XSRF-TOKEN=csrf-token-value; path=/'
    const created: CategoryResponseDto = { id: 3, name: 'Books', description: 'Reading material' }
    await renderLoaded((_url, init) =>
      init?.method === 'POST' ? jsonResponse(200, created) : jsonResponse(200, [GROCERIES]),
    )

    openCreateForm()
    fillForm('Books', 'Reading material')
    await submit('Create Category')

    const [url, init] = lastRequest()
    expect(url).toBe('/api/categories')
    expect(init?.method).toBe('POST')
    expect((init?.headers as Headers).get('Content-Type')).toBe('application/json')
    expect((init?.headers as Headers).get('X-XSRF-TOKEN')).toBe('csrf-token-value')
    expect(JSON.parse(init?.body as string)).toEqual({
      name: 'Books',
      description: 'Reading material',
    })
    expect(categoryTitles()).toEqual(['Groceries', 'Books'])
    expect(screen.getByText('Category created successfully')).toHaveClass('auth-success')
    expect(screen.queryByRole('button', { name: 'Create Category' })).toBeNull()
  })

  it('edits an existing category in place instead of appending a copy', async () => {
    const updated: CategoryResponseDto = { id: 1, name: 'Food', description: 'Groceries and dining' }
    await renderLoaded((_url, init) =>
      init?.method === 'PUT' ? jsonResponse(200, updated) : jsonResponse(200, [GROCERIES, TRAVEL]),
    )

    fireEvent.click(screen.getByRole('button', { name: 'Edit Groceries' }))
    expect(screen.getByLabelText('Category Name:')).toHaveValue('Groceries')
    expect(screen.getByLabelText('Description:')).toHaveValue('Weekly food shopping')

    fillForm('Food', 'Groceries and dining')
    await submit('Update Category')

    const [url, init] = lastRequest()
    expect(url).toBe('/api/categories/1')
    expect(init?.method).toBe('PUT')
    expect(JSON.parse(init?.body as string)).toEqual({
      name: 'Food',
      description: 'Groceries and dining',
    })
    expect(categoryTitles()).toEqual(['Food', 'Travel'])
    expect(screen.getByText('Category updated successfully')).toHaveClass('auth-success')
  })

  it('rejects a blank submission without calling the API', async () => {
    await renderLoaded(() => jsonResponse(200, [GROCERIES]))
    const callsBefore = fetchCalls().length

    openCreateForm()
    fillForm('   ', '')
    await submit('Create Category')

    expect(fetchCalls()).toHaveLength(callsBefore)
    expect(screen.getByText('Both name and description are required')).toHaveClass('auth-error')
    expect(screen.getByRole('button', { name: 'Create Category' })).toBeInTheDocument()
  })

  it('places 400 field errors beside their fields using the Java property keys', async () => {
    await renderLoaded((_url, init) =>
      init?.method === 'POST'
        ? errorResponse(
            400,
            'Validation failed',
            {
              name: 'Name must be at most 50 characters',
              description: 'Description must be at most 255 characters',
            },
            '/api/categories',
          )
        : jsonResponse(200, []),
    )

    openCreateForm()
    fillForm('a very long name', 'a very long description')
    await submit('Create Category')

    expect(screen.getByText('Name must be at most 50 characters')).toHaveClass('field-error')
    expect(screen.getByText('Description must be at most 255 characters')).toHaveClass('field-error')
    expect(screen.getByLabelText('Category Name:')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Description:')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Validation failed')).toHaveClass('auth-error')
    expect(categoryTitles()).toEqual([])
    expect(screen.getByRole('button', { name: 'Create Category' })).toBeInTheDocument()
  })

  it('deletes a confirmed category and drops only that card', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    await renderLoaded((_url, init) =>
      init?.method === 'DELETE' ? emptyResponse(204) : jsonResponse(200, [GROCERIES, TRAVEL]),
    )

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Delete Travel' }))
    })

    const [url, init] = lastRequest()
    expect(url).toBe('/api/categories/2')
    expect(init?.method).toBe('DELETE')
    expect(categoryTitles()).toEqual(['Groceries'])
    expect(screen.getByText('Category deleted successfully')).toHaveClass('auth-success')
  })

  it('keeps the card when the delete fails', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    await renderLoaded((_url, init) =>
      init?.method === 'DELETE'
        ? errorResponse(404, 'Category not found', undefined, '/api/categories/2')
        : jsonResponse(200, [GROCERIES, TRAVEL]),
    )

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Delete Travel' }))
    })

    expect(screen.getByText('Category not found')).toHaveClass('auth-error')
    expect(categoryTitles()).toEqual(['Groceries', 'Travel'])
  })

  it('does not call the API when the delete confirmation is dismissed', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    await renderLoaded(() => jsonResponse(200, [GROCERIES]))
    const callsBefore = fetchCalls().length

    await act(async () => {
      fireEvent.click(screen.getByRole('button', { name: 'Delete Groceries' }))
    })

    expect(fetchCalls()).toHaveLength(callsBefore)
    expect(categoryTitles()).toEqual(['Groceries'])
  })
})
