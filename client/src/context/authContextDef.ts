import { createContext } from 'react'
import type { LoginPayload, RegisterPayload, User } from '../types/auth.ts'

export interface AuthContextType {
  user: User | null
  accessToken: string | null
  isAuthenticated: boolean
  isLoading: boolean
  login: (payload: LoginPayload) => Promise<void>
  register: (payload: RegisterPayload) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined)
