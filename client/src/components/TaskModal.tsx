import React, { useState } from 'react'
import type { CreateTaskPayload, Task, TaskStatus } from '../types/task.ts'
import { X, CheckCircle2, Circle, Loader2, Calendar } from 'lucide-react'

interface TaskModalProps {
  isOpen: boolean
  onClose: () => void
  onSubmit: (payload: CreateTaskPayload) => Promise<void>
  initialData?: Task | null
}

const TaskModalForm: React.FC<{
  initialData?: Task | null
  onClose: () => void
  onSubmit: (payload: CreateTaskPayload) => Promise<void>
}> = ({ initialData, onClose, onSubmit }) => {
  const getDefaultDateTime = () => {
    if (initialData?.publishedAt) {
      return initialData.publishedAt.slice(0, 16)
    }
    const now = new Date()
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
    return now.toISOString().slice(0, 16)
  }

  const [title, setTitle] = useState(initialData?.title || '')
  const [content, setContent] = useState(initialData?.content || '')
  const [status, setStatus] = useState<TaskStatus>(initialData?.status || 'incomplete')
  const [publishedAt, setPublishedAt] = useState(getDefaultDateTime)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!title.trim()) {
      setError('Sarlavha kiritilishi shart')
      return
    }

    try {
      setIsSubmitting(true)
      setError(null)
      await onSubmit({
        title: title.trim(),
        content: content.trim(),
        status,
        publishedAt: publishedAt ? `${publishedAt}:00` : undefined,
      })
      onClose()
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const resp = (err as { response?: { data?: { message?: string } } }).response
        setError(resp?.data?.message || 'Xatolik yuz berdi')
      } else {
        setError('Xatolik yuz berdi')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="relative w-full max-w-lg bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl p-6 sm:p-7 overflow-hidden z-10">
      <div className="flex items-center justify-between pb-4 border-b border-slate-800">
        <div>
          <h3 className="text-lg font-bold text-white">
            {initialData ? 'Taskni Tahrirlash' : 'Yangi Task Qo‘shish'}
          </h3>
          <p className="text-xs text-slate-400 mt-0.5">
            Task ma'lumotlarini to'ldiring va holatini belgilang
          </p>
        </div>
        <button
          onClick={onClose}
          className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition cursor-pointer"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      {error && (
        <div className="mt-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit} className="mt-5 space-y-4">
        <div>
          <label className="block text-xs font-semibold text-slate-300 mb-1.5">
            Sarlavha (Title) <span className="text-rose-400">*</span>
          </label>
          <input
            type="text"
            required
            value={title}
            onChange={e => setTitle(e.target.value)}
            placeholder="Masalan: Frontend integratsiyasini tugatish"
            className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 placeholder:text-slate-600 transition"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-300 mb-1.5">
            Tavsif (Content)
          </label>
          <textarea
            rows={3}
            value={content}
            onChange={e => setContent(e.target.value)}
            placeholder="Task haqida batafsil ma'lumot..."
            className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 placeholder:text-slate-600 transition resize-none"
          />
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5 flex items-center gap-1.5">
              <Calendar className="w-3.5 h-3.5 text-indigo-400" />
              Vaqti (Published At)
            </label>
            <input
              type="datetime-local"
              value={publishedAt}
              onChange={e => setPublishedAt(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-slate-950 border border-slate-800 rounded-xl text-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 transition [color-scheme:dark]"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 mb-1.5">
              Holati (Status)
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setStatus('incomplete')}
                className={`flex items-center justify-center gap-1.5 px-3 py-2.5 rounded-xl text-xs font-semibold border transition cursor-pointer ${
                  status === 'incomplete'
                    ? 'bg-amber-500/10 text-amber-300 border-amber-500/40 ring-1 ring-amber-500/30'
                    : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700'
                }`}
              >
                <Circle className="w-3.5 h-3.5" />
                Kutilmoqda
              </button>

              <button
                type="button"
                onClick={() => setStatus('complete')}
                className={`flex items-center justify-center gap-1.5 px-3 py-2.5 rounded-xl text-xs font-semibold border transition cursor-pointer ${
                  status === 'complete'
                    ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/40 ring-1 ring-emerald-500/30'
                    : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700'
                }`}
              >
                <CheckCircle2 className="w-3.5 h-3.5" />
                Bajarildi
              </button>
            </div>
          </div>
        </div>

        <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-800">
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            className="px-4 py-2.5 rounded-xl text-xs font-medium text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700/80 transition cursor-pointer"
          >
            Bekor qilish
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="flex items-center gap-2 px-5 py-2.5 rounded-xl text-xs font-semibold text-white bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 shadow-lg shadow-indigo-600/30 active:scale-95 transition cursor-pointer disabled:opacity-60"
          >
            {isSubmitting && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
            {initialData ? 'O‘zgarishlarni saqlash' : 'Task yaratish'}
          </button>
        </div>
      </form>
    </div>
  )
}

export const TaskModal: React.FC<TaskModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  initialData,
}) => {
  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-950/80 backdrop-blur-md transition-opacity"
        onClick={onClose}
      />

      {/* Form keyed by id to avoid effect-driven state reset */}
      <TaskModalForm
        key={initialData?.id ? `edit-${initialData.id}` : 'new-task'}
        initialData={initialData}
        onClose={onClose}
        onSubmit={onSubmit}
      />
    </div>
  )
}
