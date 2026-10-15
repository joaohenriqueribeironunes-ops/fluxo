"""Frisson: biblioteca do aparelho, minutagem, volume e frequência aplicada à música."""

import os
import shutil

import flet as ft

from .. import theme, util
from ..app import data_dir
from ..waves import REPEAT, WAVES


def _state(app):
    return app.ui_state(
        "frisson", current=None, position=0.0, duration=0.0, repeat=0, wave=None, playing=False, picked=None
    )


def _tracks_dir():
    path = os.path.join(data_dir(), "tracks")
    os.makedirs(path, exist_ok=True)
    return path


def _picker(app):
    st = _state(app)
    if st.get("picked") is not None:
        return st["picked"]
    picker = ft.FilePicker(on_result=lambda e: _on_pick(app, e))
    app.add_service(picker)
    st["picked"] = picker
    return picker


def _on_pick(app, event):
    st = _state(app)
    files = getattr(event, "files", None) or []
    saved = 0
    for f in files:
        if not getattr(f, "path", None):
            continue  # web entrega bytes; aqui só há caminho local em desktop/Android
        dest = os.path.join(_tracks_dir(), os.path.basename(f.path))
        if os.path.abspath(dest) != os.path.abspath(f.path):
            shutil.copy2(f.path, dest)
        title = os.path.splitext(f.name)[0]
        app.db.add_track(title, dest)
        saved += 1
    st["last_import"] = saved
    app.say(f"{saved} faixa(s) copiada(s) para o aparelho." if saved else "Nenhuma faixa de áudio válida selecionada.")


def _add_dialog(app):
    _picker(app).pick_files(allow_multiple=True, type=ft.FilePickerFileType.AUDIO)


def _rate_for(st):
    wave = next((w for w in WAVES if w.id == st["wave"]), None)
    return 1.0 if not wave else 1.0 + wave.beat / 440


def _play(app, track):
    st = _state(app)
    st["current"] = track
    st["position"] = 0.0
    st["duration"] = 0.0
    st["playing"] = True
    app.player.play_music(
        util.file_uri(track["path"]),
        rate=_rate_for(st),
        loop=st["repeat"] == 1,
        on_state=lambda e: _on_state(app, e),
        on_position=lambda e: _on_position(app, e),
        on_duration=lambda e: _on_duration(app, e),
    )
    app.render()


def _on_state(app, event):
    st = _state(app)
    value = str(getattr(event, "state", "")).lower()
    if "completed" in value:
        st["playing"] = False
        if st["repeat"] == 2:
            _advance(app)
        app.render()
    elif "playing" in value:
        st["playing"] = True
    elif "paused" in value:
        st["playing"] = False


def _on_position(app, event):
    st = _state(app)
    position = getattr(event, "position", None)
    seconds = position.in_seconds() if position is not None else 0.0
    if int(seconds) != int(st["position"]):
        st["position"] = seconds
        if app.tab == "frisson":
            app.render()


def _on_duration(app, event):
    st = _state(app)
    duration = getattr(event, "duration", None)
    st["duration"] = duration.in_seconds() if duration is not None else 0.0
    if app.tab == "frisson":
        app.render()


def _toggle(app):
    st = _state(app)
    if not st["current"]:
        return
    if st["playing"]:
        app.player.pause_music()
    else:
        app.player.resume_music()
    app.render()


def _advance(app):
    tracks = app.db.tracks()
    st = _state(app)
    if not tracks or not st["current"]:
        return
    index = next((i for i, t in enumerate(tracks) if t["id"] == st["current"]["id"]), 0)
    _play(app, tracks[(index + 1) % len(tracks)])


def _cycle_repeat(app):
    st = _state(app)
    st["repeat"] = (st["repeat"] + 1) % len(REPEAT)
    app.player.apply_music_effect(loop=st["repeat"] == 1)
    app.render()


def _pick_wave(app, wave_id):
    st = _state(app)
    st["wave"] = None if st["wave"] == wave_id else wave_id
    app.player.apply_music_effect(rate=_rate_for(st))
    app.render()


def _seek(app, seconds):
    st = _state(app)
    st["position"] = seconds
    app.player.seek_music(seconds)


def _remove(app, track):
    app.db.delete_track(track["id"])
    st = _state(app)
    if st["current"] and st["current"]["id"] == track["id"]:
        app.player.pause_music()
        st.update(current=None, playing=False, position=0.0, duration=0.0)
    app.render()


def build(app):
    st = _state(app)
    tracks = app.db.tracks()
    current = st["current"]

    player = theme.glass(
        ft.Row(
            [
                theme.icon_button(
                    ft.Icons.PAUSE if st["playing"] else ft.Icons.PLAY_ARROW,
                    lambda e: _toggle(app),
                    label="Tocar ou pausar",
                    ghost=current is None,
                ),
                ft.Column(
                    [
                        ft.Text(current["title"] if current else "Nada tocando", size=16, weight=ft.FontWeight.W_600, color=theme.FG),
                        ft.Text(
                            f"{util.clock(st['position'])} / {util.clock(st['duration'])}"
                            if current
                            else "Escolha uma música da biblioteca",
                            size=12.5,
                            color=theme.DIM,
                        ),
                    ],
                    spacing=2,
                    expand=True,
                ),
                theme.chip(REPEAT[st["repeat"]], pressed=st["repeat"] > 0, icon=ft.Icons.REPEAT, on_click=lambda e: _cycle_repeat(app)),
            ],
            spacing=12,
            vertical_alignment=ft.CrossAxisAlignment.CENTER,
        ),
        padding=18,
    )

    scrub = theme.panel(
        [
            ft.Slider(
                min=0,
                max=max(1.0, st["duration"]),
                value=min(st["position"], st["duration"] or 0),
                expand=True,
                disabled=current is None,
                active_color=theme.EMERALD,
                inactive_color="#24FFFFFF",
                thumb_color=theme.EMERALD_BRIGHT,
                on_change=lambda e: _seek(app, e.control.value),
            ),
            ft.Row(
                [
                    ft.Text(util.clock(st["position"]), size=12, color=theme.DIM),
                    ft.Container(expand=True),
                    ft.Text(util.clock(st["duration"]), size=12, color=theme.DIM),
                ]
            ),
            ft.Row(
                [
                    ft.Icon(ft.Icons.VOLUME_UP, size=18, color=theme.DIM),
                    ft.Slider(
                        min=0,
                        max=1,
                        divisions=20,
                        value=app.player.volume,
                        expand=True,
                        active_color=theme.EMERALD,
                        inactive_color="#24FFFFFF",
                        thumb_color=theme.EMERALD_BRIGHT,
                        on_change=lambda e: app.player.set_volume(e.control.value),
                    ),
                    ft.Text(f"{int(app.player.volume * 100)}%", size=12, color=theme.DIM),
                ],
                spacing=10,
                vertical_alignment=ft.CrossAxisAlignment.CENTER,
            ),
        ],
        title="Minutagem e volume",
        hint="Posição da música e nível da saída.",
    )

    freq = theme.panel(
        [
            theme.row_of(
                [
                    theme.chip("Sem frequência", pressed=st["wave"] is None, on_click=lambda e: _pick_wave(app, None)),
                    *[
                        theme.chip(
                            f"{w.label} {w.carrier:g}Hz · {w.beat:g}Hz",
                            pressed=st["wave"] == w.id,
                            on_click=lambda e, i=w.id: _pick_wave(app, i),
                        )
                        for w in WAVES
                    ],
                ]
            )
        ],
        title="Frequência",
        hint="A música toca mais rápida ou mais lenta na batida escolhida — o player ajusta a taxa de reprodução.",
    )

    library = theme.panel(
        [
            theme.cta("Adicionar música", lambda e: _add_dialog(app), icon=ft.Icons.ADD),
            *(
                [theme.note("Vazia. As faixas ficam no aparelho e não vão para o repositório.")]
                if not tracks
                else [
                    theme.track_row(
                        t["title"],
                        None,
                        [
                            theme.chip("Tocar", pressed=bool(current and current["id"] == t["id"]), on_click=lambda e, row=t: _play(app, row)),
                            theme.chip("Remover", on_click=lambda e, row=t: _remove(app, row)),
                        ],
                    )
                    for t in tracks
                ]
            ),
        ],
        title="Sua biblioteca",
        right=theme.note(f"{len(tracks)} faixa(s)"),
    )

    return [ft.Row([player], width=theme.MAX_W), scrub, freq, library]
