import { ApiError, isAbortError } from './ApiError'

const CSRF_COOKIE_NAME = 'XSRF-TOKEN'
const CSRF_HEADER_NAME = 'X-XSRF-TOKEN'

const CSRF_PROTECTED_METHODS = new Set(['POST', 'PUT', 'PATCH', 'DELETE'])

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export interface RequestOptions {
  body?: unknown
  signal?: AbortSignal
  skipUnauthorizedHandler?: boolean
}

type UnauthorizedHandler = () => void

let unauthorizedHandler: UnauthorizedHandler | null = null

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler
}

export function readCookie(name: string): string | null {
  if (typeof document === 'undefined' || !document.cookie) {
    return null
  }
  for (const entry of document.cookie.split(';')) {
    const separator = entry.indexOf('=')
    if (separator === -1) {
      continue
    }
    if (entry.slice(0, separator).trim() === name) {
      return decodeURIComponent(entry.slice(separator + 1).trim())
    }
  }
  return null
}

async function readPayload(response: Response): Promise<unknown> {
  const raw = await response.text()
  if (raw.length === 0) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export async function request<T>(
  method: HttpMethod,
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const headers = new Headers({ Accept: 'application/json' })
  const hasBody = options.body !== undefined

  if (hasBody) {
    headers.set('Content-Type', 'application/json')
  }

  if (CSRF_PROTECTED_METHODS.has(method)) {
    const csrfToken = readCookie(CSRF_COOKIE_NAME)
    if (csrfToken !== null) {
      headers.set(CSRF_HEADER_NAME, csrfToken)
    }
  }

  let response: Response
  try {
    response = await fetch(path, {
      method,
      headers,
      credentials: 'include',
      signal: options.signal,
      body: hasBody ? JSON.stringify(options.body) : undefined,
    })
  } catch (cause) {
    if (isAbortError(cause)) {
      throw cause
    }
    throw ApiError.network()
  }

  const payload = await readPayload(response)

  if (!response.ok) {
    if (response.status === 401 && options.skipUnauthorizedHandler !== true) {
      unauthorizedHandler?.()
    }
    throw ApiError.fromResponse(response.status, payload)
  }

  return payload as T
}

export const api = {
  get: <T>(path: string, options?: RequestOptions) => request<T>('GET', path, options),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('POST', path, { ...options, body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>('PUT', path, { ...options, body }),
  delete: <T>(path: string, options?: RequestOptions) => request<T>('DELETE', path, options),
}
