"""Tokens e blocos visuais portados do CSS do site (vidro esmeralda)."""

import flet as ft

BG = "#11201b"
BG_TOP = "#1f3a31"
BG_BOTTOM = "#0c1814"
FG = "#f4f9f7"
DIM = "#a9c0b8"
EMERALD = "#34d399"
EMERALD_BRIGHT = "#a7f3d0"
EMERALD_INK = "#022c22"

_A05 = "#0DFFFFFF"
_A14 = "#24FFFFFF"
_EMERALD_15 = "#2634D399"
_EMERALD_BORDER = "#33A7F3D0"
_TRACK = "#17FFFFFF"

PADS = {0: "#22c55e", 1: "#ef4444", 2: "#3b82f6", 3: "#eab308"}

MAX_W = 680
DOCK_H = 96


def background():
    """Radial do site: clareia no topo e escurece para a base."""
    return ft.Container(
        expand=True,
        gradient=ft.RadialGradient(
            center=ft.Alignment(0, -1),
            radius=1.4,
            colors=[BG_TOP, BG, BG_BOTTOM],
            stops=[0.0, 0.55, 1.0],
        ),
    )


def glass(content, radius=24, padding=24):
    return ft.Container(
        content=content,
        padding=ft.Padding.all(padding),
        border_radius=ft.BorderRadius.all(radius),
        bgcolor=_A05,
        gradient=ft.LinearGradient(
            begin=ft.Alignment(-1, -1),
            end=ft.Alignment(1, 1),
            colors=[_A14, "#0AFFFFFF", "#05FFFFFF"],
            stops=[0.0, 0.42, 1.0],
        ),
        border=ft.Border.all(1, _A14),
        shadow=ft.BoxShadow(blur_radius=32, color="#59000000", offset=ft.Offset(0, 8)),
    )


def column(controls, spacing=14, width=MAX_W, scroll=True):
    return ft.Column(
        controls,
        spacing=spacing,
        width=width,
        scroll=ft.ScrollMode.AUTO if scroll else None,
    )


def panel(controls, title=None, hint=None, right=None, padding=24):
    head = None
    if title:
        left = ft.Column(
            [
                ft.Text(title, size=18, weight=ft.FontWeight.W_600, color=FG),
                *([ft.Text(hint, size=13, color=DIM)] if hint else []),
            ],
            spacing=2,
        )
        head = (
            ft.Row(
                [left, ft.Container(expand=True), right],
                vertical_alignment=ft.CrossAxisAlignment.CENTER,
            )
            if right
            else left
        )
    body = ft.Column(([head] if head else []) + controls, spacing=14)
    return ft.Row([glass(body, padding=padding)], width=MAX_W)


def chip(label, pressed=False, on_click=None, icon=None):
    content = (
        ft.Row(
            [
                ft.Icon(icon, size=16, color=EMERALD_BRIGHT if pressed else DIM),
                ft.Text(label, size=13, color=FG),
            ],
            spacing=6,
            alignment=ft.MainAxisAlignment.CENTER,
        )
        if icon
        else ft.Text(label, size=13, color=EMERALD_BRIGHT if pressed else FG)
    )
    return ft.Container(
        content=content,
        alignment=ft.Alignment(0, 0),
        padding=ft.Padding(14, 10, 14, 10),
        border_radius=ft.BorderRadius.all(999),
        bgcolor=_EMERALD_15 if pressed else _A05,
        border=ft.Border.all(1, _EMERALD_BORDER if pressed else _A14),
        on_click=on_click,
        ink=True,
        ink_color=FG,
    )


def cta(label, on_click, icon=None, disabled=False, expand=False):
    content = ft.Row(
        [
            *([ft.Icon(icon, size=16, color=EMERALD_INK)] if icon else []),
            ft.Text(label, size=15, weight=ft.FontWeight.W_600, color=EMERALD_INK),
        ],
        spacing=6,
        alignment=ft.MainAxisAlignment.CENTER,
    )
    return ft.Container(
        content=content,
        alignment=ft.Alignment(0, 0),
        padding=ft.Padding(20, 13, 20, 13),
        border_radius=ft.BorderRadius.all(999),
        bgcolor="#1E3A31" if disabled else EMERALD,
        on_click=None if disabled else on_click,
        expand=expand,
        ink=True,
        ink_color=EMERALD_INK,
    )


def icon_button(icon, on_click, label=None, ghost=False, size=52, glyph=22):
    return ft.Container(
        content=ft.Icon(icon, size=glyph, color=DIM if ghost else FG),
        width=size,
        height=size,
        alignment=ft.Alignment(0, 0),
        border_radius=ft.BorderRadius.all(999),
        bgcolor=None if ghost else _A05,
        border=None if ghost else ft.Border.all(1, _A14),
        on_click=on_click,
        tooltip=label,
        ink=True,
    )


def card(title, subtitle, pressed=False, on_click=None, width=None, height=None):
    return ft.Container(
        content=ft.Column(
            [
                ft.Text(title, size=15, weight=ft.FontWeight.W_600, color=FG),
                ft.Text(subtitle, size=12.5, color=DIM),
            ],
            spacing=4,
        ),
        padding=ft.Padding(16, 14, 16, 14),
        border_radius=ft.BorderRadius.all(18),
        bgcolor=_EMERALD_15 if pressed else _A05,
        border=ft.Border.all(1.5 if pressed else 1, _EMERALD_BORDER if pressed else _A14),
        on_click=on_click,
        ink=True,
        ink_color=FG,
        width=width,
        height=height,
    )


def field(value="", hint="", on_change=None, on_submit=None, multiline=False, width=None, keyboard=None):
    return ft.TextField(
        value=value,
        hint_text=hint,
        on_change=on_change,
        on_submit=on_submit,
        multiline=multiline,
        min_lines=4 if multiline else 1,
        max_lines=8 if multiline else 1,
        width=width,
        keyboard_type=keyboard,
        filled=True,
        fill_color=_A05,
        border=ft.NoInputBorder(),
        border_radius=14,
        focused_border_color=EMERALD,
        border_color=_A14,
        color=FG,
        hint_style=ft.TextStyle(color=DIM),
        content_padding=ft.Padding(14, 12, 14, 12),
    )


def output_box(text):
    return ft.Container(
        content=ft.Text(text, selectable=True, color=FG, size=13.5),
        padding=ft.Padding(16, 14, 16, 14),
        border_radius=ft.BorderRadius.all(14),
        bgcolor="#0AFFFFFF",
        border=ft.Border.all(1, "#08FFFFFF"),
        width=MAX_W - 2,
    )


def track_row(title, meta, actions):
    return ft.Container(
        content=ft.Row(
            [
                ft.Text(title, size=14, color=FG, no_wrap=True, overflow=ft.TextOverflow.ELLIPSIS, expand=True),
                *([ft.Text(meta, size=12, color=DIM)] if meta else []),
                *actions,
            ],
            spacing=10,
            vertical_alignment=ft.CrossAxisAlignment.CENTER,
        ),
        padding=ft.Padding(14, 10, 14, 10),
        border_radius=ft.BorderRadius.all(14),
        bgcolor="#08FFFFFF",
    )


def segmented(options, selected, on_change):
    """Abas internas — o .segmented do site."""
    return ft.Container(
        content=ft.Row(
            [
                ft.Container(
                    content=ft.Text(label, size=13, color=EMERALD_BRIGHT if selected == key else DIM),
                    expand=True,
                    alignment=ft.Alignment(0, 0),
                    padding=ft.Padding(10, 11, 10, 11),
                    border_radius=ft.BorderRadius.all(12),
                    bgcolor="#33A7F3D0" if selected == key else None,
                    on_click=lambda e, k=key: on_change(k),
                    ink=True,
                )
                for key, label in options
            ],
            spacing=4,
        ),
        padding=4,
        bgcolor=_A05,
        border=ft.Border.all(1, _A14),
        border_radius=ft.BorderRadius.all(14),
        width=MAX_W,
    )


def row_of(items, spacing=8):
    return ft.Row(items, spacing=spacing, wrap=True, alignment=ft.MainAxisAlignment.CENTER)


def note(text):
    return ft.Text(text, size=12.5, color=DIM)


def danger(label, icon, on_click):
    """Chip de ação destrutiva — mesmo formato do .chip, tinta vermelha no rótulo."""
    return ft.Container(
        content=ft.Row(
            [ft.Icon(icon, size=16, color="#fca5a5"), ft.Text(label, size=13, color="#fca5a5")],
            spacing=6,
        ),
        padding=ft.Padding(14, 10, 14, 10),
        border_radius=ft.BorderRadius.all(999),
        bgcolor=_A05,
        border=ft.Border.all(1, _A14),
        on_click=on_click,
        ink=True,
    )
