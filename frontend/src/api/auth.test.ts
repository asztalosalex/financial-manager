import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { login, logout, signup } from './auth'
import { changePassword } from './users'
import { ApiError } from './ApiError'
import { setUnauthorizedHandler } from './client'
import { emptyResponse, jsonResponse } from '../test/helpers'

describe('auth endpoints', () => {
  beforeEach(() => {
    setUnauthorizedHandler(null)
    vi.stubGlobal('fetch', vi.fn())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('maps the login 401 invalid_credentials code to a readable message', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(
      jsonResponse(401, { expiresIn: null, message: 'invalid_credentials' }),
    )

    const error = await login({ email: 'a@b.hu', password: 'x' }).catch((caught: unknown) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).message).toBe('Incorrect email or password.')
  })

  it('does not trigger the global unauthorized handler on a failed login', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    vi.mocked(globalThis.fetch).mockResolvedValue(
      jsonResponse(401, { expiresIn: null, message: 'invalid_credentials' }),
    )

    await login({ email: 'a@b.hu', password: 'x' }).catch(() => {})

    expect(handler).not.toHaveBeenCalled()
  })

  it('surfaces signup validation field errors', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(
      jsonResponse(400, {
        status: 400,
        message: 'Validation failed',
        fieldErrors: { username: 'Username must be between 3 and 50 characters' },
      }),
    )

    const error = await signup({ username: 'a', email: 'a@b.hu', password: 'secret12' }).catch(
      (caught: unknown) => caught,
    )

    expect((error as ApiError).fieldErrors.username).toBe(
      'Username must be between 3 and 50 characters',
    )
  })

  it('sends logout as a POST that tolerates an empty 204 body', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(emptyResponse(204))

    await logout()

    expect(vi.mocked(globalThis.fetch).mock.calls[0][1]?.method).toBe('POST')
  })

  it('retries logout once after a network error and resolves when the retry succeeds', async () => {
    vi.useFakeTimers()
    try {
      vi.mocked(globalThis.fetch)
        .mockRejectedValueOnce(new TypeError('Failed to fetch'))
        .mockResolvedValueOnce(emptyResponse(204))

      const result = logout()
      await vi.runAllTimersAsync()
      await result

      expect(vi.mocked(globalThis.fetch)).toHaveBeenCalledTimes(2)
    } finally {
      vi.useRealTimers()
    }
  })

  it('rejects with the original network ApiError after both logout attempts fail, waiting between attempts', async () => {
    vi.useFakeTimers()
    try {
      vi.mocked(globalThis.fetch).mockRejectedValue(new TypeError('Failed to fetch'))

      const caught = logout().catch((error: unknown) => error)
      await vi.advanceTimersByTimeAsync(299)
      expect(vi.mocked(globalThis.fetch)).toHaveBeenCalledTimes(1)

      await vi.advanceTimersByTimeAsync(1)
      const error = await caught

      expect(vi.mocked(globalThis.fetch)).toHaveBeenCalledTimes(2)
      expect(error).toBeInstanceOf(ApiError)
      expect((error as ApiError).status).toBe(0)
      expect((error as ApiError).isNetworkError).toBe(true)
    } finally {
      vi.useRealTimers()
    }
  })

  it('does not retry logout when the first attempt fails with a non-network error', async () => {
    vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(401, { message: 'Session expired' }))

    const error = await logout().catch((caught: unknown) => caught)

    expect(vi.mocked(globalThis.fetch)).toHaveBeenCalledTimes(1)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).status).toBe(401)
    expect((error as ApiError).isNetworkError).toBe(false)
  })

  it('does not retry login after a network failure', async () => {
    vi.mocked(globalThis.fetch).mockRejectedValue(new TypeError('Failed to fetch'))

    const error = await login({ email: 'a@b.hu', password: 'x' }).catch((caught: unknown) => caught)

    expect(vi.mocked(globalThis.fetch)).toHaveBeenCalledTimes(1)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).isNetworkError).toBe(true)
  })

  it('does not retry signup after a network failure', async () => {
    vi.mocked(globalThis.fetch).mockRejectedValue(new TypeError('Failed to fetch'))

    const error = await signup({ username: 'alex', email: 'a@b.hu', password: 'secret12' }).catch(
      (caught: unknown) => caught,
    )

    expect(vi.mocked(globalThis.fetch)).toHaveBeenCalledTimes(1)
    expect(error).toBeInstanceOf(ApiError)
    expect((error as ApiError).isNetworkError).toBe(true)
  })

  it('reports a wrong current password as a 400 field error on currentPassword', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    vi.mocked(globalThis.fetch).mockResolvedValue(
      jsonResponse(400, {
        status: 400,
        message: 'Validation failed',
        fieldErrors: { currentPassword: 'Current password is incorrect' },
      }),
    )

    const error = await changePassword({
      currentPassword: 'wrong',
      newPassword: 'newsecret',
    }).catch((caught: unknown) => caught)

    expect((error as ApiError).status).toBe(400)
    expect((error as ApiError).fieldErrors.currentPassword).toBe('Current password is incorrect')
    expect(handler).not.toHaveBeenCalled()
  })

  it('treats a 401 from change-password as a dead session and lets the global handler run', async () => {
    const handler = vi.fn()
    setUnauthorizedHandler(handler)
    vi.mocked(globalThis.fetch).mockResolvedValue(jsonResponse(401, { status: 401 }))

    await changePassword({ currentPassword: 'a', newPassword: 'newsecret' }).catch(() => {})

    expect(handler).toHaveBeenCalledTimes(1)
  })
})
