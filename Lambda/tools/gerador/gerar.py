#!/usr/bin/env python3
"""Gerador Lambda (Fase 2).

- Lê GEMINI_API_KEY apenas de local.properties (nunca loga/exibe).
- Lotes por subtema/subárea, JSON estruturado, backoff + limite de RPM, checkpoint.
- Estima chamadas/custo e EXIGE confirmação antes de consumir API.
- Deduplicação (cosseno < 0,85), tamanho mínimo, revisado=false, relatório de cobertura.
- Conteúdo sensível: informativo, sem diagnóstico/dose/plano prescritivo.
- Empacota .db em app/src/main/assets/ ao final (opcional).

Uso:
  python tools/gerador/gerar.py --estimate-only
  python tools/gerador/gerar.py --target behavior
  python tools/gerador/gerar.py --target academic
  python tools/gerador/gerar.py --target all
"""
from __future__ import annotations
import argparse, os, sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
if str(HERE) not in sys.path:
    sys.path.insert(0, str(HERE))

from config import Config, load_api_key, ASSETS_DIR, CHECKPOINT_DIR, REPORT_DIR, ROOT
from content_plan import total_estimate, estimate_behavior, estimate_academic, RELACAO_DB, SUBTEMAS_COMPORTAMENTO, ACADEMIC_AREAS
from generate import generate_behavior, generate_academic, package_assets, _report
from gemini_client import GeminiClient

def print_estimate(cfg: Config, target: str):
    est = total_estimate(cfg.batch_size)
    def cost(calls):
        tin = calls * cfg.est_input_tokens_per_call
        tout = calls * cfg.est_output_tokens_per_call
        usd = (tin/1_000_000)*cfg.input_per_mtok + (tout/1_000_000)*cfg.output_per_mtok
        return tin, tout, usd
    print("\n=== ESTIMATIVA (aproximada) ===")
    print(f"Modelo: {cfg.model} | RPM limite: {cfg.rpm_limit} | batch behavior: {cfg.batch_size}")
    print(f"Relações: {len(RELACAO_DB)} | subtemas: {len(SUBTEMAS_COMPORTAMENTO)} | perfis ND/NT")
    print(f"Áreas acadêmicas: {len(ACADEMIC_AREAS)} | subáreas: {est['academic']['subareas']} | etimologias alvo: {est['academic']['alvo_etimologia']}")
    if target in ("behavior","all"):
        b = est["behavior"]
        tin, tout, usd = cost(b["chamadas"])
        print(f"\n[behavior] alvo ~{b['alvo']} respostas distintas (células {b['celulas']}, ~{b['por_celula']}/célula)")
        print(f"  chamadas estimadas: ~{b['chamadas']} | tokens in/out ~{tin}/{tout} | custo aprox US$ {usd:.2f} (ajuste GEMINI_*_USD_MTOK)")
    if target in ("academic","all"):
        a = est["academic"]
        tin, tout, usd = cost(a["chamadas"])
        print(f"\n[academic] alvo {a['alvo_frases']} frases ({a['subareas']} subáreas × 50) + {a['alvo_etimologia']} etimologias")
        print(f"  chamadas estimadas: ~{a['chamadas']} | tokens in/out ~{tin}/{tout} | custo aprox US$ {usd:.2f}")
    t = est["total_chamadas"] if target=="all" else (est["behavior"]["chamadas"] if target=="behavior" else est["academic"]["chamadas"])
    tin, tout, usd = cost(t)
    print(f"\nTOTAL ({target}): ~{t} chamadas | ~US$ {usd:.2f} (estimativa; cota/custo real podem variar)")
    print("Checkpoint: retoma automaticamente de tools/gerador/.checkpoints/ em caso de interrupção.")
    print("Segurança: conteúdo sensível é informativo, sem diagnóstico, sem dose, sem plano prescritivo.")
    return t, usd

def confirm_or_exit(yes: bool):
    if yes:
        print("[confirmação] --yes fornecido; prosseguindo.")
        return
    if not sys.stdin.isatty():
        print("ERRO: confirmação interativa necessária. Rode em terminal ou use --yes após revisar a estimativa.", file=sys.stderr)
        sys.exit(3)
    ans = input("\nConfirmar execução e consumo de API? Digite SIM para prosseguir: ").strip()
    if ans != "SIM":
        print("Cancelado pelo usuário. Nenhuma chamada à API foi feita.")
        sys.exit(0)

def main():
    ap = argparse.ArgumentParser(description="Gerador Lambda (Gemini -> SQLite assets)")
    ap.add_argument("--target", choices=["behavior","academic","all"], default="all")
    ap.add_argument("--estimate-only", action="store_true", help="Mostra estimativa e sai sem chamar a API")
    ap.add_argument("--yes", action="store_true", help="Confirma execução (use após revisar a estimativa)")
    ap.add_argument("--out-dir", default=str(HERE / "out"), help="Diretório de saída dos .db gerados")
    ap.add_argument("--no-assets", action="store_true", help="Não copiar .db para app/src/main/assets")
    ap.add_argument("--rpm", type=int, help="Limite de requisições por minuto (padrão: GEMINI_RPM ou 12)")
    ap.add_argument("--model", help="Modelo Gemini (padrão: gemini-1.5-flash)")
    args = ap.parse_args()

    cfg = Config()
    if args.rpm: cfg.rpm_limit = args.rpm
    if args.model: cfg.model = args.model

    _calls, _usd = print_estimate(cfg, args.target)
    if args.estimate_only:
        return 0

    # confirmação obrigatória antes de qualquer chamada
    confirm_or_exit(args.yes)
    api_key = load_api_key()  # carrega só após confirmação
    client = GeminiClient(api_key, cfg)

    out = Path(args.out_dir)
    out.mkdir(parents=True, exist_ok=True)
    b_db = out / "behavior.db"
    a_db = out / "academic.db"
    b_stats = {"skipped": True}; a_stats = {"skipped": True}; jargon = []

    try:
        if args.target in ("behavior","all"):
            s, j = generate_behavior(client, cfg, b_db)
            b_stats = s; jargon += j
        if args.target in ("academic","all"):
            s, j = generate_academic(client, cfg, a_db)
            a_stats = s; jargon += j
    except KeyboardInterrupt:
        print("\nInterrompido. Progresso salvo no checkpoint; rode novamente para retomar.")
    except Exception as e:
        print(f"\nERRO: {e}\nProgresso salvo no checkpoint (quando aplicável).", file=sys.stderr)
        return 1

    _report(cfg, b_stats, a_stats, jargon)
    if not args.no_assets:
        package_assets(b_db, a_db)
        print(f"\nAssets em: {ASSETS_DIR}")
    print("\nPróximo passo: python tools/verificar_volume.py (deve passar ao atingir as metas).")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
