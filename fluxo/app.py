"""Núcleo do app: estado, navegação, tema e relógio compartilhado das telas."""

import os
import threading
import time

import flet as ft

from . import theme
from .db import FluxoDB
from .player import Player

TABS = [
    ("foco", "Foco", ft.Icons.PSYCHOLOGY),
    ("jogo", "Jogo", ft.Icons.GAMEPAD),
    ("frisson", "Frisson", ft.Icons.MUSIC_NOTE),
    ("ideas", "Ideias", ft.Icons.LIGHTBULB),
    ("ajustes", "Ajustes", ft.Icons.SETTINGS),
]

WEATHER_TINT = {
    "clear": None,
    "chuva": ("#1f3a31", "#0a1a24", "#050d12"),
    "sol": ("#3a3122", "#1c1a12", "#0d0c08"),
    "nevoa": ("#2b2f2e", "#171a19", "#0c0e0d"),
}


def data_dir():
    override = os.environ.get("FLUXO_HOME")
    if override:
        return override
    home = os.path.join(os.path.expanduser("~"), ".fluxo")
    return home


class FluxoApp:
    def __init__(self, page: ft.Page):
        self.page = page
        page.title = "Fluxo"
        page.theme_mode = ft.ThemeMode.DARK
        page.bgcolor = theme.BG
        page.padding = 0
        page.theme = ft.Theme(color_scheme_seed=theme.EMERALD, font_family="Roboto")

        self.view = ft.View(route="/", controls=[], services=[], padding=0)
        page.views.append(self.view)

        self.db = FluxoDB(os.path.join(data_dir(), "fluxo.db"))
        self.player = Player(self.view, refresh=self.refresh)

        self.tab = "foco"
        self.mode = self.db.get_setting("mode", "divergente")
        self.weather = self.db.get_setting("weather", "clear")
        self.ui = {}
        self.status = ""

        self._listeners = {}
        self._listener_seq = 0
        self._stop = threading.Event()
        threading.Thread(target=self._clock, daemon=True).start()

        self.player.mark_ready()
        from .components import appointments

        self.add_ticker(60, lambda: appointments.check_due(self))
        self.render()

    def refresh(self):
        """View em page.views ainda não tem pai antes do primeiro update: atualiza pela page."""
        self.page.update()

    def add_service(self, control):
        """Serviço avulso (ex.: FilePicker) vive na view, junto com os canais de áudio."""
        self.view.services.append(control)
        self.refresh()

    # ---------- relógio compartilhado ----------
    def _clock(self):
        """Ticker único de 1Hz; cada listener guarda quanto falta para disparar."""
        while not self._stop.is_set():
            self._stop.wait(1)
            for key, listener in list(self._listeners.items()):
                every, left, fn = listener
                left -= 1
                if left > 0:
                    self._listeners[key] = (every, left, fn)
                    continue
                self._listeners[key] = (every, every, fn)
                try:
                    self.page.run_thread(fn)
                except Exception:  # noqa: BLE001 - página pode já estar fechada
                    self._listeners.pop(key, None)

    def add_ticker(self, every, fn):
        self._listener_seq += 1
        key = self._listener_seq
        self._listeners[key] = (every, every, fn)
        return key

    def remove_ticker(self, key):
        self._listeners.pop(key, None)

    def later(self, seconds, fn):
        """Dispara fn uma vez, depois de `seconds`, dentro do laço da UI."""

        def worker():
            time.sleep(seconds)
            if not self._stop.is_set():
                try:
                    self.page.run_thread(fn)
                except Exception:  # noqa: BLE001
                    pass

        threading.Thread(target=worker, daemon=True).start()

    def background(self, fn, on_result):
        """Roda fn (bloqueante, ex.: rede) fora da UI e aplica o resultado nela."""

        def worker():
            try:
                value = fn()
            except Exception as exc:  # noqa: BLE001 - rede é limite do sistema
                value = exc
            try:
                self.page.run_thread(lambda: on_result(value))
            except Exception:  # noqa: BLE001
                pass

        threading.Thread(target=worker, daemon=True).start()

    # ---------- persistência de preferências ----------
    def persist(self, key, value):
        self.db.set_setting(key, value)

    def say(self, message):
        self.status = message
        self.render()

    # ---------- render ----------
    def goto(self, tab):
        self.tab = tab
        self.render()

    def render(self):
        from .screens import ajustes, foco, frisson, ideias, jogo

        builders = {"foco": foco.build, "jogo": jogo.build, "frisson": frisson.build,
                    "ideas": ideias.build, "ajustes": ajustes.build}
        body = builders[self.tab](self)
        # telas devolvem ou uma lista de blocos (foco, frisson) ou um painel único (jogo, ideias)
        body = list(body) if isinstance(body, (list, tuple)) else [body]
        self.view.controls = [
            ft.Stack(
                controls=[theme.background(), *self._weather_layer(), self._shell(body)],
                alignment=ft.Alignment(0, 0),
            )
        ]
        self.view.bottom_appbar = ft.BottomAppBar(
            bgcolor=None,
            elevation=0,
            content=ft.Container(alignment=ft.Alignment(0, 0), content=self._dock()),
        )
        self.refresh()

    def _weather_layer(self):
        tint = WEATHER_TINT.get(self.weather)
        if not tint:
            return []
        return [
            ft.Container(
                expand=True,
                opacity=0.55,
                gradient=ft.LinearGradient(
                    begin=ft.Alignment(0, -1), end=ft.Alignment(0, 1), colors=list(tint)
                ),
            )
        ]

    def _shell(self, body):
        return ft.SafeArea(
            content=ft.Container(
                alignment=ft.Alignment(0, -1),
                padding=ft.Padding(16, 8, 16, theme.DOCK_H + 24),
                expand=True,
                content=ft.Column([self._banner(), *body], spacing=18, width=theme.MAX_W),
            ),
        )

    def _banner(self):
        infinity = ft.Stack(
            [
                ft.Container(
                    width=15, height=15, left=0, top=0,
                    border=ft.Border.all(1.6, theme.EMERALD_BRIGHT),
                    border_radius=ft.BorderRadius.all(999),
                ),
                ft.Container(
                    width=15, height=15, left=9, top=0,
                    border=ft.Border.all(1.6, theme.EMERALD),
                    border_radius=ft.BorderRadius.all(999),
                ),
            ],
            width=25,
            height=16,
        )
        wordmark = ft.Row(
            [
                ft.Text("Fluxo", size=26, weight=ft.FontWeight.W_300, color=theme.FG),
                infinity,
            ],
            spacing=6,
            vertical_alignment=ft.CrossAxisAlignment.CENTER,
        )
        label = dict(TABS and [(t[0], t[1]) for t in TABS]).get(self.tab, "")
        return ft.Row(
            [
                ft.Column(
                    [
                        wordmark,
                        ft.Container(
                            content=ft.Text("Diferente não é menos — é potência.", size=12, color=theme.EMERALD_BRIGHT),
                            padding=ft.Padding(10, 4, 10, 4),
                            border_radius=999,
                            bgcolor="#386EE7B7",
                            border=ft.Border.all(1, "#4DA7F3D0"),
                        ),
                    ],
                    spacing=6,
                    expand=True,
                ),
                ft.Container(
                    content=ft.Text("TEA · TDAH", size=11, color=theme.DIM),
                    padding=ft.Padding(10, 6, 10, 6),
                    border_radius=999,
                    bgcolor="#0DFFFFFF",
                    border=ft.Border.all(1, "#24FFFFFF"),
                ),
                ft.Text(label, size=12, color=theme.DIM),
            ],
            vertical_alignment=ft.CrossAxisAlignment.CENTER,
            width=theme.MAX_W,
        )

    def _dock(self):
        buttons = []
        for tab_id, label, icon in TABS:
            active = tab_id == self.tab
            buttons.append(
                ft.Container(
                    content=ft.Column(
                        [
                            ft.Icon(icon, size=22, color=theme.FG if active else theme.DIM),
                            ft.Text(label, size=11, color=theme.FG if active else theme.DIM),
                        ],
                        spacing=2,
                        horizontal_alignment=ft.CrossAxisAlignment.CENTER,
                    ),
                    expand=True,
                    alignment=ft.Alignment(0, 0),
                    padding=ft.Padding(6, 8, 6, 8),
                    border_radius=ft.BorderRadius.all(16),
                    bgcolor="#33FFFFFF" if active else None,
                    scale=1.05 if active else None,
                    on_click=lambda e, t=tab_id: self.goto(t),
                    ink=True,
                )
            )
        return ft.Container(
            content=ft.Row(buttons, spacing=2),
            width=theme.MAX_W,
            padding=ft.Padding(10, 6, 10, 6),
            border_radius=ft.BorderRadius.all(28),
            bgcolor="#80464B49",
            border=ft.Border.all(1, "#24FFFFFF"),
            blur=ft.Blur(18, 18, tile_mode=ft.BlurTileMode.CLAMP),
            shadow=ft.BoxShadow(blur_radius=32, color="#80000000", offset=ft.Offset(0, 8)),
        )

    # ---------- utilidades para as telas ----------
    def ui_state(self, screen, **defaults):
        state = self.ui.setdefault(screen, {})
        for key, value in defaults.items():
            state.setdefault(key, value)
        return state

    def confirm(self, title, message, on_yes, yes_label="Confirmar"):
        dialog = ft.AlertDialog(
            modal=True,
            title=ft.Text(title, color=theme.FG),
            content=ft.Text(message, color=theme.DIM),
            actions=[
                ft.TextButton("Cancelar", on_click=lambda e: self.close(dialog)),
                ft.FilledButton(yes_label, on_click=lambda e: (self.close(dialog), on_yes())),
            ],
        )
        self.page.show_dialog(dialog)

    def close(self, dialog):
        self.page.pop_dialog()

    def shutdown(self):
        self._stop.set()
        self.player.stop_all()
        self.db.close()
