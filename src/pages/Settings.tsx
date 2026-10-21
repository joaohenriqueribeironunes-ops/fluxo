import { useEffect, useRef, useState } from 'react'
import { Moon, Sun, Cloud, Bell, Mic, ShieldCheck } from 'lucide-react'
import { cn } from '@/lib/utils'
import { loadStored, saveStored } from '@/lib/storage'

const THEME_KEY = 'fluxo_theme'
const ADAPTIVE_KEY = 'fluxo_adaptive'

type Theme = 'escuro' | 'nevoa' | 'ensolarado'

const THEMES: Array<{ id: Theme; label: string; desc: string; icon: typeof Moon }> = [
  { id: 'escuro', label: 'Escuro', desc: 'Contraste padrão, ideal à noite.', icon: Moon },
  { id: 'nevoa', label: 'Névoa', desc: 'Suaviza a tela com tons esverdeados.', icon: Cloud },
  { id: 'ensolarado', label: 'Ensolarado', desc: 'Mais claro e aconchegante.', icon: Sun },
]

export function Settings() {
  const [theme, setTheme] = useState<Theme>('escuro')
  const [adaptive, setAdaptive] = useState(false)
  const [micError, setMicError] = useState('')
  const [noiseLevel, setNoiseLevel] = useState(0)
  const [loadingMic, setLoadingMic] = useState(false)
  const [notifPermission, setNotifPermission] = useState<NotificationPermission>('default')

  const streamRef = useRef<MediaStream | null>(null)
  const rafRef = useRef<number | null>(null)
  const analyserRef = useRef<AnalyserNode | null>(null)

  useEffect(() => {
    setTheme(loadStored<Theme>(THEME_KEY, 'escuro'))
    setAdaptive(loadStored<string>(ADAPTIVE_KEY, 'off') === 'on')
    if ('Notification' in window) setNotifPermission(Notification.permission)
  }, [])

  const pickTheme = (next: Theme) => {
    setTheme(next)
    saveStored(THEME_KEY, next)
    document.documentElement.dataset.theme = next
  }

  const stopMic = () => {
    if (rafRef.current) cancelAnimationFrame(rafRef.current)
    rafRef.current = null
    streamRef.current?.getTracks().forEach((t) => t.stop())
    streamRef.current = null
    analyserRef.current = null
    setNoiseLevel(0)
  }

  useEffect(() => () => stopMic(), [])

  const toggleAdaptive = async () => {
    if (adaptive) {
      stopMic()
      setAdaptive(false)
      localStorage.setItem(ADAPTIVE_KEY, 'off')
      return
    }

    setMicError('')
    setLoadingMic(true)
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      streamRef.current = stream
      // Alguns WebViews só expõem o construtor com prefixo.
      const Ctor =
        window.AudioContext ||
        (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
      const ctx = new Ctor()
      const source = ctx.createMediaStreamSource(stream)
      const analyser = ctx.createAnalyser()
      analyser.fftSize = 512
      source.connect(analyser)
      analyserRef.current = analyser

      const data = new Uint8Array(analyser.frequencyBinCount)
      const tick = () => {
        analyser.getByteFrequencyData(data)
        let sum = 0
        for (const v of data) sum += v
        setNoiseLevel(Math.min(100, Math.round((sum / data.length / 255) * 220)))
        rafRef.current = requestAnimationFrame(tick)
      }
      tick()

      setAdaptive(true)
      saveStored(ADAPTIVE_KEY, 'on')
    } catch {
      // No WebView do Android o microfone exige a permissão RECORD_AUDIO do
      // próprio app (declarada no AndroidManifest). Sem ela, ou se o usuário
      // negar o diálogo, cai aqui.
      setMicError('Microfone indisponível. Autorize o microfone nas permissões do app.')
    } finally {
      setLoadingMic(false)
    }
  }

  const requestNotifications = async () => {
    if (!('Notification' in window)) return
    const permission = await Notification.requestPermission()
    setNotifPermission(permission)
  }

  return (
    <div className="space-y-5">
      {/* Modo visual */}
      <div className="glass rounded-3xl p-5">
        <h3 className="text-foreground/90 font-medium mb-1">Modo visual</h3>
        <p className="text-muted-foreground text-xs mb-4">Escolha o nível de suavidade da tela.</p>
        <div className="space-y-2">
          {THEMES.map(({ id, label, desc, icon: Icon }) => (
            <button
              key={id}
              onClick={() => pickTheme(id)}
              aria-pressed={theme === id}
              className={cn(
                'w-full flex items-start justify-between gap-3 rounded-2xl p-4 text-left transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                theme === id ? 'glass-chip-active text-emerald-50' : 'glass text-foreground',
              )}
            >
              <span className="min-w-0">
                <span className="flex items-center gap-2">
                  <Icon size={16} aria-hidden="true" />
                  <span className="font-medium text-sm">{label}</span>
                </span>
                <span className="block text-muted-foreground text-xs mt-1">{desc}</span>
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* Alternância (microfone) */}
      <div className="glass rounded-3xl p-5">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h3 className="text-foreground/90 font-medium">Alternância</h3>
            <p className="text-muted-foreground text-xs mt-1">
              O microfone sente o barulho do ambiente e a voz ao redor, sem mexer no som do app — que sai normal.
            </p>
          </div>
          <button
            onClick={() => void toggleAdaptive()}
            disabled={loadingMic}
            aria-label={adaptive ? 'Desligar Alternância' : 'Ligar Alternância'}
            className={cn(
              'w-12 h-12 shrink-0 rounded-full flex items-center justify-center transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
              adaptive ? 'glass-chip-active text-emerald-100' : 'glass text-foreground',
            )}
          >
            <Mic size={20} aria-hidden="true" />
          </button>
        </div>

        {adaptive && (
          <div className="mt-4 space-y-2">
            <div className="flex items-center justify-between text-[11px] text-muted-foreground">
              <span>barulho do ambiente e voz</span>
              <span>{noiseLevel}%</span>
            </div>
            <div className="h-1.5 rounded-full bg-white/10 overflow-hidden">
              <div
                className="h-full rounded-full bg-emerald-300/70 transition-[width] duration-300"
                style={{ width: `${noiseLevel}%` }}
              />
            </div>
            <p className="text-muted-foreground/70 text-[11px]">
              O som do app sai normal — só monitorando.
            </p>
          </div>
        )}

        {micError && <p className="text-rose-300 text-xs mt-3">{micError}</p>}

        <p className="text-muted-foreground/70 text-[11px] mt-3 flex items-center gap-1.5">
          <ShieldCheck size={12} aria-hidden="true" />
          Processado aqui no aparelho — nenhum áudio é enviado.
        </p>
      </div>

      {/* Lembretes */}
      <div className="glass rounded-3xl p-5">
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h3 className="text-foreground/90 font-medium">Lembretes</h3>
            <p className="text-muted-foreground text-xs mt-1">
              Receba um aviso quando o bloco de foco terminar.
            </p>
          </div>
          <button
            onClick={() => void requestNotifications()}
            disabled={notifPermission === 'granted'}
            aria-label="Ativar lembretes"
            className={cn(
              'w-12 h-12 shrink-0 rounded-full flex items-center justify-center transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
              notifPermission === 'granted' ? 'glass-chip-active text-emerald-100' : 'glass text-foreground',
            )}
          >
            <Bell size={20} aria-hidden="true" />
          </button>
        </div>
        <p className="text-muted-foreground/70 text-[11px] mt-3">
          {notifPermission === 'granted'
            ? 'Lembretes ativos.'
            : notifPermission === 'denied'
              ? 'Permissão bloqueada — ajuste nas configurações do navegador.'
              : 'Toque para permitir notificações.'}
        </p>
      </div>

      <p className="text-center text-muted-foreground/60 text-xs">
        Diferente não é menos — é potência.
      </p>
    </div>
  )
}
