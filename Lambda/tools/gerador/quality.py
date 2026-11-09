from __future__ import annotations
import math, re, time
from collections import Counter

def tokenize(text: str):
    return re.findall(r"[\wÀ-ÿ]+", (text or "").lower())

def cosine(a: str, b: str) -> float:
    ca, cb = Counter(tokenize(a)), Counter(tokenize(b))
    if not ca or not cb: return 0.0
    dot = sum(ca[k] * cb.get(k, 0) for k in ca)
    na = math.sqrt(sum(v*v for v in ca.values()))
    nb = math.sqrt(sum(v*v for v in cb.values()))
    return (dot / (na * nb)) if na and nb else 0.0

def is_near_dup(text: str, existing: list[str], threshold: float = 0.85) -> bool:
    lt = len(text or "")
    for k in existing:
        if abs(len(k) - lt) > max(40, int(0.4 * max(lt, len(k), 1))):
            continue
        if cosine(text, k) >= threshold:
            return True
    return False

JARGON_HINTS = {
    "heurística", "paradigma", "disruptivo", "sinergia", "holístico", "performar",
    "ressignificar", "protagonizar", "placebo social",
}
# Heurística leve: não bloquear falso positivo; apenas sinalizar em relatório.

def jargon_flags(text: str) -> list[str]:
    toks = set(tokenize(text))
    return [j for j in JARGON_HINTS if j in toks]

def now_ms() -> int:
    return int(time.time() * 1000)

def clean_text(s: str | None) -> str:
    return re.sub(r"\s+", " ", (s or "")).strip()

def valid_min(text: str, min_len: int) -> bool:
    return len(clean_text(text)) >= min_len
