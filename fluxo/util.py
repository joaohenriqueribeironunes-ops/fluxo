"""Formatação compartilhada entre as telas."""

import datetime as dt
from pathlib import Path

DATE_HINTS = ("AAAA-MM-DD HH:MM", "DD/MM/AAAA HH:MM")


def file_uri(path):
    """Áudio local precisa de URI de arquivo para o player nativo."""
    p = Path(path)
    return p.as_uri() if p.is_absolute() else str(path)


def mmss(total_seconds):
    total = max(0, int(total_seconds))
    return f"{total // 60:02d}:{total % 60:02d}"


def clock(seconds):
    """m:ss sem zeros à esquerda na frente — usado na minutagem do player."""
    total = max(0, int(seconds))
    return f"{total // 60}:{total % 60:02d}"


def parse_when(raw):
    """Aceita as duas máscaras que o app mostra como dica; devolve epoch em ms ou None."""
    text = (raw or "").strip().replace("T", " ")
    if not text:
        return None
    for fmt in ("%Y-%m-%d %H:%M", "%d/%m/%Y %H:%M", "%Y-%m-%d %H:%M:%S", "%d/%m/%Y %H:%M:%S"):
        try:
            return int(dt.datetime.strptime(text, fmt).timestamp() * 1000)
        except ValueError:
            continue
    return None


def when_label(ms):
    return dt.datetime.fromtimestamp(ms / 1000).strftime("%d/%m/%Y %H:%M")


def date_label(ms):
    return dt.datetime.fromtimestamp(ms / 1000).strftime("%d/%m/%Y")
