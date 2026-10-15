"""Canais de áudio: loops binaurais/ruído, toques do jogo, alarme e música da biblioteca.

Os loops vêm pré-renderizados de assets/audio (tools/gen_audio.py). O app web sintetizava
em tempo real com Web Audio; em Android o caminho estável é tocar o arquivo em loop, que
tem o mesmo conteúdo espectral (senóides duro-panorâmicas L/R, ruído marrom/branco).
"""

import flet
import flet_audio as fa

# Caminho relativo à pasta de assets (Flet resolve em web, desktop e Android).
ASSETS = "audio"
NOISE_NAMES = ("brown", "white")
LOOP_NAMES = ("beta", "gama", "teta", "alfa", "omega", "epsilon", "delta") + NOISE_NAMES


def _asset_for(name):
    prefix = f"noise-{name}" if name in NOISE_NAMES else f"wave-{name}"
    return f"{ASSETS}/{prefix}.wav"


class Player:
    def __init__(self, view, refresh=None):
        self.view = view
        self.refresh = refresh or view.update
        self.volume = 0.5
        self._ready = False
        self.loops = {}
        self.pads = []
        self.playing = None
        self.music = None

        for name in LOOP_NAMES:
            self.loops[name] = self._new(_asset_for(name), loop=True)
        for i in range(4):
            self.pads.append(self._new(f"{ASSETS}/pad-{i}.wav", loop=False))
        self.alarm = self._new(f"{ASSETS}/alarm.wav", loop=False)

    def mark_ready(self):
        """Depois do primeiro render é que dá para atualizar controles isoladamente."""
        self._ready = True

    def _new(self, src, loop):
        audio = fa.Audio(
            src=src,
            volume=self.volume,
            release_mode=fa.ReleaseMode.LOOP if loop else fa.ReleaseMode.STOP,
        )
        self.view.services.append(audio)
        return audio

    def _sync(self, *audios):
        if not self._ready:
            return
        for audio in audios:
            audio.update()
        self.refresh()

    # ---------- loops (ondas e ruídos) ----------
    def play_loop(self, name):
        self.stop_loop()
        audio = self.loops[name]
        audio.volume = self.volume
        self._sync(audio)
        audio.play()
        self.playing = name

    def stop_loop(self):
        if not self.playing:
            return
        try:
            self.loops[self.playing].pause()
        except Exception:  # noqa: BLE001 - pausar o que já terminou não é erro do app
            pass
        self.playing = None

    # ---------- toques curtos ----------
    def blip(self, index):
        pad = self.pads[index]
        pad.volume = self.volume
        self._sync(pad)
        pad.play(0)

    def ring_alarm(self):
        self.alarm.volume = min(1.0, self.volume * 1.6)
        self._sync(self.alarm)
        self.alarm.play(0)

    def set_volume(self, value):
        self.volume = max(0.0, min(1.0, value))
        touched = list(self.loops.values()) + self.pads + [self.alarm]
        if self.music:
            touched.append(self.music)
        for audio in touched:
            audio.volume = self.volume
        self._sync(*touched)

    def stop_all(self):
        self.stop_loop()
        if self.music:
            try:
                self.music.pause()
            except Exception:  # noqa: BLE001
                pass

    # ---------- música da biblioteca ----------
    def play_music(self, src, rate=1.0, loop=False, on_state=None, on_position=None, on_duration=None):
        """Trocar de faixa recria o serviço: mudar só o `src` não recarrega o decodificador."""
        if self.music:
            try:
                self.music.release()
            except Exception:  # noqa: BLE001
                pass
            if self.music in self.view.services:
                self.view.services.remove(self.music)
        self.music = fa.Audio(
            src=src,
            volume=self.volume,
            playback_rate=rate,
            release_mode=fa.ReleaseMode.LOOP if loop else fa.ReleaseMode.STOP,
            on_state_change=on_state,
            on_position_change=on_position,
            on_duration_change=on_duration,
        )
        self.view.services.append(self.music)
        self._sync(self.music)
        self.music.play()
        return self.music

    def apply_music_effect(self, rate=None, loop=None):
        if not self.music:
            return
        if rate is not None:
            self.music.playback_rate = rate
        if loop is not None:
            self.music.release_mode = fa.ReleaseMode.LOOP if loop else fa.ReleaseMode.STOP
        self._sync(self.music)

    def pause_music(self):
        if self.music:
            self.music.pause()

    def resume_music(self):
        if self.music:
            self.music.resume()

    def seek_music(self, seconds):
        if self.music:
            self.music.seek(flet.Duration(seconds=int(seconds)))
