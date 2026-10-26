export type TaskStatus = 'complete' | 'incomplete'

export interface Task {
  id: number
  title: string
  content?: string
  publishedAt?: string
  status: TaskStatus
  userId?: number
  userEmail?: string
}

export interface CreateTaskPayload {
  title: string
  content?: string
  publishedAt?: string
  status?: TaskStatus
}

export interface UpdateTaskPayload {
  title?: string
  content?: string
  publishedAt?: string
  status?: TaskStatus
}
