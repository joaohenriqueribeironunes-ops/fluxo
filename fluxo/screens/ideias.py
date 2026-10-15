"""Ideias: análise local sobre o acervo + download de artigos da Wikipedia."""

import flet as ft

from .. import sources, theme, util
from ..analyze import analyze
from ..db import now_ms

MODES = [
    ("divergente", "Divergente"),
    ("profundo", "Profundo"),
    ("dados", "Dados"),
]


def _state(app):
    return app.ui_state("ideias", idea="", result="", query="", hits=[], busy=False)


def _run(app):
    st = _state(app)
    text = st["idea"].strip()
    if not text:
        return
    result = analyze(text, app.db.articles(), app.mode)
    st["result"] = result
    app.db.add_idea(text, app.mode, result)
    app.render()


def _set_mode(app, mode):
    app.mode = mode
    app.persist("mode", mode)
    app.render()


def _search(app):
    st = _state(app)
    query = st["query"].strip()
    if not query or st["busy"]:
        return
    st["busy"] = True
    app.render()

    def show(value):
        st["busy"] = False
        if isinstance(value, Exception):
            st["hits"] = []
            app.status = f"Busca falhou: {value}"
        else:
            st["hits"] = value
        app.render()

    app.background(lambda: sources.search(query), show)


def _save(app, hit):
    st = _state(app)
    st["busy"] = True
    app.render()

    def store(value):
        st["busy"] = False
        st["hits"] = [h for h in st["hits"] if h["title"] != hit["title"]]
        if isinstance(value, str) and value:
            app.db.add_article(hit["title"], sources.article_url(hit["title"]), value)
        app.render()

    app.background(lambda: sources.plain_text(hit["title"]), store)


def _delete(app, idea):
    app.db.delete_idea(idea["id"])
    app.render()


def build(app):
    st = _state(app)
    articles = app.db.articles()
    recent = app.db.recent_ideas(12)

    panels = [
        theme.panel(
            [
                theme.segmented(MODES, app.mode, lambda k: _set_mode(app, k)),
                theme.note("Analisa a ideia aqui no aparelho, usando os artigos que você baixou."),
                theme.field(
                    value=st["idea"],
                    hint="Escreva sua ideia...",
                    multiline=True,
                    on_change=lambda e: st.__setitem__("idea", e.control.value),
                ),
                theme.cta("Analisar", lambda e: _run(app), expand=True),
                *( [theme.output_box(st["result"])] if st["result"] else [] ),
            ],
            title="IA local",
            right=theme.note("no aparelho · usa seus artigos"),
        )
    ]

    hits = [
        theme.track_row(
            h["title"],
            h["snippet"][:80] + ("…" if len(h["snippet"]) > 80 else ""),
            [theme.chip("Guardar", on_click=lambda e, row=h: _save(app, row))],
        )
        for h in st["hits"]
    ]

    panels.append(
        theme.panel(
            [
                theme.note(
                    f"{len(articles)} artigo(s) guardados no aparelho. A busca usa a Wikipedia em português."
                ),
                ft.Row(
                    [
                        theme.field(
                            value=st["query"],
                            hint="Buscar tema...",
                            on_change=lambda e: st.__setitem__("query", e.control.value),
                            on_submit=lambda e: _search(app),
                            width=theme.MAX_W - 140,
                        ),
                        theme.chip("Buscando…" if st["busy"] else "Buscar", on_click=lambda e: _search(app)),
                    ],
                    spacing=10,
                    vertical_alignment=ft.CrossAxisAlignment.CENTER,
                ),
                *hits,
            ],
            title="Acervo de artigos",
        )
    )

    if recent:
        panels.append(
            theme.panel(
                [
                    theme.track_row(
                        i["text"],
                        util.date_label(i["created"]),
                        [theme.chip("Apagar", on_click=lambda e, row=i: _delete(app, row))],
                    )
                    for i in recent
                ],
                title="Ideias recentes",
            )
        )

    return panels
