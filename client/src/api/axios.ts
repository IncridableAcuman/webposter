import axios from 'axios'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'
import type { AuthResponse, User } from '../types/auth.ts'

let inMemoryToken: string | null = null
let onLogoutCallback: (() => void) | null = null

export function setAccessToken(token: string | null): void {
  inMemoryToken = token
}

export function getAccessToken(): string | null {
  return inMemoryToken
}

export function setLogoutHandler(callback: () => void): void {
  onLogoutCallback = callback
}

export function parseJwt(token: string): User | null {
  try {
    const base64Url = token.split('.')[1]
    if (!base64Url) return null
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    const parsed = JSON.parse(jsonPayload)
    return {
      id: parsed.id,
      email: parsed.sub,
      role: parsed.role,
      username: parsed.sub?.split('@')[0],
    }
  } catch {
    return null
  }
}

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor: attach Bearer token
api.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getAccessToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: unknown) => Promise.reject(error)
)

// Handling concurrent 401s during refresh
let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (err: unknown) => void
}> = []

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else if (token) {
      prom.resolve(token)
    }
  })
  failedQueue = []
}

// Response interceptor: handle 401 and auto-refresh
api.interceptors.response.use(
  response => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

    if (!originalRequest) {
      return Promise.reject(error)
    }

    const isAuthRoute =
      originalRequest.url?.includes('/auth/login') ||
      originalRequest.url?.includes('/auth/register') ||
      originalRequest.url?.includes('/auth/refresh')

    if (error.response?.status === 401 && !originalRequest._retry && !isAuthRoute) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({
            resolve: (token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              resolve(api(originalRequest))
            },
            reject: (err: unknown) => reject(err),
          })
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const { data } = await axios.post<AuthResponse>(
          `${api.defaults.baseURL}/auth/refresh`,
          {},
          { withCredentials: true }
        )

        setAccessToken(data.accessToken)
        processQueue(null, data.accessToken)

        originalRequest.headers.Authorization = `Bearer ${data.accessToken}`
        return api(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        setAccessToken(null)
        if (onLogoutCallback) {
          onLogoutCallback()
        }
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)
