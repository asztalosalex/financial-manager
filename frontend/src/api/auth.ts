import { api } from './client'
import { ApiError } from './ApiError'
import type { LoginResponse, LoginUserDto, RegisterUserDto, UserResponseDto } from './types'

const INVALID_CREDENTIALS_MESSAGE = 'Incorrect email or password.'

export async function login(credentials: LoginUserDto): Promise<LoginResponse> {
  try {
    return await api.post<LoginResponse>('/api/auth/login', credentials, {
      skipUnauthorizedHandler: true,
    })
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      throw new ApiError(401, INVALID_CREDENTIALS_MESSAGE, {}, error.payload)
    }
    throw error
  }
}

export function signup(payload: RegisterUserDto): Promise<UserResponseDto> {
  return api.post<UserResponseDto>('/api/auth/signup', payload, {
    skipUnauthorizedHandler: true,
  })
}

const LOGOUT_MAX_ATTEMPTS = 2
const LOGOUT_RETRY_DELAY_MS = 300

function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export async function logout(): Promise<void> {
  for (let attempt = 1; attempt <= LOGOUT_MAX_ATTEMPTS; attempt += 1) {
    try {
      await api.post<void>('/api/auth/logout', undefined, { skipUnauthorizedHandler: true })
      return
    } catch (error) {
      const isRetryable = error instanceof ApiError && error.isNetworkError
      if (!isRetryable || attempt === LOGOUT_MAX_ATTEMPTS) {
        throw error
      }
      await delay(LOGOUT_RETRY_DELAY_MS)
    }
  }
}
