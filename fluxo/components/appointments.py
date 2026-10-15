"""Compromissos com lembrete enquanto o app está aberto."""

import flet as ft

from .. import theme, util
from ..db import now_ms

AHEAD = [0, 5, 10, 15, 30, 60]
# janela de 2 minutos: um compromisso que já venceu há muito tempo não deve disparar
GRACE_MS = 120_000


def _state(app):
    return app.ui_state("appt", title="", when="", ahead=0)


def _add(app):
    st = _state(app)
    when = util.parse_when(st["when"])
    if not st["title"].strip() or when is None:
        app.say("Preencha o título e a data no formato AAAA-MM-DD HH:MM.")
        return
    app.db.add_appointment(st["title"].strip(), when, st["ahead"])
    st.update(title="", when="")
    app.say("Compromisso salvo.")


def _remove(app, appt):
    app.db.delete_appointment(appt["id"])
    app.render()


def check_due(app):
    """Chamado pelo relógio do app: avisa com som quando um lembrete chega na hora."""
    now = now_ms()
    due = [
        a
        for a in app.db.upcoming_appointments(hours=24)
        if not a["notified"] and now - GRACE_MS <= a["when"] <= now + a["remind_before_min"] * 60_000 + 1
    ]
    if not due:
        return
    for appt in due:
        app.db.mark_notified(appt["id"])
    app.player.stop_loop()
    app.player.ring_alarm()
    names = ", ".join(a["title"] for a in due)
    dialog = ft.AlertDialog(
        title=ft.Text("Compromisso", color=theme.FG),
        content=ft.Text(names, color=theme.DIM),
        actions=[ft.FilledButton("Ok", on_click=lambda e: app.close(None))],
    )
    app.page.show_dialog(dialog)


def build(app):
    st = _state(app)
    items = app.db.upcoming_appointments(hours=24)
    ahead_label = lambda m: "Lembrar: no horário" if m == 0 else f"Lembrar: {m} min antes"

    rows = [
        theme.track_row(
            a["title"],
            f"{util.when_label(a['when'])} · {ahead_label(a['remind_before_min'])}",
            [theme.chip("Remover", on_click=lambda e, row=a: _remove(app, row))],
        )
        for a in items
    ]

    return theme.panel(
        [
            theme.field(
                value=st["title"],
                hint="Compromisso (ex: reunião, remédio, consulta)",
                on_change=lambda e: st.__setitem__("title", e.control.value),
            ),
            theme.field(
                value=st["when"],
                hint="AAAA-MM-DD HH:MM",
                on_change=lambda e: st.__setitem__("when", e.control.value),
                on_submit=lambda e: _add(app),
            ),
            theme.row_of(
                [
                    theme.chip(ahead_label(m), pressed=st["ahead"] == m, on_click=lambda e, v=m: _set_ahead(app, v))
                    for m in AHEAD
                ]
            ),
            theme.cta("Adicionar", lambda e: _add(app), icon=ft.Icons.ADD),
            *( [theme.note("Nenhum compromisso. Adicione um acima.")] if not rows else rows ),
            theme.note(
                "O aviso toca enquanto o app está aberto — o Android não permite "
                "agendar notificação sem serviço externo."
            ),
        ],
        title="Meus compromissos",
    )


def _set_ahead(app, minutes):
    _state(app)["ahead"] = minutes
    app.render()
