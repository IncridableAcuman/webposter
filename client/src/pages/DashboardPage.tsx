import React, { useState, useEffect, useMemo } from 'react'
import { api } from '../api/axios.ts'
import type { CreateTaskPayload, Task, TaskStatus } from '../types/task.ts'
import { Navbar } from '../components/Navbar.tsx'
import { TaskModal } from '../components/TaskModal.tsx'
import {
  Plus,
  Search,
  CheckCircle2,
  Circle,
  Edit3,
  Trash2,
  Calendar,
  Sparkles,
  Loader2,
  ListTodo,
  CheckCircle,
  Clock,
} from 'lucide-react'

export const DashboardPage: React.FC = () => {
  const [tasks, setTasks] = useState<Task[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [activeFilter, setActiveFilter] = useState<'all' | 'incomplete' | 'complete'>('all')
  const [searchQuery, setSearchQuery] = useState('')
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [selectedTask, setSelectedTask] = useState<Task | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const [togglingId, setTogglingId] = useState<number | null>(null)

  useEffect(() => {
    let isSubscribed = true

    api
      .get<Task[]>('/tasks')
      .then(res => {
        if (isSubscribed) {
          setTasks(res.data)
          setIsLoading(false)
        }
      })
      .catch(() => {
        if (isSubscribed) {
          setIsLoading(false)
        }
      })

    return () => {
      isSubscribed = false
    }
  }, [])

  const handleCreateOrUpdate = async (payload: CreateTaskPayload) => {
    if (selectedTask) {
      const { data } = await api.put<Task>(`/tasks/${selectedTask.id}`, payload)
      setTasks(prev => prev.map(t => (t.id === selectedTask.id ? data : t)))
    } else {
      const { data } = await api.post<Task>('/tasks', payload)
      setTasks(prev => [data, ...prev])
    }
  }

  const handleDelete = async (id: number) => {
    if (!window.confirm('Haqiqatan ham bu taskni o‘chirmoqchimisiz?')) return

    try {
      setDeletingId(id)
      await api.delete(`/tasks/${id}`)
      setTasks(prev => prev.filter(t => t.id !== id))
    } catch {
      alert('Taskni o‘chirishda xatolik yuz berdi')
    } finally {
      setDeletingId(null)
    }
  }

  const handleToggleStatus = async (task: Task) => {
    const nextStatus: TaskStatus = task.status === 'complete' ? 'incomplete' : 'complete'
    try {
      setTogglingId(task.id)
      const { data } = await api.put<Task>(`/tasks/${task.id}`, { status: nextStatus })
      setTasks(prev => prev.map(t => (t.id === task.id ? data : t)))
    } catch {
      alert('Holatni o‘zgartirishda xatolik yuz berdi')
    } finally {
      setTogglingId(null)
    }
  }

  const filteredTasks = useMemo(() => {
    return tasks.filter(task => {
      const matchesFilter =
        activeFilter === 'all'
          ? true
          : activeFilter === 'complete'
          ? task.status === 'complete'
          : task.status === 'incomplete'

      const matchesSearch =
        task.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        (task.content && task.content.toLowerCase().includes(searchQuery.toLowerCase()))

      return matchesFilter && matchesSearch
    })
  }, [tasks, activeFilter, searchQuery])

  const stats = useMemo(() => {
    const total = tasks.length
    const complete = tasks.filter(t => t.status === 'complete').length
    const incomplete = total - complete
    return { total, complete, incomplete }
  }, [tasks])

  const formatDate = (dateString?: string) => {
    if (!dateString) return 'Vaqt belgilanmagan'
    try {
      const d = new Date(dateString)
      return d.toLocaleDateString('uz-UZ', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    } catch {
      return dateString
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Welcome & Stats Row */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur-sm flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-400">Jami Vazifalar</p>
              <h4 className="text-2xl font-extrabold text-white mt-1">{stats.total}</h4>
            </div>
            <div className="w-12 h-12 rounded-xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
              <ListTodo className="w-6 h-6" />
            </div>
          </div>

          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur-sm flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-400">Kutilayotgan (Incomplete)</p>
              <h4 className="text-2xl font-extrabold text-amber-400 mt-1">{stats.incomplete}</h4>
            </div>
            <div className="w-12 h-12 rounded-xl bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <Clock className="w-6 h-6" />
            </div>
          </div>

          <div className="bg-slate-900/60 border border-slate-800 rounded-2xl p-5 backdrop-blur-sm flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-slate-400">Bajarilgan (Complete)</p>
              <h4 className="text-2xl font-extrabold text-emerald-400 mt-1">{stats.complete}</h4>
            </div>
            <div className="w-12 h-12 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <CheckCircle className="w-6 h-6" />
            </div>
          </div>
        </div>

        {/* Action Bar: Filters, Search, Add Button */}
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 mb-6">
          {/* Filters */}
          <div className="flex items-center gap-1.5 p-1 bg-slate-900/80 border border-slate-800 rounded-xl w-fit">
            <button
              onClick={() => setActiveFilter('all')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-medium transition cursor-pointer ${
                activeFilter === 'all'
                  ? 'bg-indigo-600 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Barchasi ({stats.total})
            </button>
            <button
              onClick={() => setActiveFilter('incomplete')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-medium transition cursor-pointer ${
                activeFilter === 'incomplete'
                  ? 'bg-indigo-600 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Kutilmoqda ({stats.incomplete})
            </button>
            <button
              onClick={() => setActiveFilter('complete')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-medium transition cursor-pointer ${
                activeFilter === 'complete'
                  ? 'bg-indigo-600 text-white shadow'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              Bajarildi ({stats.complete})
            </button>
          </div>

          {/* Search & Add */}
          <div className="flex items-center gap-3">
            <div className="relative flex-1 sm:w-64">
              <Search className="w-4 h-4 absolute inset-y-0 left-3 my-auto text-slate-500 pointer-events-none" />
              <input
                type="text"
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                placeholder="Qidirish..."
                className="w-full pl-9 pr-3.5 py-2 bg-slate-900 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 placeholder:text-slate-600 transition"
              />
            </div>

            <button
              onClick={() => {
                setSelectedTask(null)
                setIsModalOpen(true)
              }}
              className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 text-white text-xs font-semibold rounded-xl shadow-lg shadow-indigo-600/25 active:scale-95 transition cursor-pointer whitespace-nowrap"
            >
              <Plus className="w-4 h-4" />
              <span>Yangi Task</span>
            </button>
          </div>
        </div>

        {/* Task List Content */}
        {isLoading ? (
          <div className="flex flex-col items-center justify-center py-20 text-slate-400">
            <Loader2 className="w-8 h-8 animate-spin text-indigo-500 mb-3" />
            <p className="text-sm">Vazifalar yuklanmoqda...</p>
          </div>
        ) : filteredTasks.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-20 px-4 text-center bg-slate-900/30 border border-dashed border-slate-800 rounded-3xl">
            <div className="w-14 h-14 rounded-2xl bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400 mb-4">
              <Sparkles className="w-7 h-7" />
            </div>
            <h3 className="text-base font-semibold text-slate-200">Tasklar topilmadi</h3>
            <p className="text-xs text-slate-500 max-w-sm mt-1 mb-5">
              {searchQuery
                ? 'Qidiruv so‘rovingiz bo‘yicha mos keluvchi vazifa mavjud emas.'
                : 'Hozircha birorta ham vazifa mavjud emas. Yangi vazifa qo‘shib boshlang!'}
            </p>
            <button
              onClick={() => {
                setSelectedTask(null)
                setIsModalOpen(true)
              }}
              className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-white text-xs font-medium rounded-xl transition cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              Yangi task qo‘shish
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredTasks.map(task => {
              const isComplete = task.status === 'complete'
              const isDeleting = deletingId === task.id
              const isToggling = togglingId === task.id

              return (
                <div
                  key={task.id}
                  className={`group relative bg-slate-900/80 border rounded-2xl p-5 flex flex-col justify-between transition-all duration-200 hover:shadow-xl hover:shadow-black/40 hover:-translate-y-0.5 ${
                    isComplete
                      ? 'border-slate-800/60 bg-slate-900/40'
                      : 'border-slate-800 hover:border-slate-700'
                  }`}
                >
                  <div>
                    {/* Header: Toggle Status & Badges */}
                    <div className="flex items-start justify-between gap-3 mb-3">
                      <button
                        onClick={() => handleToggleStatus(task)}
                        disabled={isToggling}
                        className="flex items-center gap-2 text-left group/btn cursor-pointer"
                        title={isComplete ? 'Kutilayotganga o‘tkazish' : 'Bajarildiga o‘tkazish'}
                      >
                        {isToggling ? (
                          <Loader2 className="w-5 h-5 animate-spin text-slate-500 flex-shrink-0" />
                        ) : isComplete ? (
                          <CheckCircle2 className="w-5 h-5 text-emerald-400 group-hover/btn:scale-110 transition flex-shrink-0" />
                        ) : (
                          <Circle className="w-5 h-5 text-slate-600 group-hover/btn:text-amber-400 group-hover/btn:scale-110 transition flex-shrink-0" />
                        )}
                        <span
                          className={`text-xs px-2.5 py-0.5 rounded-full font-semibold border ${
                            isComplete
                              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                              : 'bg-amber-500/10 text-amber-400 border-amber-500/20'
                          }`}
                        >
                          {isComplete ? 'Bajarildi' : 'Kutilmoqda'}
                        </span>
                      </button>

                      {/* Action buttons */}
                      <div className="flex items-center gap-1 opacity-80 group-hover:opacity-100 transition">
                        <button
                          onClick={() => {
                            setSelectedTask(task)
                            setIsModalOpen(true)
                          }}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-indigo-400 hover:bg-slate-800 transition cursor-pointer"
                          title="Tahrirlash"
                        >
                          <Edit3 className="w-3.5 h-3.5" />
                        </button>
                        <button
                          onClick={() => handleDelete(task.id)}
                          disabled={isDeleting}
                          className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition cursor-pointer disabled:opacity-50"
                          title="O‘chirish"
                        >
                          {isDeleting ? (
                            <Loader2 className="w-3.5 h-3.5 animate-spin text-rose-400" />
                          ) : (
                            <Trash2 className="w-3.5 h-3.5" />
                          )}
                        </button>
                      </div>
                    </div>

                    {/* Title */}
                    <h3
                      className={`text-sm font-semibold text-slate-100 mb-1.5 line-clamp-2 ${
                        isComplete ? 'line-through text-slate-400' : ''
                      }`}
                    >
                      {task.title}
                    </h3>

                    {/* Content */}
                    {task.content && (
                      <p
                        className={`text-xs text-slate-400 mb-4 line-clamp-3 leading-relaxed ${
                          isComplete ? 'line-through text-slate-500' : ''
                        }`}
                      >
                        {task.content}
                      </p>
                    )}
                  </div>

                  {/* Footer: Date */}
                  <div className="pt-3 border-t border-slate-800/60 flex items-center justify-between text-[11px] text-slate-500 mt-2">
                    <span className="flex items-center gap-1.5">
                      <Calendar className="w-3 h-3 text-slate-500" />
                      {formatDate(task.publishedAt)}
                    </span>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </main>

      {/* Task Modal */}
      <TaskModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false)
          setSelectedTask(null)
        }}
        onSubmit={handleCreateOrUpdate}
        initialData={selectedTask}
      />
    </div>
  )
}
