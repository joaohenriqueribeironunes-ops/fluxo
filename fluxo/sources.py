"""Busca de artigos na Wikipedia em português — a única parte do app que precisa de rede."""

import json
import re
import urllib.parse
import urllib.request

API = "https://pt.wikipedia.org/w/api.php"
UA = "Fluxo/1.0 (app local; contato do usuário no repositório)"
TAGS = re.compile(r"<[^>]+>")


def _get(params):
    query = dict(params, format="json", origin="*")
    url = f"{API}?{urllib.parse.urlencode(query)}"
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=15) as res:
        return json.loads(res.read().decode("utf-8"))


def search(query, limit=6):
    data = _get({"action": "query", "list": "search", "srsearch": query, "srlimit": limit})
    results = data.get("query", {}).get("search", []) or []
    return [{"title": r["title"], "snippet": TAGS.sub("", r.get("snippet", ""))} for r in results]


def plain_text(title, max_chars=40_000):
    data = _get(
        {
            "action": "query",
            "prop": "extracts",
            "explaintext": "1",
            "redirects": "1",
            "titles": title,
        }
    )
    pages = data.get("query", {}).get("pages", {}) or {}
    for page in pages.values():
        extract = page.get("extract")
        if extract:
            return extract[:max_chars]
    return ""


def article_url(title):
    return f"https://pt.wikipedia.org/wiki/{urllib.parse.quote(title.replace(' ', '_'))}"
