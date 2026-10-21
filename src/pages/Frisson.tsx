import { useEffect, useRef, useState } from 'react'
import {
  Play,
  Pause,
  Upload,
  Trash2,
  Music,
  Waves,
  Repeat,
  Repeat1,
  Sliders,
} from 'lucide-react'
import { cn } from '@/lib/utils'
import { BINAURAL_PRESETS } from '@/lib/audio'
import { loadStored, saveStored } from '@/lib/storage'

const LIBRARY_KEY = 'fluxo_music'
const LOOP_KEY = 'fluxo_loop_mode'

type LoopMode = 'off' | 'one' | 'all'

interface Track {
  localId: string
  title: string
  dataUrl?: string
}

const LOOP_LABELS: Record<LoopMode, string> = {
  off: 'Sem repetir',
  all: 'Repetir lista',
  one: 'Repetir uma',
}

const LOOP_ICONS: Record<LoopMode, typeof Repeat> = {
  off: Repeat,
  all: Repeat,
  one: Repeat1,
}

function loadTracks(): Track[] {
  return loadStored<Track[]>(LIBRARY_KEY, [])
}

/** Reinterpreta as frequências: aplica filtros em cascata ao áudio. */
function buildChain(ctx: AudioContext, base: number, beat: number) {
  const chain: AudioNode[] = []
  const toneGain = ctx.createGain()
  toneGain.gain.value = 0.06

  const left = ctx.createOscillator()
  const right = ctx.createOscillator()
  left.type = 'sine'
  right.type = 'sine'
  left.frequency.value = base
  right.frequency.value = base + beat

  const panLeft = ctx.createStereoPanner()
  panLeft.pan.value = -1
  const panRight = ctx.createStereoPanner()
  panRight.pan.value = 1
  left.connect(panLeft).connect(toneGain)
  right.connect(panRight).connect(toneGain)
  left.start()
  right.start()

  const filters = [
    { frequency: Math.min(base, 16000), Q: 1.2, gain: 8 },
    { frequency: Math.min(base * 3, 16000), Q: 1.8, gain: 5 },
    { frequency: Math.min(base * 6, 18000), Q: 2.2, gain: 3 },
  ].map((spec) => {
    const filter = ctx.createBiquadFilter()
    filter.type = 'peaking'
    filter.frequency.value = spec.frequency
    filter.Q.value = spec.Q
    filter.gain.value = spec.gain
    return filter
  })

  chain.push(toneGain, panLeft, panRight, ...filters)
  return { chain, oscillators: [left, right] }
}

export function Frisson() {
  const [tracks, setTracks] = useState<Track[]>([])
  const [activeId, setActiveId] = useState<string | null>(null)
  const [playing, setPlaying] = useState(false)
  const [presetKey, setPresetKey] = useState<string | null>(null)
  const [loop, setLoop] = useState<LoopMode>('off')
  const [volume, setVolume] = useState(0.7)
  const [adding, setAdding] = useState(false)
  const [reinterpreting, setReinterpreting] = useState(false)
  const [storageError, setStorageError] = useState('')

  const audioRef = useRef<HTMLAudioElement | null>(null)
  const ctxRef = useRef<AudioContext | null>(null)
  const sourceRef = useRef<MediaElementAudioSourceNode | null>(null)
  const nodesRef = useRef<AudioNode[]>([])
  const oscRef = useRef<OscillatorNode[]>([])
  const fileRef = useRef<HTMLInputElement | null>(null)

  useEffect(() => {
    setTracks(loadTracks())
    setLoop((localStorage.getItem(LOOP_KEY) as LoopMode) || 'off')
  }, [])

  // Limpa tudo ao desmontar.
  useEffect(
    () => () => {
      audioRef.current?.pause()
      oscRef.current.forEach((o) => {
        try {
          o.stop()
        } catch {
          /* já parado */
        }
      })
      nodesRef.current.forEach((n) => {
        try {
          n.disconnect()
        } catch {
          /* já desconectado */
        }
      })
    },
    [],
  )

  const persist = (next: Track[]) => {
    setTracks(next)
    const res = saveStored(LIBRARY_KEY, next)
    if (!res.ok) setStorageError(res.error ?? 'Não foi possível salvar.')
  }

  const applyPreset = (key: string | null) => {
    const audio = audioRef.current
    if (!audio) return
    setPresetKey(key)

    oscRef.current.forEach((o) => {
      try {
        o.stop()
      } catch {
        /* já parado */
      }
    })
    oscRef.current = []
    nodesRef.current.forEach((n) => {
      try {
        n.disconnect()
      } catch {
        /* já desconectado */
      }
    })
    nodesRef.current = []

    if (!key) return
    const preset = BINAURAL_PRESETS[key]
    if (!preset) return
    const ctx = ctxRef.current
    if (!ctx) return
    const { chain, oscillators } = buildChain(ctx, preset.base, preset.beat)
    chain.forEach((node) => node.connect(ctx.destination))
    oscRef.current = oscillators
    nodesRef.current = chain
  }

  const ensureGraph = (audio: HTMLAudioElement) => {
    if (ctxRef.current) return
    const Ctor =
      window.AudioContext ||
      (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    const ctx = new Ctor()
    const source = ctx.createMediaElementSource(audio)
    const gain = ctx.createGain()
    gain.gain.value = volume
    source.connect(gain).connect(ctx.destination)
    ctxRef.current = ctx
    sourceRef.current = source
    nodesRef.current.push(gain)
  }

  const playTrack = async (track: Track) => {
    if (activeId === track.localId) {
      if (playing) {
        audioRef.current?.pause()
        setPlaying(false)
      } else {
        void ctxRef.current?.resume()
        void audioRef.current?.play()
        setPlaying(true)
      }
      return
    }

    if (!track.dataUrl) {
      setActiveId(track.localId)
      setPlaying(false)
      return
    }

    if (!audioRef.current) {
      const audio = new Audio()
      // Sem crossOrigin: as faixas são data URLs locais (lidas do aparelho),
      // não vêm de outra origem. Definir 'anonymous' aqui faz o WebView
      // recusar o carregamento e o play() falha silenciosamente.
      audio.onended = () => {
        setPlaying(false)
        if (loop === 'one') {
          void audio.play()
          setPlaying(true)
        }
      }
      audioRef.current = audio
    }

    const audio = audioRef.current
    audio.src = track.dataUrl
    audio.loop = loop === 'one'
    ensureGraph(audio)
    if (ctxRef.current?.state === 'suspended') void ctxRef.current.resume()
    setActiveId(track.localId)
    try {
      await audio.play()
      setPlaying(true)
    } catch {
      setPlaying(false)
    }
  }

  const reinterpret = async () => {
    if (!activeId || !presetKey) return
    setReinterpreting(true)
    // Aplica as frequências ao que está tocando.
    applyPreset(presetKey)
    await new Promise((resolve) => setTimeout(resolve, 700))
    setReinterpreting(false)
  }

  const onFile = (file: File) => {
    setAdding(true)
    const reader = new FileReader()
    reader.onload = () => {
      const track: Track = {
        localId: `music_${Date.now()}`,
        title: file.name.replace(/\.[^.]+$/, ''),
        dataUrl: typeof reader.result === 'string' ? reader.result : undefined,
      }
      persist([track, ...tracks])
      setAdding(false)
    }
    reader.onerror = () => setAdding(false)
    reader.readAsDataURL(file)
  }

  const remove = (id: string) => {
    if (activeId === id) {
      audioRef.current?.pause()
      setActiveId(null)
      setPlaying(false)
    }
    persist(tracks.filter((t) => t.localId !== id))
  }

  const cycleLoop = () => {
    const order: LoopMode[] = ['off', 'all', 'one']
    const next = order[(order.indexOf(loop) + 1) % order.length]
    setLoop(next)
    saveStored(LOOP_KEY, next)
    if (audioRef.current) audioRef.current.loop = next === 'one'
  }

  const LoopIcon = LOOP_ICONS[loop]
  const preset = presetKey ? BINAURAL_PRESETS[presetKey] : null
  const activeTrack = tracks.find((t) => t.localId === activeId) || null

  return (
    <div className="space-y-4">
      {storageError && (
        <div
          role="alert"
          className="rounded-2xl border border-rose-400/30 bg-rose-400/10 px-4 py-3 text-sm text-rose-200"
        >
          {storageError}
        </div>
      )}

      {/* Player */}
      {activeTrack && (
        <div className="glass rounded-3xl p-5">
          <div className="flex items-center gap-4">
            <button
              onClick={() => void playTrack(activeTrack)}
              aria-label={playing ? 'Pausar' : 'Tocar'}
              className="w-14 h-14 shrink-0 rounded-full glass-orb flex items-center justify-center text-foreground disabled:opacity-40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {playing ? <Pause size={22} /> : <Play size={22} className="ml-0.5" />}
            </button>
            <div className="flex-1 min-w-0">
              <p className="text-foreground font-medium text-sm truncate">{activeTrack.title}</p>
              <p className="text-muted-foreground text-xs">
                {preset ? `Reinterpretado · ${preset.name}` : 'Original'}
              </p>
            </div>
            <button
              onClick={cycleLoop}
              aria-label={`Repetição: ${LOOP_LABELS[loop]}`}
              className="w-11 h-11 shrink-0 rounded-full glass flex items-center justify-center text-muted-foreground hover:text-foreground transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              <LoopIcon size={18} aria-hidden="true" />
            </button>
          </div>

          <div className="flex items-center gap-3 mt-4">
            <Waves size={16} className="text-muted-foreground shrink-0" aria-hidden="true" />
            <input
              type="range"
              min={0}
              max={1}
              step={0.01}
              value={volume}
              aria-label="Volume da música"
              onChange={(e) => {
                const v = parseFloat(e.target.value)
                setVolume(v)
                if (ctxRef.current) {
                  const gain = nodesRef.current.find(
                    (n): n is GainNode => n instanceof GainNode,
                  )
                  if (gain) gain.gain.value = v
                }
              }}
              className="w-full h-1.5 rounded-full bg-white/10 accent-emerald-300"
            />
          </div>

          {preset && (
            <p className="text-muted-foreground/70 text-[11px] mt-3">
              {reinterpreting
                ? 'Reinterpretando...'
                : 'Este arquivo toca direto com as frequências aplicadas.'}
            </p>
          )}
        </div>
      )}

      {/* Escolha de frequência */}
      <div className="glass rounded-3xl p-5">
        <div className="flex items-center gap-2 mb-4">
          <Sliders size={18} className="text-emerald-200" aria-hidden="true" />
          <h3 className="text-foreground/90 font-medium">Escolha uma frequência</h3>
        </div>

        {!activeTrack && (
          <p className="text-muted-foreground text-xs mb-4">Escolha uma música da biblioteca.</p>
        )}

        <div className="grid grid-cols-2 gap-2">
          <button
            onClick={() => applyPreset(null)}
            className={cn(
              'rounded-2xl p-3 text-left border transition',
              presetKey === null ? 'border-white/25 glass-chip-active' : 'border-white/10 hover:border-white/25 glass',
            )}
          >
            <span className="block text-foreground font-medium text-sm">Sem análise</span>
            <span className="block text-muted-foreground text-[11px]">Toca sem alterar</span>
          </button>
          {Object.entries(BINAURAL_PRESETS).map(([key, p]) => {
            const active = presetKey === key
            return (
              <button
                key={key}
                onClick={() => applyPreset(key)}
                aria-label={p.name}
                className={cn(
                  'relative z-10 rounded-2xl p-3 text-left border transition',
                  active ? 'border-white/25' : 'border-white/10 hover:border-white/25',
                )}
                style={{ backgroundImage: `linear-gradient(135deg, ${p.color}55, ${p.color}22)` }}
              >
                <span className="block text-foreground font-medium text-sm">{p.name}</span>
                <span className="block text-muted-foreground text-[11px]">{p.desc}</span>
              </button>
            )
          })}
        </div>

        {activeTrack && presetKey && (
          <button
            onClick={() => void reinterpret()}
            disabled={reinterpreting}
            className="w-full mt-4 min-h-[44px] rounded-full bg-emerald-400 text-emerald-950 font-medium flex items-center justify-center gap-2 disabled:opacity-50 hover:bg-emerald-300 transition"
          >
            {reinterpreting ? 'Reinterpretando...' : 'Reinterpretar e tocar'}
          </button>
        )}
      </div>

      {/* Biblioteca */}
      <div className="glass rounded-3xl p-4">
        <div className="flex items-center justify-between gap-3 mb-3">
          <div className="flex items-center gap-2">
            <Music size={18} className="text-emerald-200" aria-hidden="true" />
            <h4 className="text-foreground/90 text-sm font-medium">Sua biblioteca</h4>
          </div>
          <button
            onClick={() => fileRef.current?.click()}
            disabled={adding}
            className="min-h-[44px] px-3 rounded-xl border border-dashed border-white/20 text-foreground/80 hover:border-emerald-300/50 hover:text-foreground transition flex items-center gap-2 text-xs disabled:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <Upload size={14} aria-hidden="true" />
            {adding ? 'Adicionando...' : 'Adicionar música'}
          </button>
        </div>

        <input
          ref={fileRef}
          type="file"
          accept="audio/*"
          onChange={(e) => {
            const file = e.target.files?.[0]
            if (file) onFile(file)
            if (fileRef.current) fileRef.current.value = ''
          }}
          className="hidden"
        />

        {tracks.length === 0 ? (
          <p className="text-muted-foreground text-sm text-center py-6">
            Nenhuma música ainda. Adicione um arquivo de áudio para ouvir reimaginado na frequência.
          </p>
        ) : (
          <div className="space-y-2">
            {tracks.map((track) => {
              const isPlaying = activeId === track.localId && playing
              const isActive = activeId === track.localId
              return (
                <div
                  key={track.localId}
                  className={cn(
                    'glass rounded-2xl p-3 flex items-center gap-3',
                    isActive && 'glass-chip-active',
                  )}
                >
                  <button
                    onClick={() => void playTrack(track)}
                    aria-label={isPlaying ? 'Pausar' : 'Tocar'}
                    className="w-11 h-11 shrink-0 rounded-full glass flex items-center justify-center text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    {isPlaying ? <Pause size={18} /> : <Play size={18} className="ml-0.5" />}
                  </button>
                  <div className="flex-1 min-w-0">
                    <p className="text-foreground text-sm font-medium truncate">{track.title}</p>
                    {isActive && (
                      <p className="text-muted-foreground text-[11px]">
                        {isPlaying ? 'tocando' : 'pausado'}
                      </p>
                    )}
                  </div>
                  <button
                    onClick={() => remove(track.localId)}
                    aria-label="Remover música"
                    className="p-2.5 min-h-[44px] text-foreground/40 hover:text-rose-300 transition rounded-lg shrink-0 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    <Trash2 size={16} aria-hidden="true" />
                  </button>
                </div>
              )
            })}
          </div>
        )}
      </div>
    </div>
  )
}
