"""Jogo da sequência: memória de cores com sons por pad."""

import random

import flet as ft

from .. import theme
from ..waves import PADS

FLASH_S = 0.4
GAP_S = 0.6


def _state(app):
    st = app.ui_state("jogo", phase="idle", round=0, lit=None, seq=[], step=0, error=False)
    if "best" not in st:
        st["best"] = app.db.get_setting("gameBest", 0)
    return st


def _flash(app, index):
    st = _state(app)
    st["lit"] = index
    app.player.blip(index)
    app.render()
    app.later(FLASH_S, lambda: _unlight(app))


def _unlight(app):
    _state(app)["lit"] = None
    app.render()


def _next_round(app):
    st = _state(app)
    st["seq"] = st["seq"] + [random.randrange(len(PADS))]
    st["step"] = 0
    st["phase"] = "show"
    st["round"] = len(st["seq"])
    app.render()

    for k, pad in enumerate(st["seq"]):
        app.later(GAP_S * (k + 1) + 0.4, lambda p=pad: _flash(app, p))
    app.later(GAP_S * len(st["seq"]) + 0.5, lambda: _await_input(app))


def _await_input(app):
    st = _state(app)
    if st["phase"] == "show":
        st["phase"] = "input"
        app.render()


def _press(app, index):
    st = _state(app)
    if st["phase"] != "input":
        return
    _flash(app, index)
    if st["seq"][st["step"]] != index:
        st["error"] = True
        st["phase"] = "over"
        if st["round"] > st["best"]:
            st["best"] = st["round"]
            app.db.set_setting("gameBest", st["round"])
        app.render()
        return
    st["step"] += 1
    if st["step"] == len(st["seq"]):
        st["phase"] = "show"
        app.later(0.7, lambda: _next_round(app))
        app.render()


def _start(app):
    st = _state(app)
    app.player.stop_all()
    st.update(seq=[], step=0, error=False, round=0, lit=None, phase="show")
    _next_round(app)


def _pad_button(app, pad):
    st = _state(app)
    lit = st["lit"] == pad["id"]
    enabled = st["phase"] == "input"
    color = pad["color"] if lit else "#121a17"
    return ft.Container(
        width=150,
        height=110,
        border_radius=ft.BorderRadius.all(20),
        bgcolor=color if lit else "#141f1a",
        border=ft.Border.all(2, pad["color"] if not lit else pad["color"]),
        opacity=1.0 if (lit or enabled) else 0.45,
        scale=1.06 if lit else 1.0,
        on_click=lambda e: _press(app, pad["id"]),
        ink=True,
    )


def build(app):
    st = _state(app)
    status = {
        "idle": "Toque em iniciar",
        "show": "Observe a sequência",
        "input": "Sua vez",
        "over": f"Errou na rodada {st['round']} — melhor: {st['best']}",
    }[st["phase"]]

    header_right = ft.Row(
        [
            ft.Text(f"Rodada: {st['round']}", size=13, color=theme.DIM),
            ft.Icon(ft.Icons.EMOJI_EVENTS, size=16, color="#eab308"),
            ft.Text(str(st["best"]), size=13, color=theme.FG),
        ],
        spacing=6,
        vertical_alignment=ft.CrossAxisAlignment.CENTER,
    )

    return theme.panel(
        [
            ft.Row([_pad_button(app, p) for p in PADS], wrap=True, spacing=12, run_spacing=12),
            theme.cta("Jogar de novo" if st["phase"] == "over" else "Iniciar", lambda e: _start(app)),
        ],
        title="Sequência de cores",
        hint=status,
        right=header_right,
        padding=28,
    )
