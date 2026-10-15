"""Foco: timer com anel de progresso, ondas binaurais, ruídos, repouso e compromissos."""

import flet as ft

from .. import theme, util
from ..components import appointments, rest
from ..waves import NOISES, PRESETS, WAVES

CARD_W = 196


def _state(app):
    return app.ui_state(
        "foco", target=25 * 60, left=25 * 60, running=False, wave=None, noise=None, tick=None
    )


def _toggle_run(app):
    st = _state(app)
    if st["running"]:
        app.remove_ticker(st["tick"])
        st.update(running=False, tick=None)
        app.player.stop_all()
        app.render()
        return
    if st["left"] <= 0:
        return
    st["running"] = True
    st["tick"] = app.add_ticker(1, lambda: _tick(app))
    app.render()


def _tick(app):
    st = _state(app)
    if not st["running"]:
        return
    if st["left"] <= 1:
        tick = st["tick"]
        st.update(left=0, running=False, tick=None)
        app.remove_ticker(tick)
        app.player.stop_all()
        app.player.ring_alarm()
        app.say("Ciclo concluído. Faça uma pausa.")
        return
    st["left"] -= 1
    app.render()


def _preset(app, sec):
    st = _state(app)
    if st["tick"]:
        app.remove_ticker(st["tick"])
    st.update(target=sec, left=sec, running=False, tick=None)
    app.render()


def _pick_wave(app, name):
    st = _state(app)
    if st["wave"] == name:
        app.player.stop_loop()
        st["wave"] = None
    else:
        app.player.play_loop(name)
        st["wave"] = name
        st["noise"] = None
    app.render()


def _pick_noise(app, name):
    st = _state(app)
    if st["noise"] == name:
        app.player.stop_loop()
        st["noise"] = None
    else:
        app.player.play_loop(name)
        st["noise"] = name
        st["wave"] = None
    app.render()


def _ring(app, st):
    progress = (st["left"] / st["target"]) if st["target"] else 0
    status = "EM FOCO" if st["running"] else ("CONCLUÍDO" if st["left"] == 0 else "PRONTO")
    m, s = util.mmss(st["left"]).split(":")
    return ft.Container(
        width=240,
        height=240,
        alignment=ft.Alignment(0, 0),
        content=ft.Stack(
            [
                ft.ProgressRing(
                    width=240,
                    height=240,
                    value=progress,
                    stroke_width=18,
                    color=theme.EMERALD_BRIGHT,
                    bgcolor="#17FFFFFF",
                    stroke_cap=ft.StrokeCap.ROUND,
                    track_gap=34,
                ),
                ft.Container(
                    alignment=ft.Alignment(0, 0),
                    content=ft.Column(
                        [
                            ft.Row(
                                [
                                    ft.Text(m, size=52, weight=ft.FontWeight.W_200, color=theme.FG),
                                    ft.Text(":", size=52, weight=ft.FontWeight.W_200, color=theme.DIM),
                                    ft.Text(s, size=52, weight=ft.FontWeight.W_200, color=theme.FG),
                                ],
                                spacing=2,
                                alignment=ft.MainAxisAlignment.CENTER,
                            ),
                            ft.Text(status, size=12, color=theme.DIM, style=ft.TextStyle(letter_spacing=2)),
                        ],
                        horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                        spacing=2,
                    ),
                    width=240,
                    height=240,
                ),
            ],
            alignment=ft.Alignment(0, 0),
        ),
    )


def build(app):
    st = _state(app)
    volume = app.player.volume

    timer_panel = theme.glass(
        ft.Column(
            [
                _ring(app, st),
                ft.Row(
                    [
                        theme.icon_button(
                            ft.Icons.PAUSE if st["running"] else ft.Icons.PLAY_ARROW,
                            lambda e: _toggle_run(app),
                            label="Pausar" if st["running"] else "Iniciar",
                        ),
                        theme.icon_button(
                            ft.Icons.REPLAY,
                            lambda e: _preset(app, st["target"]),
                            label="Reiniciar",
                            ghost=True,
                            glyph=18,
                        ),
                    ],
                    spacing=10,
                    alignment=ft.MainAxisAlignment.CENTER,
                ),
                theme.row_of(
                    [theme.chip(label, pressed=st["target"] == sec, on_click=lambda e, s=sec: _preset(app, s))
                     for label, sec in PRESETS]
                ),
            ],
            spacing=16,
            horizontal_alignment=ft.CrossAxisAlignment.CENTER,
            width=theme.MAX_W - 48,
        ),
        padding=32,
    )

    sound_panel = theme.panel(
        [
            ft.Row(
                [
                    ft.Icon(ft.Icons.VOLUME_UP, size=18, color=theme.DIM),
                    ft.Slider(
                        min=0,
                        max=1,
                        divisions=20,
                        value=volume,
                        expand=True,
                        active_color=theme.EMERALD,
                        inactive_color="#24FFFFFF",
                        thumb_color=theme.EMERALD_BRIGHT,
                        on_change=lambda e: app.player.set_volume(e.control.value),
                    ),
                ],
                spacing=10,
                vertical_alignment=ft.CrossAxisAlignment.CENTER,
            ),
            ft.Row(
                [
                    theme.card(
                        w.label,
                        f"{w.desc} · {w.carrier:g}Hz base, {w.beat:g}Hz de batida",
                        pressed=st["wave"] == w.id,
                        on_click=lambda e, n=w.id: _pick_wave(app, n),
                        width=CARD_W,
                    )
                    for w in WAVES
                ],
                wrap=True,
                spacing=10,
                run_spacing=10,
            ),
            ft.Row(
                [
                    theme.card(
                        n["label"],
                        n["desc"],
                        pressed=st["noise"] == n["id"],
                        on_click=lambda e, k=n["id"]: _pick_noise(app, k),
                        width=CARD_W,
                    )
                    for n in NOISES
                ],
                wrap=True,
                spacing=10,
                run_spacing=10,
            ),
            theme.note(
                "Use fones de ouvido para o efeito binaural completo. "
                "As ondas são geradas no aparelho, em assets/audio."
            ),
        ],
        title="Sons binaurais e ambientes",
    )

    return [
        ft.Row([timer_panel], width=theme.MAX_W),
        sound_panel,
        rest.build(app),
        appointments.build(app),
    ]
