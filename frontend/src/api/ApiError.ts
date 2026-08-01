export const NETWORK_ERROR_STATUS = 0

const DEFAULT_MESSAGES: Record<number, string> = {
  [NETWORK_ERROR_STATUS]: 'Network error. Please check your connection and try again.',
  400: 'The submitted data is invalid. Please review the fields below.',
  401: 'Your session has expired. Please log in again.',
  403: 'This action is not allowed.',
  404: 'The requested resource was not found.',
  409: 'That value is already taken.',
  415: 'The server rejected the request format.',
  500: 'Something went wrong on the server. Please try again later.',
}

const FALLBACK_MESSAGE = 'Unexpected error. Please try again.'

function defaultMessageFor(status: number): string {
  return DEFAULT_MESSAGES[status] ?? FALLBACK_MESSAGE
}

function extractMessage(payload: unknown, status: number): string {
  if (payload !== null && typeof payload === 'object' && !Array.isArray(payload)) {
    const message = (payload as Record<string, unknown>).message
    if (typeof message === 'string' && message.trim().length > 0) {
      return message
    }
  }
  return defaultMessageFor(status)
}

function extractFieldErrors(payload: unknown): Record<string, string> {
  if (payload === null || typeof payload !== 'object' || Array.isArray(payload)) {
    return {}
  }
  const raw = (payload as Record<string, unknown>).fieldErrors
  if (raw === null || typeof raw !== 'object' || Array.isArray(raw)) {
    return {}
  }
  const result: Record<string, string> = {}
  for (const [field, message] of Object.entries(raw as Record<string, unknown>)) {
    if (typeof message === 'string') {
      result[field] = message
    }
  }
  return result
}

export class ApiError extends Error {
  readonly status: number
  readonly fieldErrors: Record<string, string>
  readonly payload: unknown

  constructor(
    status: number,
    message: string,
    fieldErrors: Record<string, string> = {},
    payload: unknown = null,
  ) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
    this.payload = payload
  }

  static fromResponse(status: number, payload: unknown): ApiError {
    return new ApiError(status, extractMessage(payload, status), extractFieldErrors(payload), payload)
  }

  static network(): ApiError {
    return new ApiError(NETWORK_ERROR_STATUS, defaultMessageFor(NETWORK_ERROR_STATUS))
  }

  get isNetworkError(): boolean {
    return this.status === NETWORK_ERROR_STATUS
  }

  get hasFieldErrors(): boolean {
    return Object.keys(this.fieldErrors).length > 0
  }
}

export function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError'
}

export function toFormError(error: unknown): { message: string; fieldErrors: Record<string, string> } {
  if (error instanceof ApiError) {
    return { message: error.message, fieldErrors: error.fieldErrors }
  }
  return { message: FALLBACK_MESSAGE, fieldErrors: {} }
}
