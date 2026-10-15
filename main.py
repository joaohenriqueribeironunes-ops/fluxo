"""Ponto de entrada do Fluxo em Python/Flet.

Desktop/web:  flet run .        (ou python main.py)
Android:      flet build apk    (o CI faz isso em .github/workflows/android.yml)
"""

import flet as ft

from fluxo.app import FluxoApp


def main(page: ft.Page):
    app = FluxoApp(page)
    page.on_close = lambda e: app.shutdown()


if __name__ == "__main__":
    ft.run(main)
