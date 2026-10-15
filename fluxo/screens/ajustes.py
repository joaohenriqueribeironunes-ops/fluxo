"""Ajustes: modo de tempo, dados do aparelho e funcionamento offline."""

import flet as ft

from .. import theme
from ..waves import MODES

CARD_W = 250


def _icon(name):
    return getattr(ft.Icons, name.upper(), ft.Icons.BLOCK)


def _state(app):
    return app.ui_state("ajustes", confirming=False)


def _set_weather(app, mode_id):
    app.weather = mode_id
    app.persist("weather", mode_id)
    app.render()


def _ask_confirm(app):
    _state(app)["confirming"] = True
    app.render()


def _cancel(app):
    _state(app)["confirming"] = False
    app.render()


def _wipe(app):
    app.player.stop_all()
    app.db.wipe()
    app.ui.clear()
    app.mode = "divergente"
    app.weather = "clear"
    _state(app)["confirming"] = False
    app.say("Dados apagados. Nada saiu do aparelho.")


def build(app):
    st = _state(app)

    weather = theme.panel(
        [
            ft.Row(
                [
                    theme.card(
                        m["label"],
                        m["hint"],
                        pressed=app.weather == m["id"],
                        on_click=lambda e, i=m["id"]: _set_weather(app, i),
                        width=CARD_W,
                        height=132,
                    )
                    for m in MODES
                ],
                wrap=True,
                spacing=12,
                run_spacing=12,
            ),
            theme.note(
                "Cada clima recolore o fundo do app inteiro. A camada animada do site "
                "(gotas e fumaça) não existe nesta versão em Python."
            ),
        ],
        title="Modo de tempo relativo",
    )

    data_actions = (
        [
            theme.row_of(
                [
                    theme.chip("Cancelar", on_click=lambda e: _cancel(app)),
                    theme.cta("Apagar tudo", lambda e: _wipe(app)),
                ]
            )
        ]
        if st["confirming"]
        else [
            theme.danger(
                "Apagar ideias, compromissos, acervo e faixas",
                ft.Icons.DELETE,
                lambda e: _ask_confirm(app),
            )
        ]
    )

    data = theme.panel(
        [
            theme.note("Não há conta nem servidor: tudo vive no aparelho. Apagar é irreversível."),
            *data_actions,
        ],
        title="Seus dados",
    )

    status = theme.panel(
        [
            theme.note(
                "Modo offline: sons, jogo, efeitos, acervo e análise funcionam sem internet. "
                "Só a busca de artigos na Wikipedia precisa de rede."
            ),
            theme.note(
                "Lembretes tocam enquanto o app está aberto. Notificação agendada do Android "
                "depende de um plugin externo e ficou de fora desta versão."
            ),
        ],
        title="Notificações e funcionamento",
    )

    return [
        weather,
        status,
        data,
        ft.Container(
            content=ft.Text("Fluxo — feito para mentes que pensam diferente.", size=12.5, color=theme.DIM),
            alignment=ft.Alignment(0, 0),
            width=theme.MAX_W,
        ),
    ]
