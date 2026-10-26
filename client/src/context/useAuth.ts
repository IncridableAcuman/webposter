import { useContext } from 'react'
import { AuthContext } from './authContextDef.ts'
import type { AuthContextType } from './authContextDef.ts'

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
