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

export function logout(): Promise<void> {
  return api.post<void>('/api/auth/logout', undefined, { skipUnauthorizedHandler: true })
}
