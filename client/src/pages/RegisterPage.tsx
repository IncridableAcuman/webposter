import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/useAuth.ts'
import { Mail, Lock, User as UserIcon, Eye, EyeOff, Loader2, Sparkles, ArrowRight, Check } from 'lucide-react'

export const RegisterPage: React.FC = () => {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const { register } = useAuth()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    if (!username.trim() || !email.trim() || !password) {
      setErrorMessage('Iltimos, barcha maydonlarni to‘ldiring.')
      return
    }

    if (password.length < 8) {
      setErrorMessage('Parol uzunligi kamida 8 ta belgidan iborat bo‘lishi lozim.')
      return
    }

    if (password !== confirmPassword) {
      setErrorMessage('Parollar bir-biriga mos kelmadi.')
      return
    }

    try {
      setIsLoading(true)
      await register({
        username: username.trim(),
        email: email.trim(),
        password,
      })
      navigate('/')
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'response' in err) {
        const resp = (err as { response?: { data?: { message?: string } } }).response
        setErrorMessage(resp?.data?.message || 'Ro‘yxatdan o‘tishda xatolik yuz berdi.')
      } else {
        setErrorMessage('Server bilan bog‘lanishda xatolik yuz berdi.')
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col justify-center items-center px-4 sm:px-6 lg:px-8 relative overflow-hidden select-none py-12">
      {/* Background radial glows */}
      <div className="absolute top-1/4 -right-32 w-96 h-96 bg-indigo-600/15 rounded-full blur-3xl pointer-events-none" />
      <div className="absolute bottom-1/4 -left-32 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl pointer-events-none" />

      {/* Brand Header */}
      <div className="flex flex-col items-center mb-8 relative z-10 text-center">
        <div className="w-14 h-14 rounded-2xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-cyan-400 flex items-center justify-center shadow-xl shadow-indigo-500/25 ring-1 ring-white/20 mb-4">
          <Sparkles className="w-7 h-7 text-white" />
        </div>
        <h1 className="text-3xl font-extrabold text-white tracking-tight">
          Yangi hisob yaratish
        </h1>
        <p className="text-sm text-slate-400 mt-1 max-w-sm">
          TaskFlow orqali vazifalaringizni xavfsiz boshqaring
        </p>
      </div>

      {/* Card */}
      <div className="w-full max-w-md relative z-10">
        <div className="bg-slate-900/90 backdrop-blur-xl border border-slate-800 rounded-3xl p-7 sm:p-9 shadow-2xl shadow-black/60">
          <h2 className="text-xl font-bold text-slate-100 mb-1">Ro‘yxatdan o‘tish</h2>
          <p className="text-xs text-slate-400 mb-6">
            Davom etish uchun ma'lumotlaringizni kiriting
          </p>

          {errorMessage && (
            <div className="mb-5 p-3.5 rounded-xl bg-rose-500/10 border border-rose-500/30 text-rose-400 text-xs font-medium flex items-center gap-2">
              <span className="w-1.5 h-1.5 rounded-full bg-rose-400 flex-shrink-0" />
              <span>{errorMessage}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Foydalanuvchi nomi (Username)
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <UserIcon className="w-4 h-4" />
                </div>
                <input
                  type="text"
                  required
                  value={username}
                  onChange={e => setUsername(e.target.value)}
                  placeholder="izzatbek"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-xl text-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 placeholder:text-slate-600 transition"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Elektron pochta (Email)
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  placeholder="izzatbek@gmail.com"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-xl text-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 placeholder:text-slate-600 transition"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Parol
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="Kamida 8 ta belgi"
                  className="w-full pl-10 pr-11 py-2.5 bg-slate-950/80 border border-slate-800 rounded-xl text-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 placeholder:text-slate-600 transition"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-500 hover:text-slate-300 transition cursor-pointer"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              <div className="mt-1.5 flex items-center gap-1.5 text-[11px] text-slate-500">
                <span
                  className={`flex items-center gap-1 ${
                    password.length >= 8 ? 'text-emerald-400 font-medium' : 'text-slate-500'
                  }`}
                >
                  <Check className="w-3 h-3" />
                  Kamida 8 ta belgi
                </span>
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1.5">
                Parolni tasdiqlang
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={confirmPassword}
                  onChange={e => setConfirmPassword(e.target.value)}
                  placeholder="Parolni qayta kiriting"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-950/80 border border-slate-800 rounded-xl text-slate-200 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 placeholder:text-slate-600 transition"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="w-full mt-2 py-3 px-4 rounded-xl text-sm font-semibold text-white bg-gradient-to-r from-indigo-600 to-indigo-500 hover:from-indigo-500 hover:to-indigo-400 shadow-lg shadow-indigo-600/30 flex items-center justify-center gap-2 active:scale-[0.98] transition-all duration-200 cursor-pointer disabled:opacity-60"
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Yaratilmoqda...</span>
                </>
              ) : (
                <>
                  <span>Ro‘yxatdan o‘tish</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          <div className="mt-6 pt-6 border-t border-slate-800 text-center">
            <p className="text-xs text-slate-400">
              Hisobingiz bormi?{' '}
              <Link
                to="/login"
                className="font-semibold text-indigo-400 hover:text-indigo-300 transition ml-1"
              >
                Kirish
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}
