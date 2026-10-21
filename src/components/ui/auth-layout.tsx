import * as React from 'react'
import { LucideIcon } from 'lucide-react'
import { FluxoLogo } from '@/components/layout'

interface AuthLayoutProps {
  icon: LucideIcon
  title: string
  subtitle?: string
  children: React.ReactNode
  footer?: React.ReactNode
}

export function AuthLayout({ icon: Icon, title, subtitle, children, footer }: AuthLayoutProps) {
  return (
    <div className="min-h-[100dvh] flex flex-col justify-center app-bg px-5 pt-[calc(env(safe-area-inset-top)+1.5rem)] pb-[calc(env(safe-area-inset-bottom)+1.5rem)]">
      <div className="w-full max-w-md mx-auto">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl glass-orb mb-4">
            <Icon className="w-7 h-7 text-foreground" aria-hidden="true" />
          </div>
          <h1 className="text-3xl font-bold tracking-tight text-foreground">{title}</h1>
          {subtitle && <p className="text-muted-foreground mt-2">{subtitle}</p>}
        </div>
        <div className="glass-strong rounded-3xl p-6 sm:p-8">{children}</div>
        {footer && <p className="text-center text-sm text-muted-foreground mt-6">{footer}</p>}
        <div className="flex justify-center mt-8">
          <FluxoLogo className="opacity-70" />
        </div>
      </div>
    </div>
  )
}
