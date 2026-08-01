import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, request, setUnauthorizedHandler } from './client'
import { ApiError } from './ApiError'
import { clearCookies, emptyResponse, jsonResponse } from '../test/helpers'

function lastRequestInit(): RequestInit {
  const mock = vi.mocked(globalThis.fetch)
  const call = mock.mock.calls.at(-1)
  if (!call) {
    throw new Error('fetch was not called')
  }
  return call[1] as RequestInit
}

function lastHeaders(): Headers {
  return lastRequestInit().headers as Headers
}

describe('api client', () => {
  beforeEach(() => {
    clearCookies()
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    clearCookies()
  })

  it('sends every request with credentials included', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(200, { id: 1 }))

    await api.get('/api/users/profile')

    expect(lastRequestInit().credentials).toBe('include')
  })

  it('adds the XSRF-TOKEN cookie as X-XSRF-TOKEN on POST, PUT and DELETE', async () => {
    document.cookie = 'XSRF-TOKEN=csrf-abc-123'
    vi.mocked(globalThis.fetch).mockResolvedValue(emptyResponse(204))

    await api.post('/api/categories', { name: 'Food', description: 'Groceries' })
    expect(lastHeaders().get('X-XSRF-TOKEN')).toBe('csrf-abc-123')

    await api.put('/api/categories/1', { name: 'Food', description: 'Groceries' })
    expect(lastHeaders().get('X-XSRF-TOKEN')).toBe('csrf-abc-123')

    await api.delete('/api/categories/1')
    expect(lastHeaders().get('X-XSRF-TOKEN')).toBe('csrf-abc-123')
  })

  it('does not send the CSRF header on GET', async () => {
    document.cookie = 'XSRF-TOKEN=csrf-abc-123'
    vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(200, []))

    await api.get('/api/categories/user')

    expect(lastHeaders().has('X-XSRF-TOKEN')).toBe(false)
  })

  it('reads the CSRF cookie even when other cookies are present', async () => {
    document.cookie = 'other=first'
    document.cookie = 'XSRF-TOKEN=csrf-second'
    vi.mocked(globalThis.fetch).mockResolvedValue(emptyResponse(204))

    await api.post('/api/auth/logout')

    expect(lastHeaders().get('X-XSRF-TOKEN')).toBe('csrf-second')
  })

  it('omits the CSRF header when the cookie is missing', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(emptyResponse(204))

    await api.post('/api/auth/logout')

    expect(lastHeaders().has('X-XSRF-TOKEN')).toBe(false)
  })

  it('sets Content-Type application/json whenever a body is sent', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(emptyResponse(204))

    await api.post('/api/users/change-password', { currentPassword: 'a', newPassword: 'b' })

    expect(lastHeaders().get('Content-Type')).toBe('application/json')
    expect(lastRequestInit().body).toBe('{"currentPassword":"a","newPassword":"b"}')
  })

  it('does not set Content-Type when there is no body', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(emptyResponse(204))

    await api.post('/api/auth/logout')

    expect(lastHeaders().has('Content-Type')).toBe(false)
  })

  it('resolves 204 responses to null', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(emptyResponse(204))

    await expect(api.post('/api/auth/logout')).resolves.toBeNull()
  })

  it('takes the error message from the ErrorResponse body', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(
      jsonResponse(409, { status: 409, error: 'Conflict', message: 'Email already in use' }),
    )

    await expect(api.post('/api/auth/signup', {})).rejects.toMatchObject({
      status: 409,
      message: 'Email already in use',
    })
  })

  it('exposes fieldErrors from a 400 ErrorResponse', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(
      jsonResponse(400, {
        status: 400,
        message: 'Validation failed',
        fieldErrors: { email: 'Email must be a valid email address', password: 'too short' },
      }),
    )

    const error = await api.post('/api/auth/signup', {}).catch((caught: unknown) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).fieldErrors).toEqual({
      email: 'Email must be a valid email address',
      password: 'too short',
    })
    expect((error as ApiError).hasFieldErrors).toBe(true)
  })

  it('never surfaces a raw non-JSON response body as the error message', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(
      new Response('You can only update your own account', { status: 403 }),
    )

    const error = await api.put('/api/users/9', {}).catch((caught: unknown) => caught)

    expect((error as ApiError).message).toBe('This action is not allowed.')
    expect((error as ApiError).message).not.toContain('You can only update')
  })

  it('falls back to a status based message when the body has no message', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(404, { status: 404 }))

    await expect(api.get('/api/users/9')).rejects.toMatchObject({
      status: 404,
      message: 'The requested resource was not found.',
    })
  })

  it('reports transport failures as a network ApiError', async () => {
    vi.mocked(globalThis.fetch).mockRejectedValue(new TypeError('Failed to fetch'))

    const error = await api.get('/api/users/profile').catch((caught: unknown) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).isNetworkError).toBe(true)
  })

  it('rethrows abort errors untouched', async () => {
    const abortError = new DOMException('aborted', 'AbortError')
    vi.mocked(globalThis.fetch).mockRejectedValue(abortError)

    await expect(api.get('/api/users/profile')).rejects.toBe(abortError)
  })

  it('invokes the unauthorized handler on any 401', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(401, { status: 401 }))

    await expect(api.get('/api/categories/user')).rejects.toBeInstanceOf(ApiError)

    expect(handler).toHaveBeenCalledTimes(1)
  })

  it('does not invoke the unauthorized handler when the caller opts out', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(401, { status: 401 }))

    await expect(
      request('GET', '/api/users/profile', { skipUnauthorizedHandler: true }),
    ).rejects.toBeInstanceOf(ApiError)

    expect(handler).not.toHaveBeenCalled()
  })

  it('does not invoke the unauthorized handler for non-401 failures', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(403, { status: 403 }))

    await expect(api.get('/api/categories/user')).rejects.toBeInstanceOf(ApiError)

    expect(handler).not.toHaveBeenCalled()
  })
})
