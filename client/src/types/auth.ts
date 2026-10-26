export interface User {
  id?: number
  email: string
  username?: string
  role?: string
}

export interface AuthResponse {
  accessToken: string
}

export interface RegisterPayload {
  username: string
  email: string
  password: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface ApiErrorResponse {
  status?: number
  error?: string
  message: string
  errorTime?: string
}
