import { createContext } from 'react'
import type { UserResponseDto } from '../api/types'

export type AuthStatus = 'loading' | 'authenticated' | 'anonymous'

export interface AuthContextValue {
  status: AuthStatus
  user: UserResponseDto | null
  isAuthenticated: boolean
  isLoading: boolean
  setUser: (user: UserResponseDto) => void
  refresh: () => Promise<void>
  logout: () => Promise<void>
  clearSession: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
