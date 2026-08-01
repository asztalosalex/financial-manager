import type { ErrorResponse } from '../api/types'

const REASON_PHRASES: Record<number, string> = {
  400: 'Bad Request',
  401: 'Unauthorized',
  403: 'Forbidden',
  404: 'Not Found',
  409: 'Conflict',
  415: 'Unsupported Media Type',
  500: 'Internal Server Error',
}

export function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

export function emptyResponse(status: number): Response {
  return new Response(null, { status })
}

export function errorResponse(
  status: number,
  message: string,
  fieldErrors?: Record<string, string>,
  path = '/api',
): Response {
  const body: ErrorResponse = {
    timestamp: '2026-08-01T10:15:30.000Z',
    status,
    error: REASON_PHRASES[status] ?? 'Error',
    message,
    path,
  }
  if (fieldErrors !== undefined) {
    body.fieldErrors = fieldErrors
  }
  return jsonResponse(status, body)
}

export function clearCookies(): void {
  for (const entry of document.cookie.split(';')) {
    const name = entry.split('=')[0]?.trim()
    if (name) {
      document.cookie = `${name}=; Max-Age=0; path=/`
    }
  }
}
