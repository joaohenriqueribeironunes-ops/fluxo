"""Gera as faixas de áudio em WAV com a biblioteca padrão — sem síntese em tempo real.

O app web sintetizava as ondas com Web Audio. Em Python/Android o caminho estável é
pré-renderizar loops sem costura e deixá-los em loop pelo player, com o mesmo conteúdo
espectral (senóides duro-panorâmicas L/R e ruído marrom/branco).
"""

import array
import math
import os
import random
import struct
import wave

SR = 22050
SEED = 7


def _write(path, frames, channels):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    pcm = array.array("h")
    pcm.fromlist([max(-32768, min(32767, int(round(v * 32767)))) for v in frames])
    with wave.open(path, "wb") as out:
        out.setnchannels(channels)
        out.setsampwidth(2)
        out.setframerate(SR)
        out.writeframes(pcm.tobytes())


def _loopable(samples, fade=0.05):
    """Cruza as bordas para o loop não estalar."""
    n = int(SR * fade)
    out = list(samples)
    head = out[:n]
    tail = out[-n:]
    for i in range(n):
        k = (i + 1) / (n + 1)
        out[-n + i] = tail[i] * (1 - k) + head[i] * k
        out[i] = head[i] * k + tail[i] * (1 - k)
    return out


def binaural(path, carrier, beat, seconds=6.0, amp=0.25):
    total = int(SR * seconds)
    left = [math.sin(2 * math.pi * carrier * i / SR) for i in range(total)]
    right = [math.sin(2 * math.pi * (carrier + beat) * i / SR) for i in range(total)]
    joined = []
    for l, r in zip(left, right):
        joined.append(l * amp)
        joined.append(r * amp)
    _write(path, joined, 2)
    return total


def noise(path, kind, seconds=6.0, amp=0.3):
    rnd = random.Random(SEED)
    total = int(SR * seconds)
    out = []
    last = 0.0
    for _ in range(total):
        white = rnd.uniform(-1, 1)
        if kind == "brown":
            last = (last + 0.02 * white) / 1.02
            out.append(last * 3.5 * amp)
        else:
            out.append(white * amp)
    _write(path, _loopable(out), 1)
    return total


def blip(path, freq, ms=260, amp=0.35):
    total = int(SR * ms / 1000)
    attack = int(SR * 0.008)
    out = []
    for i in range(total):
        env = min(1.0, i / max(1, attack)) if i < attack else math.exp(-3.0 * (i - attack) / total)
        out.append(math.sin(2 * math.pi * freq * i / SR) * env * amp)
    _write(path, out, 1)
    return total


def generate_all(out_dir, waves, pads):
    made = []
    for w in waves:
        p = os.path.join(out_dir, f"wave-{w.id}.wav")
        binaural(p, w.carrier, w.beat)
        made.append(p)
    for n in ("brown", "white"):
        p = os.path.join(out_dir, f"noise-{n}.wav")
        noise(p, n)
        made.append(p)
    for pad in pads:
        p = os.path.join(out_dir, f"pad-{pad['id']}.wav")
        blip(p, pad["freq"])
        made.append(p)
    alarm = os.path.join(out_dir, "alarm.wav")
    blip(alarm, 880, 600, 0.4)
    made.append(alarm)
    return made


if __name__ == "__main__":
    raise SystemExit("use tools/gen_audio.py")
