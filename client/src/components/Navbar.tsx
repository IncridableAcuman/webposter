import React from 'react'
import { useAuth } from '../context/useAuth.ts'
import { LogOut, Sparkles, User as UserIcon } from 'lucide-react'

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth()

  return (
    <header className="sticky top-0 z-30 w-full backdrop-blur-xl bg-slate-900/80 border-b border-slate-800/80">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 via-indigo-500 to-cyan-400 flex items-center justify-center shadow-lg shadow-indigo-500/20 ring-1 ring-white/20">
            <Sparkles className="w-5 h-5 text-white" />
          </div>
          <div>
            <span className="text-lg font-bold bg-gradient-to-r from-white via-slate-200 to-indigo-300 bg-clip-text text-transparent tracking-tight">
              TaskFlow
            </span>
            <span className="hidden sm:inline-block ml-2 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              JWT Secured
            </span>
          </div>
        </div>

        {/* User profile & actions */}
        <div className="flex items-center gap-4">
          <div className="hidden sm:flex items-center gap-2.5 px-3 py-1.5 rounded-lg bg-slate-800/60 border border-slate-700/60">
            <div className="w-7 h-7 rounded-full bg-indigo-500/20 border border-indigo-500/30 flex items-center justify-center text-indigo-300">
              <UserIcon className="w-3.5 h-3.5" />
            </div>
            <div className="flex flex-col text-left">
              <span className="text-xs font-semibold text-slate-200">
                {user?.username || user?.email?.split('@')[0]}
              </span>
              <span className="text-[10px] text-slate-400 truncate max-w-[140px]">
                {user?.email}
              </span>
            </div>
          </div>

          <button
            onClick={logout}
            className="flex items-center gap-2 px-3.5 py-2 text-xs font-medium text-slate-300 hover:text-white bg-slate-800/80 hover:bg-rose-500/10 hover:border-rose-500/30 border border-slate-700/80 rounded-lg transition-all duration-200 cursor-pointer shadow-sm active:scale-95"
            title="Chiqish"
          >
            <LogOut className="w-3.5 h-3.5 text-slate-400 group-hover:text-rose-400" />
            <span className="hidden sm:inline">Chiqish</span>
          </button>
        </div>
      </div>
    </header>
  )
}
