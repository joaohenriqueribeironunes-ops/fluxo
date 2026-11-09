from __future__ import annotations
import os, sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LOCAL_PROPS = ROOT / "local.properties"
ASSETS_DIR = ROOT / "app" / "src" / "main" / "assets"
CHECKPOINT_DIR = ROOT / "tools" / "gerador" / ".checkpoints"
REPORT_DIR = ROOT / "tools" / "gerador" / "reports"
BEHAVIOR_SCHEMA = ROOT / "data-behavior" / "schemas" / "com.lambda.databehavior.db.BehaviorDatabase" / "2.json"
ACADEMIC_SCHEMA = ROOT / "data-academic" / "schemas" / "com.lambda.dataacademic.db.AcademicDatabase" / "1.json"

@dataclass
class Config:
    model: str = os.environ.get("GEMINI_MODEL", "gemini-1.5-flash")
    rpm_limit: int = int(os.environ.get("GEMINI_RPM", "12"))  # conservador; ajuste conforme cota
    max_retries: int = 5
    backoff_base: float = 2.0
    backoff_max: float = 90.0
    batch_size: int = 6  # itens por chamada (balanceado para JSON estruturado)
    min_text_len: int = 24
    # estimativa de custo (USD por 1M tokens, aproximado; ajuste antes de rodar)
    input_per_mtok: float = float(os.environ.get("GEMINI_IN_USD_MTOK", "0.075"))
    output_per_mtok: float = float(os.environ.get("GEMINI_OUT_USD_MTOK", "0.30"))
    est_input_tokens_per_call: int = 900
    est_output_tokens_per_call: int = 1800

def load_api_key() -> str:
    if not LOCAL_PROPS.exists():
        print(f"ERRO: {LOCAL_PROPS} não encontrado. Crie com GEMINI_API_KEY=... (não commitar).", file=sys.stderr)
        sys.exit(2)
    for line in LOCAL_PROPS.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = line.strip()
        if not line or line.startswith("#"): continue
        if line.startswith("GEMINI_API_KEY="):
            key = line.split("=", 1)[1].strip().strip('"')
            if key and key != "COLOQUE_SUA_CHAVE_AQUI":
                return key
    print("ERRO: GEMINI_API_KEY ausente em local.properties.", file=sys.stderr)
    sys.exit(2)
