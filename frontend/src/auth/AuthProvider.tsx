import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { isAbortError } from '../api/ApiError'
import { setUnauthorizedHandler } from '../api/client'
import { logout as logoutRequest } from '../api/auth'
import { fetchProfile } from '../api/users'
import type { UserResponseDto } from '../api/types'
import { AuthContext, type AuthContextValue, type AuthStatus } from './AuthContext'

interface AuthState {
  status: AuthStatus
  user: UserResponseDto | null
}

const ANONYMOUS_STATE: AuthState = { status: 'anonymous', user: null }

function AuthProvider({ children }: { children: ReactNode }) {
  const navigate = useNavigate()
  const [state, setState] = useState<AuthState>({ status: 'loading', user: null })

  const loadProfile = useCallback(async (signal?: AbortSignal) => {
    try {
      const user = await fetchProfile(signal)
      setState({ status: 'authenticated', user })
    } catch (error) {
      if (isAbortError(error)) {
        return
      }
      setState(ANONYMOUS_STATE)
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    void loadProfile(controller.signal)
    return () => controller.abort()
  }, [loadProfile])

  useEffect(() => {
    setUnauthorizedHandler(() => {
      setState(ANONYMOUS_STATE)
      navigate('/login', { replace: true })
    })
    return () => setUnauthorizedHandler(null)
  }, [navigate])

  const clearSession = useCallback(() => {
    setState(ANONYMOUS_STATE)
  }, [])

  const setUser = useCallback((user: UserResponseDto) => {
    setState({ status: 'authenticated', user })
  }, [])

  const refresh = useCallback(() => loadProfile(), [loadProfile])

  const logout = useCallback(async () => {
    try {
      await logoutRequest()
    } finally {
      setState(ANONYMOUS_STATE)
      navigate('/', { replace: true })
    }
  }, [navigate])

  const value = useMemo<AuthContextValue>(
    () => ({
      status: state.status,
      user: state.user,
      isAuthenticated: state.status === 'authenticated',
      isLoading: state.status === 'loading',
      setUser,
      refresh,
      logout,
      clearSession,
    }),
    [state, setUser, refresh, logout, clearSession],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export default AuthProvider
