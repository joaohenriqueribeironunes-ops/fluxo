/**
 * Motor de áudio do Fluxo.
 * Sons binaurais (batidas entre os dois ouvidos) e ruídos de mascaramento.
 * Todo o processamento acontece no aparelho — nada é enviado.
 */

export interface BinauralPreset {
  name: string
  base: number
  beat: number
  color: string
  desc: string
  kind: 'binaural'
}

export interface NoisePreset {
  name: string
  color: string
  desc: string
  kind: 'noise'
}

export const BINAURAL_PRESETS: Record<string, BinauralPreset> = {
  gamma: {
    name: 'Gama',
    base: 200,
    beat: 40,
    color: '#a78bfa',
    desc: 'Foco e alerta',
    kind: 'binaural',
  },
  beta: {
    name: 'Beta',
    base: 200,
    beat: 18,
    color: '#60a5fa',
    desc: 'Alta cognição',
    kind: 'binaural',
  },
  theta: {
    name: 'Teta',
    base: 180,
    beat: 6,
    color: '#34d399',
    desc: 'Meditação profunda',
    kind: 'binaural',
  },
  alpha: {
    name: 'Alfa',
    base: 180,
    beat: 10,
    color: '#2dd4bf',
    desc: 'Relaxamento',
    kind: 'binaural',
  },
  omega: {
    name: 'Ômega',
    base: 140,
    beat: 3,
    color: '#818cf8',
    desc: 'Expansão mental',
    kind: 'binaural',
  },
  epsilon: {
    name: 'Épsilon',
    base: 110,
    beat: 1.5,
    color: '#c084fc',
    desc: 'Repouso profundo',
    kind: 'binaural',
  },
  delta: {
    name: 'Delta',
    base: 90,
    beat: 2,
    color: '#3b82f6',
    desc: 'Sono profundo e recuperação',
    kind: 'binaural',
  },
}

export const NOISE_PRESETS: Record<string, NoisePreset> = {
  brown: {
    name: 'Ruído Marrom',
    color: '#92400e',
    desc: 'Som grave e acolhedor',
    kind: 'noise',
  },
  white: {
    name: 'Ruído Branco',
    color: '#94a3b8',
    desc: 'Som uniforme e constante',
    kind: 'noise',
  },
}

export type AnyPreset = BinauralPreset | NoisePreset

export type SoundKind = 'binaural' | 'noise'

interface EngineOptions {
  base: number
  beat: number
  kind: SoundKind
  volume?: number
}

let ctx: AudioContext | null = null
let nodes: AudioNode[] = []
let oscillators: OscillatorNode[] = []
let stopFns: Array<() => void> = []

function ensureContext(): AudioContext {
  if (!ctx) {
    const Ctor = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext
    ctx = new Ctor()
  }
  if (ctx.state === 'suspended') void ctx.resume()
  return ctx
}

/** Cria um buffer de ruído (branco ou marrom). */
function makeNoiseBuffer(audio: AudioContext, kind: 'white' | 'brown'): AudioBuffer {
  const seconds = 4
  const buffer = audio.createBuffer(1, audio.sampleRate * seconds, audio.sampleRate)
  const data = buffer.getChannelData(0)
  let last = 0
  for (let i = 0; i < data.length; i++) {
    const white = Math.random() * 2 - 1
    if (kind === 'white') {
      data[i] = white * 0.35
    } else {
      last = (last + 0.02 * white) / 1.02
      data[i] = last * 3.2
    }
  }
  return buffer
}

/** Liga o som. Retorna uma função para desligar. */
export function startSound({ base, beat, kind, volume = 0.5 }: EngineOptions): () => void {
  stopSound()
  const audio = ensureContext()
  const master = audio.createGain()
  master.gain.value = Math.max(0, Math.min(1, volume)) * 0.9
  master.connect(audio.destination)

  if (kind === 'noise') {
    const source = audio.createBufferSource()
    source.buffer = makeNoiseBuffer(audio, base < 300 ? 'brown' : 'white')
    source.loop = true
    const gain = audio.createGain()
    gain.gain.value = 0.22
    source.connect(gain).connect(master)
    source.start()
    nodes.push(master, gain)
    stopFns.push(() => {
      try {
        source.stop()
        source.disconnect()
      } catch {
        /* já parado */
      }
    })
    return stopSound
  }

  // Binaurais: mesma base levemente desafinada em cada ouvido.
  const left = audio.createOscillator()
  const right = audio.createOscillator()
  left.type = 'sine'
  right.type = 'sine'
  left.frequency.value = base
  right.frequency.value = base + beat

  const panLeft = audio.createStereoPanner()
  panLeft.pan.value = -1
  const panRight = audio.createStereoPanner()
  panRight.pan.value = 1

  const toneGain = audio.createGain()
  toneGain.gain.value = 0.12

  left.connect(panLeft).connect(toneGain)
  right.connect(panRight).connect(toneGain)
  toneGain.connect(master)

  // Filtros suavizam o tom.
  const filters: BiquadFilterNode[] = []
  let chain: AudioNode = master
  const specs = [
    { frequency: Math.min(base, 16000), Q: 1.2 },
    { frequency: Math.min(base * 3, 16000), Q: 1.8 },
  ]
  for (const spec of specs) {
    const filter = audio.createBiquadFilter()
    filter.type = 'peaking'
    filter.frequency.value = spec.frequency
    filter.Q.value = spec.Q
    filter.gain.value = 6
    chain.connect(filter)
    chain = filter
    filters.push(filter)
  }
  chain.connect(audio.destination)

  left.start()
  right.start()

  oscillators.push(left, right)
  nodes.push(master, toneGain, panLeft, panRight, ...filters)
  stopFns.push(() => {
    try {
      left.stop()
      right.stop()
      left.disconnect()
      right.disconnect()
    } catch {
      /* já parado */
    }
  })

  return stopSound
}

export function stopSound(): void {
  for (const fn of stopFns) fn()
  stopFns = []
  for (const node of nodes) {
    try {
      node.disconnect()
    } catch {
      /* já desconectado */
    }
  }
  for (const osc of oscillators) {
    try {
      osc.stop()
    } catch {
      /* já parado */
    }
  }
  nodes = []
  oscillators = []
}
