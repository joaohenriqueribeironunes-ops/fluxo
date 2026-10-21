import { useCallback, useEffect, useRef, useState } from 'react'
import { Play, RotateCcw, Trophy } from 'lucide-react'
import { cn } from '@/lib/utils'

const PADS = [
  { color: '#22c55e', glow: '#bbf7d0', freq: 329.63 },
  { color: '#ef4444', glow: '#fecaca', freq: 261.63 },
  { color: '#3b82f6', glow: '#bfdbfe', freq: 392 },
  { color: '#eab308', glow: '#fef08a', freq: 440 },
]

const BEST_KEY = 'fluxo_best'

let audioCtx: AudioContext | null = null

/** Toca um tom curto (usado pelos pads). */
function playTone(freq: number, duration = 0.4) {
  if (!audioCtx) {
    const Ctor =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    audioCtx = new Ctor()
  }
  if (audioCtx.state === 'suspended') void audioCtx.resume()

  const osc = audioCtx.createOscillator()
  const gain = audioCtx.createGain()
  osc.type = 'sine'
  osc.frequency.value = freq
  gain.gain.setValueAtTime(0, audioCtx.currentTime)
  gain.gain.linearRampToValueAtTime(0.22, audioCtx.currentTime + 0.02)
  gain.gain.linearRampToValueAtTime(0, audioCtx.currentTime + duration)
  osc.connect(gain).connect(audioCtx.destination)
  osc.start()
  osc.stop(audioCtx.currentTime + duration)
}

type Phase = 'idle' | 'watching' | 'input' | 'playing' | 'over'

export function Game() {
  const [sequence, setSequence] = useState<number[]>([])
  const [index, setIndex] = useState(0)
  const [lit, setLit] = useState<number | null>(null)
  const [watching, setWatching] = useState(false)
  const [round, setRound] = useState(0)
  const [best, setBest] = useState(0)
  const [phase, setPhase] = useState<Phase>('idle')
  const [message, setMessage] = useState('Toque em iniciar')
  const timers = useRef<Array<ReturnType<typeof setTimeout>>>([])

  const clearTimers = useCallback(() => {
    timers.current.forEach(clearTimeout)
    timers.current = []
  }, [])

  useEffect(() => () => clearTimers(), [clearTimers])

  useEffect(() => {
    const stored = parseInt(localStorage.getItem(BEST_KEY) || '0', 10)
    setBest(Number.isNaN(stored) ? 0 : stored)
  }, [])

  const showSequence = useCallback(
    async (seq: number[]) => {
      setWatching(true)
      setPhase('watching')
      setMessage('Observe a sequência')
      await new Promise<void>((resolve) => timers.current.push(setTimeout(resolve, 500)))
      for (const pad of seq) {
        setLit(pad)
        playTone(PADS[pad].freq)
        await new Promise<void>((resolve) => timers.current.push(setTimeout(resolve, 450)))
        setLit(null)
        await new Promise<void>((resolve) => timers.current.push(setTimeout(resolve, 180)))
      }
      setWatching(false)
      setPhase('input')
      setMessage('Sua vez — repita a sequência')
      setIndex(0)
    },
    [],
  )

  const start = () => {
    clearTimers()
    const seq = [Math.floor(Math.random() * 4)]
    setSequence(seq)
    setRound(0)
    setPhase('playing')
    void showSequence(seq)
  }

  const press = (pad: number) => {
    if (phase !== 'input' || watching) return
    setLit(pad)
    playTone(PADS[pad].freq)
    timers.current.push(setTimeout(() => setLit(null), 250))

    if (sequence[index] === pad) {
      const next = index + 1
      if (next === sequence.length) {
        const size = sequence.length
        setRound(size)
        if (size > best) {
          setBest(size)
          localStorage.setItem(BEST_KEY, String(size))
        }
        setPhase('playing')
        setMessage('Correto! Próxima rodada...')
        const extended = [...sequence, Math.floor(Math.random() * 4)]
        setSequence(extended)
        timers.current.push(setTimeout(() => void showSequence(extended), 900))
      } else {
        setIndex(next)
      }
    } else {
      setPhase('over')
      setMessage(`Errou! Pontuação: ${sequence.length - 1}`)
      playTone(120, 0.6)
    }
  }

  return (
    <div className="space-y-5">
      <div className="glass rounded-3xl p-6">
        <div className="flex items-center justify-between mb-5">
          <div>
            <h2 className="text-foreground font-medium">Sequência de cores</h2>
            <p className="text-muted-foreground text-xs">{message}</p>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-foreground/70">Rodada: {round}</span>
            <span className="flex items-center gap-1 text-amber-200/80">
              <Trophy size={14} aria-hidden="true" /> {best}
            </span>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3 max-w-xs mx-auto">
          {PADS.map((pad, i) => {
            const isLit = lit === i
            return (
              <button
                key={i}
                onClick={() => press(i)}
                disabled={phase !== 'input'}
                aria-label={`Pad ${i + 1}`}
                className="aspect-square rounded-2xl transition-all duration-150 disabled:cursor-default focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                style={{
                  background: isLit ? pad.glow : pad.color,
                  opacity: phase === 'input' || isLit ? 1 : 0.65,
                  boxShadow: isLit
                    ? `0 0 32px 6px ${pad.glow}, inset 0 0 20px rgba(255,255,255,0.4)`
                    : 'inset 0 -3px 12px rgba(0,0,0,0.25)',
                  transform: isLit ? 'scale(0.96)' : 'scale(1)',
                }}
              />
            )
          })}
        </div>

        <div className="flex justify-center gap-3 mt-6">
          {phase === 'idle' && (
            <button
              onClick={start}
              className="px-5 py-2.5 min-h-[44px] rounded-full bg-white text-emerald-950 font-medium flex items-center gap-2 hover:scale-105 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <Play size={18} aria-hidden="true" /> Iniciar
            </button>
          )}
          {phase !== 'idle' && (
            <button
              onClick={start}
              aria-label="Reiniciar jogo"
              className={cn(
                'px-5 py-2.5 min-h-[44px] rounded-full glass text-foreground flex items-center gap-2 transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
              )}
            >
              <RotateCcw size={18} aria-hidden="true" /> Reiniciar
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
