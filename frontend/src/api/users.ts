import { api } from './client'
import type { ChangePasswordRequestDto, UpdateProfileDto, UserResponseDto } from './types'

export function fetchProfile(signal?: AbortSignal): Promise<UserResponseDto> {
  return api.get<UserResponseDto>('/api/users/profile', {
    signal,
    skipUnauthorizedHandler: true,
  })
}

export function updateProfile(userId: number, payload: UpdateProfileDto): Promise<UserResponseDto> {
  return api.put<UserResponseDto>(`/api/users/${userId}`, payload)
}

export function deleteAccount(userId: number): Promise<void> {
  return api.delete<void>(`/api/users/${userId}`)
}

export function changePassword(payload: ChangePasswordRequestDto): Promise<void> {
  return api.post<void>('/api/users/change-password', payload)
}

export function fetchUserCount(signal?: AbortSignal): Promise<number> {
  return api.get<number>('/api/users/count', { signal, skipUnauthorizedHandler: true })
}
