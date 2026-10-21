import { useEffect, useState } from 'react'
import { NavLink, useLocation, Outlet } from 'react-router-dom'
import { Brain, Gamepad2, Waves, Lightbulb, Settings as SettingsIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

const NAV_ITEMS = [
  { id: 'focus', label: 'Foco', icon: Brain },
  { id: 'game', label: 'Jogo', icon: Gamepad2 },
  { id: 'frisson', label: 'Frisson', icon: Waves },
  { id: 'ideas', label: 'Ideias', icon: Lightbulb },
  { id: 'settings', label: 'Ajustes', icon: SettingsIcon },
] as const

/** Marca Fluxo, com o infinito em gradiente. */
export function FluxoLogo({ className }: { className?: string }) {
  return (
    <span
      role="img"
      aria-label="Fluxo"
      className={cn('inline-flex items-center gap-1.5 leading-none', className)}
    >
      <span className="font-heading text-[24px] font-medium leading-none tracking-[0.01em] bg-gradient-to-r from-white to-emerald-200 bg-clip-text text-transparent">
        Fluxo
      </span>
      <svg
        viewBox="0 0 24 12"
        aria-hidden="true"
        className="h-[10px] w-auto shrink-0 translate-y-[0.5px]"
      >
        <defs>
          <linearGradient id="fluxoInf" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="#ffffff" />
            <stop offset="100%" stopColor="#6ee7b7" />
          </linearGradient>
        </defs>
        <circle cx="7" cy="6" r="4.4" fill="none" stroke="url(#fluxoInf)" strokeWidth="1.6" />
        <circle cx="17" cy="6" r="4.4" fill="none" stroke="url(#fluxoInf)" strokeWidth="1.6" />
      </svg>
    </span>
  )
}

/** Dock inferior, esconde ao rolar para baixo. */
function BottomDock({ active }: { active: string }) {
  const [hidden, setHidden] = useState(false)

  useEffect(() => {
    let last = window.scrollY
    const onScroll = () => {
      const y = window.scrollY
      const delta = y - last
      if (y < 60) setHidden(false)
      else if (delta > 6) setHidden(true)
      else if (delta < -6) setHidden(false)
      last = y
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <div
      className={cn(
        'fixed bottom-0 left-0 right-0 z-50 flex justify-center px-4 transition-transform duration-300 ease-out',
        hidden && 'translate-y-[140%]',
      )}
      style={{ paddingBottom: 'calc(env(safe-area-inset-bottom) + 1.25rem)' }}
    >
      <div className="glass-dock rounded-3xl px-2 py-2 flex gap-1">
        {NAV_ITEMS.map(({ id, label, icon: Icon }) => {
          const isActive = active === id
          return (
            <NavLink
              key={id}
              to={`/${id}`}
              aria-label={label}
              className={cn(
                'flex flex-col items-center gap-1 px-3 py-3 min-h-[44px] rounded-2xl transition-all duration-300 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                isActive
                  ? 'bg-white/20 text-foreground scale-105 shadow-lg'
                  : 'text-muted-foreground hover:text-foreground',
              )}
            >
              <Icon size={22} strokeWidth={isActive ? 2.5 : 2} aria-hidden="true" />
              <span className="text-xs font-medium tracking-wide">{label}</span>
            </NavLink>
          )
        })}
      </div>
    </div>
  )
}

const PAGE_LABELS: Record<string, string> = {
  focus: 'Foco',
  game: 'Jogo',
  frisson: 'Frisson',
  ideas: 'Ideias',
  settings: 'Ajustes',
}

/** Casca do app: fundo, cabeçalho, conteúdo e dock. */
export function AppShell() {
  const location = useLocation()
  const section = location.pathname.split('/').filter(Boolean)[0] || 'focus'

  return (
    <div className="min-h-[100dvh] relative overflow-hidden app-bg">
      {/* Orbes decorativos de fundo */}
      <div className="fixed inset-0 pointer-events-none z-10 overflow-hidden">
        <div className="absolute -top-32 -left-24 w-72 h-72 rounded-full bg-emerald-400/10 blur-3xl" />
        <div className="absolute top-1/3 -right-28 w-80 h-80 rounded-full bg-teal-300/10 blur-3xl" />
      </div>

      <div className="relative z-20 pb-32 px-4 max-w-2xl mx-auto pt-[calc(env(safe-area-inset-top)+2rem)]">
        <header className="flex items-start justify-between gap-3 mb-6">
          <div className="flex items-center gap-3 flex-wrap min-w-0">
            <FluxoLogo />
            <span className="glass-chip-active rounded-full px-3 py-1 text-[12px] font-medium tracking-tight text-emerald-50">
              Diferente não é menos — é potência.
            </span>
            <p className="w-full text-muted-foreground text-xs">{PAGE_LABELS[section]}</p>
          </div>
          <div className="glass rounded-full px-3 py-1.5 text-muted-foreground text-xs font-medium shrink-0">
            TEA · TDAH
          </div>
        </header>

        <Outlet />
      </div>

      <BottomDock active={section} />
    </div>
  )
}
