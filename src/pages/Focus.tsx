import { useCallback, useEffect, useRef, useState } from 'react'
import {
  Pause,
  Play,
  RotateCcw,
  Volume2,
  Mic,
  Bell,
  BellRing,
  Wind,
  Waves,
  Lightbulb,
  ChevronRight,
  ShieldCheck,
  Plus,
  Trash2,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { loadStored, saveStored } from '@/lib/storage'
import {
  BINAURAL_PRESETS,
  NOISE_PRESETS,
  startSound,
  stopSound,
  type SoundKind,
} from '@/lib/audio'

const DURATIONS = [
  { label: 'Sprint 15', minutes: 15 },
  { label: 'Foco 25', minutes: 25 },
  { label: 'Hiperfoco 50', minutes: 50 },
  { label: 'Imersão 90', minutes: 90 },
  { label: 'Pausa 5', minutes: 5 },
  { label: 'Pausa 15', minutes: 15 },
]

/** Notifica ao final do bloco (se o usuário permitir). */
async function notify(title: string, body: string) {
  try {
    if (!('Notification' in window)) return
    let permission = Notification.permission
    if (permission === 'default') permission = await Notification.requestPermission()
    if (permission === 'granted') new Notification(title, { body })
  } catch {
    /* notificações indisponíveis */
  }
}

function FocusTimer() {
  const [total, setTotal] = useState(25 * 60)
  const [remaining, setRemaining] = useState(25 * 60)
  const [running, setRunning] = useState(false)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  useEffect(() => {
    if (!running) return
    intervalRef.current = setInterval(() => {
      setRemaining((prev) => {
        if (prev <= 1) {
          if (intervalRef.current) clearInterval(intervalRef.current)
          setRunning(false)
          void notify('Bloco concluído', 'Seu bloco de hiperfoco terminou. Respire e faça uma pausa.')
          return 0
        }
        return prev - 1
      })
    }, 1000)
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [running])

  useEffect(() => {
    const onVisibility = () => {
      if (document.hidden && running) setRunning(false)
    }
    document.addEventListener('visibilitychange', onVisibility)
    return () => document.removeEventListener('visibilitychange', onVisibility)
  }, [running])

  const pick = (minutes: number) => {
    setTotal(minutes * 60)
    setRemaining(minutes * 60)
    setRunning(false)
  }

  const toggle = () => {
    if (remaining <= 0) setRemaining(total)
    setRunning((r) => !r)
  }

  const reset = () => {
    setRunning(false)
    setRemaining(total)
  }

  const progress = total > 0 ? remaining / total : 0
  const minutes = Math.floor(remaining / 60)
  const seconds = remaining % 60

  const circumference = 2 * Math.PI * 105
  const dashoffset = circumference * (1 - progress)
  const angle = 2 * Math.PI * progress
  const dotX = 128 + 105 * Math.cos(angle)
  const dotY = 128 + 105 * Math.sin(angle)

  return (
    <div className="glass rounded-3xl p-8 flex flex-col items-center">
      <div className="relative w-64 h-64 flex items-center justify-center">
        <svg
          className="absolute inset-0 -rotate-90"
          width="256"
          height="256"
          viewBox="0 0 256 256"
          aria-hidden="true"
        >
          <defs>
            <linearGradient id="grad" x1="0" y1="0" x2="1" y2="1">
              <stop offset="0%" stopColor="#ffffff" stopOpacity="0.75" />
              <stop offset="55%" stopColor="#ffffff" stopOpacity="0.5" />
              <stop offset="100%" stopColor="#ffffff" stopOpacity="0.3" />
            </linearGradient>
            <filter id="ringGlow" x="-30%" y="-30%" width="160%" height="160%">
              <feGaussianBlur stdDeviation="1.2" result="b" />
              <feMerge>
                <feMergeNode in="b" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
          </defs>
          <circle
            cx="128"
            cy="128"
            r="105"
            fill="none"
            stroke="rgba(255,255,255,0.09)"
            strokeWidth="18"
            strokeLinecap="round"
          />
          <circle
            cx="128"
            cy="128"
            r="105"
            fill="none"
            stroke="url(#grad)"
            strokeWidth="18"
            strokeLinecap="round"
            strokeDasharray={circumference}
            strokeDashoffset={dashoffset}
            style={{ transition: 'stroke-dashoffset 1s linear', filter: 'url(#ringGlow)' }}
          />
          {remaining > 0 && (
            <circle
              cx={dotX}
              cy={dotY}
              r="6"
              fill="#ffffff"
              fillOpacity="0.85"
              style={{ filter: 'drop-shadow(0 0 3px rgba(255,255,255,0.35))' }}
            />
          )}
        </svg>
        <div className="text-center">
          <div className="text-6xl font-extralight text-foreground tabular-nums tracking-tight">
            {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
          </div>
          <div className="text-muted-foreground/60 text-xs mt-3 tracking-[0.25em] uppercase">
            {running ? 'em foco' : remaining === 0 ? 'concluído' : 'pronto'}
          </div>
        </div>
      </div>

      <div className="flex items-center gap-5 mt-8">
        <button
          onClick={toggle}
          aria-label={running ? 'Pausar' : 'Iniciar'}
          className="w-14 h-14 rounded-full glass flex items-center justify-center text-foreground hover:scale-105 transition-transform focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          {running ? <Pause size={22} /> : <Play size={22} className="ml-0.5" />}
        </button>
        <button
          onClick={reset}
          aria-label="Reiniciar"
          className="w-11 h-11 rounded-full flex items-center justify-center text-muted-foreground hover:text-foreground transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
        >
          <RotateCcw size={20} />
        </button>
      </div>

      <div className="flex flex-wrap gap-2 mt-6 justify-center">
        {DURATIONS.map(({ label, minutes: m }) => {
          const active = total === m * 60
          return (
            <button
              key={label}
              onClick={() => pick(m)}
              className={cn(
                'px-3 py-2 min-h-[44px] rounded-full text-xs font-medium transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
                active
                  ? 'text-emerald-200 bg-emerald-400/15'
                  : 'glass text-muted-foreground hover:text-foreground',
              )}
            >
              {label}
            </button>
          )
        })}
      </div>
    </div>
  )
}

/** Sons binaurais + ambientes, com volume. */
function SoundScape() {
  const [volume, setVolume] = useState(0.5)
  const [binaural, setBinaural] = useState<string | null>(null)
  const [ambient, setAmbient] = useState<string | null>(null)

  useEffect(() => () => stopSound(), [])

  const apply = useCallback((bin: string | null, amb: string | null, vol: number) => {
    if (!bin && !amb) {
      stopSound()
      return
    }
    const b = bin ? BINAURAL_PRESETS[bin] : null
    const a = amb ? NOISE_PRESETS[amb] : null
    startSound({
      base: b ? b.base : a ? (amb === 'brown' ? 200 : 300) : 200,
      beat: b ? b.beat : 0,
      kind: (b ? 'binaural' : 'noise') as SoundKind,
      volume: vol,
    })
  }, [])

  const toggleBinaural = (key: string) => {
    const next = binaural === key ? null : key
    setBinaural(next)
    apply(next, ambient, volume)
  }

  const toggleAmbient = (key: string) => {
    const next = ambient === key ? null : key
    setAmbient(next)
    apply(binaural, next, volume)
  }

  const onVolume = (v: number) => {
    setVolume(v)
    apply(binaural, ambient, v)
  }

  return (
    <div className="glass rounded-3xl p-5">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Volume2 size={18} className="text-emerald-200" aria-hidden="true" />
          <h3 className="text-foreground/90 font-medium">Sons binaurais e ambientes</h3>
        </div>
      </div>

      <div className="flex items-center gap-3 mb-4">
        <Volume2 size={16} className="text-muted-foreground shrink-0" aria-hidden="true" />
        <input
          type="range"
          min={0}
          max={1}
          step={0.01}
          value={volume}
          aria-label="Volume"
          onChange={(e) => onVolume(parseFloat(e.target.value))}
          className="w-full h-1.5 rounded-full bg-white/10 accent-emerald-300"
        />
      </div>

      <div className="grid grid-cols-2 gap-2">
        {Object.entries(BINAURAL_PRESETS).map(([key, preset]) => {
          const active = binaural === key
          return (
            <button
              key={key}
              onClick={() => toggleBinaural(key)}
              aria-label={`Som binaural: ${preset.name}`}
              className={cn(
                'relative z-10 rounded-2xl p-3 text-left border transition',
                active ? 'border-white/25' : 'border-white/10 hover:border-white/25',
              )}
              style={{ backgroundImage: `linear-gradient(135deg, ${preset.color}55, ${preset.color}22)` }}
            >
              <span className="block text-foreground font-medium text-sm">{preset.name}</span>
              <span className="block text-muted-foreground text-[11px]">{preset.desc}</span>
            </button>
          )
        })}
        {Object.entries(NOISE_PRESETS).map(([key, preset]) => {
          const active = ambient === key
          return (
            <button
              key={key}
              onClick={() => toggleAmbient(key)}
              aria-label={`Som ambiente: ${preset.name}`}
              className={cn(
                'relative z-10 rounded-2xl p-3 text-left border transition',
                active ? 'border-white/25' : 'border-white/10 hover:border-white/25',
              )}
              style={{ backgroundImage: `linear-gradient(135deg, ${preset.color}55, ${preset.color}22)` }}
            >
              <span className="block text-foreground font-medium text-sm">{preset.name}</span>
              <span className="block text-muted-foreground text-[11px]">{preset.desc}</span>
            </button>
          )
        })}
      </div>

      <p className="text-muted-foreground/70 text-[11px] mt-3">
        Use fones de ouvido para o efeito binaural completo. As ondas são geradas no dispositivo.
      </p>
    </div>
  )
}

/** Alternância por microfone. */
function Alternance() {
  const [on, setOn] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [level, setLevel] = useState(0)
  const streamRef = useRef<MediaStream | null>(null)
  const rafRef = useRef<number | null>(null)

  const stop = () => {
    if (rafRef.current) cancelAnimationFrame(rafRef.current)
    rafRef.current = null
    streamRef.current?.getTracks().forEach((t) => t.stop())
    streamRef.current = null
    setLevel(0)
  }

  useEffect(() => () => stop(), [])

  const toggle = async () => {
    if (on) {
      stop()
      setOn(false)
      return
    }
    setError('')
    setLoading(true)
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
      const data = new Uint8Array(analyser.frequencyBinCount)
      const tick = () => {
        analyser.getByteFrequencyData(data)
        let sum = 0
        for (const v of data) sum += v
        setLevel(Math.min(100, Math.round((sum / data.length / 255) * 220)))
        rafRef.current = requestAnimationFrame(tick)
      }
      tick()
      setOn(true)
    } catch {
      setError('Microfone indisponível. Autorize o microfone nas permissões do app.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="glass rounded-3xl p-5">
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <h3 className="text-foreground/90 font-medium">Alternância</h3>
          <p className="text-muted-foreground text-xs mt-1">
            O microfone sente o barulho do ambiente e a voz ao redor, sem mexer no som do app — que sai normal.
          </p>
        </div>
        <button
          onClick={() => void toggle()}
          disabled={loading}
          aria-label={on ? 'Desligar Alternância' : 'Ligar Alternância'}
          className={cn(
            'w-12 h-12 shrink-0 rounded-full flex items-center justify-center transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
            on ? 'glass-chip-active text-emerald-100' : 'glass text-foreground',
          )}
        >
          <Mic size={20} aria-hidden="true" />
        </button>
      </div>

      {on && (
        <div className="mt-4 space-y-2">
          <div className="flex items-center justify-between text-[11px] text-muted-foreground">
            <span>barulho do ambiente e voz</span>
            <span>{level}%</span>
          </div>
          <div className="h-1.5 rounded-full bg-white/10 overflow-hidden">
            <div
              className="h-full rounded-full bg-emerald-300/70 transition-[width] duration-300"
              style={{ width: `${level}%` }}
            />
          </div>
          <p className="text-muted-foreground/70 text-[11px]">O som do app sai normal — só monitorando.</p>
        </div>
      )}

      {error && <p className="text-rose-300 text-xs mt-3">{error}</p>}

      <p className="text-muted-foreground/70 text-[11px] mt-3 flex items-center gap-1.5">
        <ShieldCheck size={12} aria-hidden="true" />
        Processado aqui no aparelho — nenhum áudio é enviado.
      </p>
    </div>
  )
}

const BREATH_PHASES = [
  { label: 'Inspire', seconds: 4 },
  { label: 'Segure', seconds: 4 },
  { label: 'Solte', seconds: 4 },
  { label: 'Pronto', seconds: 4 },
]

function Breathing() {
  const [running, setRunning] = useState(false)
  const [phase, setPhase] = useState(0)
  const [count, setCount] = useState(BREATH_PHASES[0].seconds)

  useEffect(() => {
    if (!running) return
    const timer = setInterval(() => {
      setCount((c) => {
        if (c <= 1) {
          const next = (phase + 1) % BREATH_PHASES.length
          setPhase(next)
          return BREATH_PHASES[next].seconds
        }
        return c - 1
      })
    }, 1000)
    return () => clearInterval(timer)
  }, [running, phase])

  return (
    <div className="flex flex-col items-center">
      <div className="w-48 h-48 flex items-center justify-center mb-4">
        <div
          className={cn(
            'w-28 h-28 rounded-full bg-emerald-400/25 border border-emerald-200/40 flex items-center justify-center transition-transform duration-1000',
            running && 'scale-125',
          )}
        >
          <div className="text-center">
            <div className="text-foreground text-sm font-medium">
              {running ? BREATH_PHASES[phase].label : 'Pronto'}
            </div>
            <div className="text-2xl font-light text-foreground tabular-nums">
              {running ? count : 4}
            </div>
          </div>
        </div>
      </div>
      <p className="text-muted-foreground text-xs mb-4">Respiração quadrada 4-4-4-4</p>
      <button
        onClick={() => {
          setRunning((r) => !r)
          setPhase(0)
          setCount(BREATH_PHASES[0].seconds)
        }}
        className="min-h-[44px] px-5 py-2.5 rounded-full bg-emerald-400 text-emerald-950 font-medium flex items-center gap-2 hover:bg-emerald-300 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        {running ? <Pause size={18} /> : <Play size={18} />}
        {running ? 'Pausar' : 'Começar'}
      </button>
    </div>
  )
}

const GROUNDING = [
  { n: '5', sense: 'Visão', prompt: 'Observe e nomeie mentalmente 5 coisas que você vê ao redor.' },
  { n: '4', sense: 'Tato', prompt: 'Sinta 4 texturas ao seu redor (roupa, mesa, pele...).' },
  { n: '3', sense: 'Audição', prompt: 'Identifique 3 sons que consegue ouvir agora.' },
  { n: '2', sense: 'Olfato', prompt: 'Perceba 2 cheiros presentes no ambiente.' },
  { n: '1', sense: 'Gosto', prompt: 'Note 1 sabor na boca ou tome um gole de água.' },
]

function Grounding() {
  const [step, setStep] = useState(0)
  const current = GROUNDING[step]

  return (
    <div className="flex flex-col items-center text-center">
      <div className="w-14 h-14 rounded-full bg-emerald-400/20 border border-emerald-200/40 flex items-center justify-center mb-4">
        <Wind size={26} className="text-emerald-200" aria-hidden="true" />
      </div>
      <p className="text-foreground text-sm font-medium">
        {current.n} · {current.sense}
      </p>
      <p className="text-muted-foreground text-sm mb-6 max-w-xs">{current.prompt}</p>
      <button
        onClick={() => setStep((s) => (s + 1) % GROUNDING.length)}
        className="min-h-[44px] px-5 py-2.5 rounded-full bg-emerald-400 text-emerald-950 font-medium flex items-center gap-2 hover:bg-emerald-300 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        {step < GROUNDING.length - 1 ? 'Próximo' : 'Concluir'}
        <ChevronRight size={18} aria-hidden="true" />
      </button>
    </div>
  )
}

const TIPS = [
  { title: 'Reduza a luz', body: 'Diminua o brilho da tela e das luzes do ambiente por alguns minutos.' },
  { title: 'Som ambiente', body: 'Use os sons binaurais ou ruídos do app para mascarar estímulos externos.' },
  { title: 'Modo visual suave', body: 'Ative o modo Névoa ou Ensolarado em Ajustes para suavizar a tela.' },
  { title: 'Movimento regulatório', body: 'Balance as pernas, caminhe um pouco ou aperte algo macio nas mãos.' },
  { title: 'Pressão profunda', body: 'Cruze os braços sobre o peito, use um peso no colo ou peça um abraço apertado.' },
  { title: 'Pausa de 1 minuto', body: 'Faça a respiração guiada por um ciclo para reiniciar o foco.' },
]

/** Repouso sensorial com abas: Respiração, Ancoragem, Dicas. */
function SensoryRest() {
  const [tab, setTab] = useState<'breath' | 'ground' | 'tips'>('breath')
  const TABS = [
    { id: 'breath' as const, label: 'Respiração', icon: Wind },
    { id: 'ground' as const, label: 'Ancoragem', icon: Waves },
    { id: 'tips' as const, label: 'Dicas', icon: Lightbulb },
  ]

  return (
    <div className="glass rounded-3xl p-5">
      <h3 className="text-foreground/90 font-medium mb-1">Repouso sensorial</h3>
      <p className="text-muted-foreground text-xs mb-4">
        Exercícios curtos para regular o sistema sensorial e voltar ao eixo.
      </p>

      <div className="flex gap-1 mb-5 bg-white/5 rounded-full p-1">
        {TABS.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            onClick={() => setTab(id)}
            className={cn(
              'flex-1 flex items-center justify-center gap-1.5 min-h-[44px] rounded-full text-xs font-medium transition',
              tab === id ? 'glass-chip-active text-emerald-100' : 'text-muted-foreground',
            )}
          >
            <Icon size={14} aria-hidden="true" />
            {label}
          </button>
        ))}
      </div>

      {tab === 'breath' && <Breathing />}
      {tab === 'ground' && <Grounding />}
      {tab === 'tips' && (
        <div className="space-y-3">
          {TIPS.map((tip) => (
            <div key={tip.title} className="glass rounded-2xl p-4">
              <p className="text-foreground text-sm font-medium mb-1">{tip.title}</p>
              <p className="text-muted-foreground text-xs leading-relaxed">{tip.body}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

const REMIND_OPTIONS = ['No horário', '5 min antes', '15 min antes', '30 min antes', '1 hora antes']
const APPOINTMENTS_KEY = 'fluxo_appointments'

interface Appointment {
  id: string
  title: string
  datetime: number
  remind: string
}

/** Meus compromissos — lembretes locais. */
function Appointments() {
  const [items, setItems] = useState<Appointment[]>([])
  const [title, setTitle] = useState('')
  const [datetime, setDatetime] = useState('')
  const [remind, setRemind] = useState(REMIND_OPTIONS[0])
  const [saveError, setSaveError] = useState('')

  useEffect(() => {
    setItems(loadStored<Appointment[]>(APPOINTMENTS_KEY, []))
  }, [])

  const persist = (next: Appointment[]) => {
    setItems(next)
    const res = saveStored(APPOINTMENTS_KEY, next)
    if (!res.ok) setSaveError(res.error ?? 'Não foi possível salvar.')
  }

  const add = () => {
    if (!title.trim() || !datetime) return
    persist([
      { id: `appt_${Date.now()}`, title: title.trim(), datetime: new Date(datetime).getTime(), remind },
      ...items,
    ])
    setTitle('')
    setDatetime('')
    setRemind(REMIND_OPTIONS[0])
  }

  const remove = (id: string) => persist(items.filter((i) => i.id !== id))

  return (
    <div className="glass rounded-3xl p-5">
      <div className="flex items-center gap-2 mb-3">
        <BellRing size={18} className="text-emerald-200" aria-hidden="true" />
        <h3 className="text-foreground/90 font-medium">Meus compromissos</h3>
      </div>

      {saveError && (
        <p role="alert" className="text-rose-300 text-xs mb-3">
          {saveError}
        </p>
      )}

      <div className="space-y-3">
        <input
          type="text"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          placeholder="Compromisso (ex: reunião, remédio, consulta)"
          className="w-full h-12 rounded-xl border border-border bg-transparent px-4 text-sm text-foreground placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
        />
        <input
          type="datetime-local"
          value={datetime}
          aria-label="Mostrar seletor de horário e data local"
          onChange={(e) => setDatetime(e.target.value)}
          className="w-full h-12 rounded-xl border border-border bg-transparent px-4 text-sm text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
        />
        <div className="flex items-center gap-2">
          <button
            onClick={add}
            disabled={!title.trim() || !datetime}
            className="flex-1 min-h-[44px] rounded-full bg-emerald-400 text-emerald-950 font-medium flex items-center justify-center gap-2 disabled:opacity-50 hover:bg-emerald-300 transition"
          >
            <Plus size={16} aria-hidden="true" /> Adicionar compromisso
          </button>
        </div>
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <Bell size={14} aria-hidden="true" />
          <span>Lembrar:</span>
          <select
            value={remind}
            onChange={(e) => setRemind(e.target.value)}
            aria-label="Lembrar"
            className="flex-1 h-10 rounded-lg border border-border bg-transparent px-2 text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
          >
            {REMIND_OPTIONS.map((option) => (
              <option key={option} value={option} className="bg-card">
                {option}
              </option>
            ))}
          </select>
        </div>
      </div>

      {items.length === 0 ? (
        <p className="text-muted-foreground text-xs mt-4">Nenhum compromisso. Adicione um acima.</p>
      ) : (
        <div className="space-y-2 mt-4">
          {items.map((item) => (
            <div key={item.id} className="glass rounded-2xl p-3 flex items-center justify-between gap-3">
              <div className="min-w-0">
                <p className="text-foreground text-sm font-medium truncate">{item.title}</p>
                <p className="text-muted-foreground text-[11px]">
                  {new Date(item.datetime).toLocaleString('pt-BR', {
                    day: '2-digit',
                    month: '2-digit',
                    year: 'numeric',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}{' '}
                  · {item.remind}
                </p>
              </div>
              <button
                onClick={() => remove(item.id)}
                aria-label="Excluir compromisso"
                className="p-2.5 min-h-[44px] text-foreground/40 hover:text-rose-300 transition rounded-lg shrink-0"
              >
                <Trash2 size={16} aria-hidden="true" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export function Focus() {
  return (
    <div className="space-y-5">
      <FocusTimer />
      <SoundScape />
      <Alternance />
      <SensoryRest />
      <Appointments />
    </div>
  )
}
