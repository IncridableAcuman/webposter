import React, { useEffect, useState, useCallback } from 'react'
import { api, parseJwt, setAccessToken, setLogoutHandler } from '../api/axios.ts'
import { AuthContext } from './authContextDef.ts'
import type { AuthResponse, LoginPayload, RegisterPayload, User } from '../types/auth.ts'

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null)
  const [token, setToken] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState<boolean>(true)

  const handleSetAuth = useCallback((accessToken: string | null) => {
    setAccessToken(accessToken)
    setToken(accessToken)
    if (accessToken) {
      const decodedUser = parseJwt(accessToken)
      setUser(decodedUser)
    } else {
      setUser(null)
    }
  }, [])

  // Silent refresh on app startup
  useEffect(() => {
    let isMounted = true

    setLogoutHandler(() => {
      handleSetAuth(null)
    })

    const restoreSession = async () => {
      try {
        const { data } = await api.post<AuthResponse>('/auth/refresh')
        if (isMounted) {
          handleSetAuth(data.accessToken)
        }
      } catch {
        if (isMounted) {
          handleSetAuth(null)
        }
      } finally {
        if (isMounted) {
          setIsLoading(false)
        }
      }
    }

    restoreSession()

    return () => {
      isMounted = false
    }
  }, [handleSetAuth])

  const login = async (payload: LoginPayload) => {
    const { data } = await api.post<AuthResponse>('/auth/login', payload)
    handleSetAuth(data.accessToken)
  }

  const register = async (payload: RegisterPayload) => {
    const { data } = await api.post<AuthResponse>('/auth/register', payload)
    handleSetAuth(data.accessToken)
  }

  const logout = async () => {
    try {
      await api.post('/auth/logout')
    } catch {
      // Ignore network errors on logout
    } finally {
      handleSetAuth(null)
    }
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        accessToken: token,
        isAuthenticated: !!token,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}
