"""Repouso sensorial: respiração quadrada, ancoragem 5-4-3-2-1 e dicas."""

import flet as ft

from .. import theme

BOX_PHRASES = [
    "Solte o ar completamente.",
    "Puxe pelo nariz em 4 tempos.",
    "Segure sem forçar, 4 tempos.",
    "Solte devagar em 4 tempos.",
]

GROUNDDING = [
    "5 coisas que você está vendo",
    "4 coisas que você está tocando",
    "3 sons que você ouve",
    "2 cheiros no ambiente",
    "1 coisa que você gosta em você",
]

TIPS = [
    "Comece errado. Iniciar vale mais que acertar de primeira.",
    "Uma tarefa de 2 minutos custa menos que uma de 25 — faça a de 2 minutos.",
    "Hiperfoco não é prêmio nem desculpa: é estado. Anote onde você parou antes de sair dele.",
    "Barulho previsível acalma; imprevisível esgota. Controle o que você pode controlar.",
    "Se a intenção era descansar, descanse de verdade. Metade do repouso não conta.",
]

LABELS = [("respiracao", "Respiração"), ("ancoragem", "Ancoragem"), ("dicas", "Dicas")]


def _state(app):
    return app.ui_state("rest", mode=None, running=False, phase=0, tick=None)


def _toggle(app):
    st = _state(app)
    if st["running"]:
        app.remove_ticker(st["tick"])
        st.update(running=False, tick=None)
    else:
        st["phase"] = 0
        st["running"] = True
        st["tick"] = app.add_ticker(4, lambda: _advance(app))
    app.render()


def _advance(app):
    st = _state(app)
    if not st["running"]:
        return
    st["phase"] = (st["phase"] + 1) % len(BOX_PHRASES)
    app.render()


def _pick_mode(app, mode):
    st = _state(app)
    if st["running"]:
        app.remove_ticker(st["tick"])
        st.update(running=False, tick=None)
    st["mode"] = mode
    app.render()


def build(app):
    st = _state(app)
    body = []

    if st["mode"] == "respiracao":
        phrase = BOX_PHRASES[st["phase"]] if st["running"] else BOX_PHRASES[0]
        body += [
            theme.note("Respiração quadrada 4-4-4-4"),
            ft.Text(phrase, size=22, color=theme.FG),
            theme.cta("Pausar" if st["running"] else "Começar", lambda e: _toggle(app)),
        ]
    elif st["mode"] == "ancoragem":
        body += [
            ft.Column(
                [
                    ft.Text(f"{i + 1}. {g}", size=15, color=theme.FG)
                    for i, g in enumerate(GROUNDDING)
                ],
                spacing=8,
            )
        ]
    elif st["mode"] == "dicas":
        body += [theme.output_box("\n\n".join(TIPS))]

    return theme.panel(
        [theme.segmented(LABELS, st["mode"], lambda k: _pick_mode(app, k)), *body],
        title="Repouso sensorial",
        hint="Exercícios curtos para regular o sistema sensorial e voltar ao eixo.",
    )
