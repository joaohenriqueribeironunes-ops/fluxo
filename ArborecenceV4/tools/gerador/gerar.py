#!/usr/bin/env python3
"""Esqueleto do gerador (Fase 2). Lê GEMINI_API_KEY de local.properties; nunca loga a chave."""
from __future__ import annotations
import os, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LOCAL_PROPS = ROOT / "local.properties"

def load_api_key() -> str:
    if not LOCAL_PROPS.exists():
        print(f"ERRO: {LOCAL_PROPS} não encontrado. Crie com GEMINI_API_KEY=... (não commitar).", file=sys.stderr)
        sys.exit(2)
    for line in LOCAL_PROPS.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = line.strip()
        if line.startswith("GEMINI_API_KEY="):
            key = line.split("=", 1)[1].strip()
            if key and key != "COLOQUE_SUA_CHAVE_AQUI":
                return key
    print("ERRO: GEMINI_API_KEY ausente em local.properties.", file=sys.stderr)
    sys.exit(2)

def main():
    key = load_api_key()  # noqa: F841 - usado nas chamadas de API (Fase 2)
    print("Gerador (Fase 2): chave carregada de local.properties (não exibida).")
    print("Implementação completa (lotes, schema, backoff, checkpoint, estimativa/custo, segurança) pendente — ver docs/PENDENCIAS.md.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
