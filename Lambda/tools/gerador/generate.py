from __future__ import annotations
import json, time
from collections import Counter, defaultdict
from pathlib import Path

from config import Config, ASSETS_DIR, CHECKPOINT_DIR, REPORT_DIR
from content_plan import RELACAO_DB, SUBTEMAS_COMPORTAMENTO, PERFIS, ACADEMIC_AREAS, ETIMOLOGIA_ALVO, total_estimate
from checkpoint import Checkpoint
from gemini_client import GeminiClient, GeminiError
from prompts import prompt_respostas, prompt_frases, prompt_etimologia
from quality import clean_text, is_near_dup, valid_min, jargon_flags, now_ms
from db_writer import (create_behavior_db, create_academic_db, insert_resposta, seed_relacao_tipos,
                        ensure_area, ensure_subarea, ensure_conceito, insert_frase, insert_etimologia)

def _report(cfg: Config, behavior_stats: dict, academic_stats: dict, jargon: list):
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    rep = {
        "gerado_em": int(time.time()),
        "modelo": cfg.model,
        "behavior": behavior_stats,
        "academic": academic_stats,
        "jargon_sinalizado_amostras": jargon[:50],
        "observacao": "Itens marcados revisado=false. Conteúdo sensível é informativo, sem diagnóstico/dose/plano prescritivo.",
    }
    (REPORT_DIR / "cobertura.json").write_text(json.dumps(rep, ensure_ascii=False, indent=2), encoding="utf-8")
    # markdown summary
    lines = ["# Relatório de cobertura", f"Modelo: {cfg.model}", ""]
    b = behavior_stats
    lines += ["## Comportamento", f"- total inserido: {b.get('inseridos',0)}", f"- duplicatas/quase-duplicatas descartadas: {b.get('descartadas_dup',0)}",
              f"- inválidas (curtas): {b.get('descartadas_invalidas',0)}", f"- por perfil: {b.get('por_perfil',{})}",
              f"- por relação: {b.get('por_relacao',{})}", ""]
    a = academic_stats
    lines += ["## Acadêmico", f"- frases inseridas: {a.get('frases_inseridas',0)}", f"- subáreas cobertas: {a.get('subareas_cobertas',0)}",
              f"- frases por subárea (amostra): {dict(list((a.get('por_subarea') or {}).items())[:12])}",
              f"- etimologias inseridas: {a.get('etimologias',0)}", f"- descartadas dup: {a.get('descartadas_dup',0)}", ""]
    if jargon:
        lines += [f"## Sinalizações leves de jargão (amostra {min(len(jargon),20)})", *[f"- {j}" for j in jargon[:20]], ""]
    (REPORT_DIR / "COBERTURA_GERADA.md").write_text("\n".join(lines), encoding="utf-8")
    print(f"[relatório] salvo em {REPORT_DIR}")

def generate_behavior(client: GeminiClient, cfg: Config, out_db: Path):
    cp = Checkpoint(CHECKPOINT_DIR / "behavior.json")
    con = create_behavior_db(out_db)
    seed_relacao_tipos(con)
    # load existing texts for dedup (resume)
    existing = defaultdict(list)  # (relacao, subtema, perfil) -> [textos]
    for row in con.execute("SELECT relacaoTipo, subtema, perfilNdOuNt, texto FROM resposta_relacionamento"):
        existing[(row[0], row[1], row[2])].append(row[3])
    stats = {"inseridos": 0, "descartadas_dup": 0, "descartadas_invalidas": 0, "por_perfil": Counter(), "por_relacao": Counter()}
    jargon = []
    alvo_por_celula = 6  # ~3200 / 624 células
    for rel_key, rel_db in RELACAO_DB.items():
        for subtema in SUBTEMAS_COMPORTAMENTO:
            for perfil in PERFIS:
                ck = f"b::{rel_db}::{subtema}::{perfil}"
                key = (rel_db, subtema, perfil)
                need = alvo_por_celula - len(existing[key])
                if need <= 0 or cp.is_done(ck):
                    cp.mark_done(ck, {"count": len(existing[key])})
                    continue
                attempts = 0
                while len(existing[key]) < alvo_por_celula and attempts < 4:
                    attempts += 1
                    batch_n = min(cfg.batch_size, alvo_por_celula - len(existing[key]) + 1)
                    prompt = prompt_respostas(rel_db, subtema, perfil, batch_n, existing[key][-12:])
                    try:
                        data = client.generate_json(prompt)
                    except GeminiError as e:
                        print(f"[ERRO] {ck}: {e}")
                        if e.status in (401, 403): raise
                        break
                    items = data if isinstance(data, list) else data.get("items") or data.get("respostas") or []
                    if not isinstance(items, list): items = []
                    for it in items:
                        texto = clean_text((it or {}).get("texto"))
                        if not valid_min(texto, cfg.min_text_len):
                            stats["descartadas_invalidas"] += 1; continue
                        if is_near_dup(texto, existing[key]):
                            stats["descartadas_dup"] += 1; continue
                        jf = jargon_flags(texto)
                        if jf: jargon.append(f"{subtema}: {jf}")
                        rec = {
                            "relacaoTipo": rel_db, "subtema": subtema, "perfilNdOuNt": perfil,
                            "texto": texto, "tom": clean_text(it.get("tom")) or "caloroso e direto",
                            "tamanho": (it.get("tamanho") or "padrao") if it.get("tamanho") in ("curto","padrao","detalhado") else "padrao",
                            "gatilhoDeUso": clean_text(it.get("gatilhoDeUso")), "tags": clean_text(it.get("tags")),
                            "fonte": f"gemini:{cfg.model}", "nivelConfianca": 0.6, "dataGeracao": now_ms(),
                        }
                        insert_resposta(con, rec)
                        existing[key].append(texto)
                        stats["inseridos"] += 1
                        stats["por_perfil"][perfil] += 1
                        stats["por_relacao"][rel_db] += 1
                    con.commit()
                cp.mark_done(ck, {"count": len(existing[key])})
                print(f"[behavior] {rel_db}/{subtema}/{perfil}: {len(existing[key])} (total {stats['inseridos']})")
    con.commit(); con.close()
    stats["por_perfil"] = dict(stats["por_perfil"]); stats["por_relacao"] = dict(stats["por_relacao"])
    return stats, jargon

def generate_academic(client: GeminiClient, cfg: Config, out_db: Path):
    cp = Checkpoint(CHECKPOINT_DIR / "academic.json")
    con = create_academic_db(out_db)
    area_cache, sub_cache, conc_cache = {}, {}, {}
    existing = defaultdict(list)  # subarea_id -> textos
    for row in con.execute("SELECT subareaId, texto FROM frase_explicativa"):
        existing[row[0]].append(row[1])
    stats = {"frases_inseridas": 0, "descartadas_dup": 0, "descartadas_invalidas": 0, "por_subarea": {}, "subareas_cobertas": 0, "etimologias": 0}
    jargon = []
    for area in ACADEMIC_AREAS:
        area_id = ensure_area(con, area.nome, area_cache)
        for sub in area.subareas:
            ck = f"a::{area.nome}::{sub.nome}"
            sub_id = ensure_subarea(con, area_id, sub.nome, sub_cache)
            conc_id = ensure_conceito(con, sub_id, f"Fundamentos de {sub.nome}", conc_cache)
            need = sub.frases_alvo - len(existing[sub_id])
            if need <= 0 or cp.is_done(ck):
                cp.mark_done(ck, {"count": len(existing[sub_id])})
                stats["por_subarea"][sub.nome] = len(existing[sub_id]); continue
            attempts = 0
            while len(existing[sub_id]) < sub.frases_alvo and attempts < 5:
                attempts += 1
                batch_n = min(20, sub.frases_alvo - len(existing[sub_id]) + 2)
                prompt = prompt_frases(area.nome, sub.nome, batch_n, sub.angulos, area.sensivel, existing[sub_id][-12:])
                try:
                    data = client.generate_json(prompt)
                except GeminiError as e:
                    print(f"[ERRO] {ck}: {e}")
                    if e.status in (401, 403): raise
                    break
                items = data if isinstance(data, list) else data.get("items") or data.get("frases") or []
                if not isinstance(items, list): items = []
                for it in items:
                    texto = clean_text((it or {}).get("texto"))
                    if not valid_min(texto, cfg.min_text_len):
                        stats["descartadas_invalidas"] += 1; continue
                    if is_near_dup(texto, existing[sub_id]):
                        stats["descartadas_dup"] += 1; continue
                    jf = jargon_flags(texto)
                    if jf: jargon.append(f"{sub.nome}: {jf}")
                    rec = {"angulo": clean_text(it.get("angulo")) or "definição", "texto": texto,
                           "fonte": f"gemini:{cfg.model}", "nivelConfianca": 0.6, "dataGeracao": now_ms()}
                    insert_frase(con, conc_id, sub_id, rec)
                    existing[sub_id].append(texto)
                    stats["frases_inseridas"] += 1
                con.commit()
            stats["por_subarea"][sub.nome] = len(existing[sub_id])
            if len(existing[sub_id]) >= sub.frases_alvo: stats["subareas_cobertas"] += 1
            cp.mark_done(ck, {"count": len(existing[sub_id])})
            print(f"[academic] {area.nome}/{sub.nome}: {len(existing[sub_id])} (total {stats['frases_inseridas']})")
    # etimologia
    et_ck = "etimologia::v1"
    existing_terms = {r[0].lower() for r in con.execute("SELECT termo FROM etimologia")}
    attempts = 0
    while len(existing_terms) < ETIMOLOGIA_ALVO and attempts < (ETIMOLOGIA_ALVO // 10 + 10) and not cp.is_done(et_ck):
        attempts += 1
        try:
            data = client.generate_json(prompt_etimologia(20, list(existing_terms)[-60:]))
        except GeminiError as e:
            print(f"[ERRO] etimologia: {e}")
            if e.status in (401, 403): raise
            break
        items = data if isinstance(data, list) else data.get("items") or data.get("etimologias") or []
        for it in items:
            termo = clean_text((it or {}).get("termo"))
            if not termo or termo.lower() in existing_terms: continue
            rec = {"termo": termo, "raizOriginal": clean_text(it.get("raizOriginal")),
                   "evolucaoFonetica": clean_text(it.get("evolucaoFonetica")),
                   "mudancaSemantica": clean_text(it.get("mudancaSemantica")),
                   "fonte": f"gemini:{cfg.model}", "nivelConfianca": 0.6, "dataGeracao": now_ms()}
            if not (rec["raizOriginal"] and rec["evolucaoFonetica"] and rec["mudancaSemantica"]): continue
            insert_etimologia(con, rec)
            existing_terms.add(termo.lower())
            stats["etimologias"] += 1
        con.commit()
        print(f"[etimologia] {len(existing_terms)}/{ETIMOLOGIA_ALVO}")
    if len(existing_terms) >= ETIMOLOGIA_ALVO: cp.mark_done(et_ck, {"count": len(existing_terms)})
    con.commit(); con.close()
    return stats, jargon

def package_assets(behavior_db: Path, academic_db: Path):
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    import shutil
    for src, name in [(behavior_db, "behavior.db"), (academic_db, "academic.db")]:
        if src.exists():
            dst = ASSETS_DIR / name
            shutil.copy2(src, dst)
            print(f"[assets] copiado: {dst} ({dst.stat().st_size/1024/1024:.2f} MB)")
        else:
            print(f"[assets] ausente, não copiado: {src}")
