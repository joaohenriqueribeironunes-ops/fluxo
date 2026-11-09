#!/usr/bin/env python3
"""Verifica metas de volume dos bancos Lambda.

- behavior.db: >= 3000 respostas distintas em resposta_relacionamento,
  divididas igualmente entre ND/NT e entre tipos de relação.
  Distintas = sem duplicatas/quase-duplicatas (cosseno < 0,85 no mesmo subtema).
- academic.db: >= 2500 frases_explicativa distintas, >= 50 por subárea.
  (Falha se houver subáreas com frases e alguma tiver < 50.)

Uso:
  python tools/verificar_volume.py [--behavior PATH] [--academic PATH] [--base DIR]

Sai com código 1 se alguma meta falhar; 0 se passar.
"""
from __future__ import annotations
import argparse, math, os, re, sqlite3, sys
from collections import Counter, defaultdict

COSINE_MAX = 0.85  # >= 0.85 => quase-duplicata

def tokenize(text: str):
    return re.findall(r"[\wÀ-ÿ]+", (text or "").lower())

def cosine(a: str, b: str) -> float:
    ca, cb = Counter(tokenize(a)), Counter(tokenize(b))
    if not ca or not cb: return 0.0
    dot = sum(ca[k] * cb.get(k, 0) for k in ca)
    na = math.sqrt(sum(v*v for v in ca.values()))
    nb = math.sqrt(sum(v*v for v in cb.values()))
    return (dot / (na * nb)) if na and nb else 0.0

def dedup_texts(rows, text_key="texto", group_key=None):
    """Retorna (total, distintos, quase_duplicatas). Dedup por cosseno dentro do grupo."""
    groups = defaultdict(list)
    for r in rows:
        g = r[group_key] if group_key else "_all"
        groups[g].append(r[text_key] or "")
    distinct = 0
    dups = 0
    for _g, texts in groups.items():
        kept = []
        for t in texts:
            # otimização: compara só com kept; bucket por tamanho
            is_dup = False
            lt = len(t)
            for k in kept:
                if abs(len(k) - lt) > max(40, int(0.4 * max(lt, len(k), 1))):
                    continue
                if cosine(t, k) >= COSINE_MAX:
                    is_dup = True; break
            if is_dup: dups += 1
            else: kept.append(t); distinct += 1
    return len(rows), distinct, dups

def find_db(base: str, name: str, explicit: str | None):
    if explicit and os.path.exists(explicit): return explicit
    cands = [
        os.path.join(base, "app", "src", "main", "assets", name),
        os.path.join(base, "assets", name),
        os.path.join(base, name),
        os.path.join(base, "build", name),
    ]
    for c in cands:
        if os.path.exists(c): return c
    return None

def check_behavior(path: str | None):
    ok = True
    print("\n== behavior.db (resposta_relacionamento) ==")
    if not path:
        print("FALHA: behavior.db não encontrado (esperado em app/src/main/assets/behavior.db). Meta: >= 3000.")
        return False
    print(f"DB: {path}")
    con = sqlite3.connect(path)
    con.row_factory = sqlite3.Row
    try:
        cur = con.execute("SELECT id, relacaoTipo, subtema, perfilNdOuNt, texto FROM resposta_relacionamento")
        rows = [dict(r) for r in cur.fetchall()]
    except Exception as e:
        print(f"FALHA: não foi possível ler resposta_relacionamento: {e}")
        return False
    finally:
        con.close()
    total, distinct, dups = dedup_texts(rows, "texto", "subtema")
    print(f"Total: {total} | Distintas (cosseno<{COSINE_MAX} no subtema): {distinct} | Quase-duplicatas: {dups}")
    if distinct < 3000:
        print(f"FALHA: meta >= 3000 respostas distintas; encontrado {distinct}.")
        ok = False
    nd = sum(1 for r in rows if (r.get('perfilNdOuNt') or '').upper() == 'ND')
    nt = sum(1 for r in rows if (r.get('perfilNdOuNt') or '').upper() == 'NT')
    print(f"Perfil ND: {nd} | NT: {nt} | outros: {total-nd-nt}")
    if total > 0:
        # "divididas igualmente" - tolerância razoável (45-55%)
        for label, n in [("ND", nd), ("NT", nt)]:
            pct = 100.0 * n / total
            if pct < 45.0 or pct > 55.0:
                print(f"FALHA: distribuição ND/NT desequilibrada ({label}={pct:.1f}%, esperado ~50%).")
                ok = False
    rel = Counter(r.get('relacaoTipo') or '?' for r in rows)
    print(f"Tipos de relação ({len(rel)}): {dict(rel)}")
    if rel and total > 0:
        expected = total / max(len(rel), 1)
        for k, v in rel.items():
            if v < expected * 0.5 or v > expected * 1.5:
                print(f"FALHA: tipo '{k}' desbalanceado ({v}, esperado ~{expected:.0f}).")
                ok = False; break
    if ok: print("OK: metas de behavior atendidas.")
    return ok

def check_academic(path: str | None):
    ok = True
    print("\n== academic.db (frase_explicativa) ==")
    if not path:
        print("FALHA: academic.db não encontrado. Meta: >= 2500 frases, >= 50 por subárea.")
        return False
    print(f"DB: {path}")
    con = sqlite3.connect(path)
    con.row_factory = sqlite3.Row
    try:
        rows = [dict(r) for r in con.execute("SELECT id, conceitoId, subareaId, angulo, texto FROM frase_explicativa").fetchall()]
    except Exception as e:
        print(f"FALHA: não foi possível ler frase_explicativa: {e}")
        return False
    finally:
        con.close()
    total, distinct, dups = dedup_texts(rows, "texto", "subareaId")
    print(f"Total: {total} | Distintas (cosseno<{COSINE_MAX} por subárea): {distinct} | Quase-duplicatas: {dups}")
    if distinct < 2500:
        print(f"FALHA: meta >= 2500 frases distintas; encontrado {distinct}.")
        ok = False
    by_sub = defaultdict(list)
    for r in rows: by_sub[r.get('subareaId')].append(r)
    bad = []
    for sid, srows in by_sub.items():
        _t, sd, _d = dedup_texts(srows, "texto", None)
        if sd < 50: bad.append((sid, sd, len(srows)))
    if by_sub:
        print(f"Subáreas com frases: {len(by_sub)}")
        if bad:
            print(f"FALHA: {len(bad)} subárea(s) com < 50 frases distintas: {bad[:10]}{'...' if len(bad)>10 else ''}")
            ok = False
        else:
            print("OK: todas as subáreas com frases têm >= 50 distintas.")
    elif total > 0:
        pass
    if ok and distinct >= 2500: print("OK: metas acadêmicas atendidas.")
    return ok

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--behavior')
    ap.add_argument('--academic')
    ap.add_argument('--base', default=os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    args = ap.parse_args()
    b = find_db(args.base, 'behavior.db', args.behavior)
    a = find_db(args.base, 'academic.db', args.academic)
    ok_b = check_behavior(b)
    ok_a = check_academic(a)
    if ok_b and ok_a:
        print("\nRESULTADO: PASSOU (todas as metas atendidas).")
        return 0
    print("\nRESULTADO: FALHOU (metas de volume não atendidas). Gere/importe o conteúdo e rode novamente.")
    return 1

if __name__ == '__main__':
    raise SystemExit(main())
