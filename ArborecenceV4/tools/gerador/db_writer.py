from __future__ import annotations
import json, sqlite3
from pathlib import Path
from typing import Iterable

from config import BEHAVIOR_SCHEMA, ACADEMIC_SCHEMA

FTS_BEHAVIOR = [
    "CREATE VIRTUAL TABLE IF NOT EXISTS resposta_relacionamento_fts USING fts5(texto, subtema, tags, content='resposta_relacionamento', content_rowid='id')",
    "CREATE TRIGGER IF NOT EXISTS rr_ai AFTER INSERT ON resposta_relacionamento BEGIN INSERT INTO resposta_relacionamento_fts(rowid, texto, subtema, tags) VALUES (new.id, new.texto, new.subtema, new.tags); END",
    "CREATE TRIGGER IF NOT EXISTS rr_ad AFTER DELETE ON resposta_relacionamento BEGIN INSERT INTO resposta_relacionamento_fts(resposta_relacionamento_fts, rowid, texto, subtema, tags) VALUES('delete', old.id, old.texto, old.subtema, old.tags); END",
    "CREATE TRIGGER IF NOT EXISTS rr_au AFTER UPDATE ON resposta_relacionamento BEGIN INSERT INTO resposta_relacionamento_fts(resposta_relacionamento_fts, rowid, texto, subtema, tags) VALUES('delete', old.id, old.texto, old.subtema, old.tags); INSERT INTO resposta_relacionamento_fts(rowid, texto, subtema, tags) VALUES (new.id, new.texto, new.subtema, new.tags); END",
]
FTS_ACADEMIC = [
    "CREATE VIRTUAL TABLE IF NOT EXISTS frase_explicativa_fts USING fts5(texto, angulo, content='frase_explicativa', content_rowid='id')",
    "CREATE TRIGGER IF NOT EXISTS fe_ai AFTER INSERT ON frase_explicativa BEGIN INSERT INTO frase_explicativa_fts(rowid, texto, angulo) VALUES (new.id, new.texto, new.angulo); END",
    "CREATE TRIGGER IF NOT EXISTS fe_ad AFTER DELETE ON frase_explicativa BEGIN INSERT INTO frase_explicativa_fts(frase_explicativa_fts, rowid, texto, angulo) VALUES('delete', old.id, old.texto, old.angulo); END",
    "CREATE TRIGGER IF NOT EXISTS fe_au AFTER UPDATE ON frase_explicativa BEGIN INSERT INTO frase_explicativa_fts(frase_explicativa_fts, rowid, texto, angulo) VALUES('delete', old.id, old.texto, old.angulo); INSERT INTO frase_explicativa_fts(rowid, texto, angulo) VALUES (new.id, new.texto, new.angulo); END",
    "CREATE VIRTUAL TABLE IF NOT EXISTS conceito_fts USING fts5(nome, definicaoCurta, tags, content='conceito', content_rowid='id')",
]

def _load_schema(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))

def _exec_schema_sql(con: sqlite3.Connection, sql: str, table_name: str):
    sql = sql.replace("${TABLE_NAME}", f"`{table_name}`").replace("${VIEW_NAME}", f"`{table_name}`")
    con.execute(sql)

def _apply_room_schema(con: sqlite3.Connection, schema: dict):
    db = schema["database"]
    for ent in db.get("entities", []):
        t = ent["tableName"]
        _exec_schema_sql(con, ent["createSql"], t)
        for idx in ent.get("indices", []):
            _exec_schema_sql(con, idx["createSql"], t)
    # views
    for v in db.get("views", []):
        _exec_schema_sql(con, v["createSql"], v.get("viewName", ""))
    for q in db.get("setupQueries", []):
        con.execute(q)

def create_behavior_db(path: Path) -> sqlite3.Connection:
    path.parent.mkdir(parents=True, exist_ok=True)
    con = sqlite3.connect(str(path))
    con.execute("PRAGMA journal_mode=DELETE")
    _apply_room_schema(con, _load_schema(BEHAVIOR_SCHEMA))
    for s in FTS_BEHAVIOR:
        con.execute(s)
    con.commit()
    return con

def create_academic_db(path: Path) -> sqlite3.Connection:
    path.parent.mkdir(parents=True, exist_ok=True)
    con = sqlite3.connect(str(path))
    con.execute("PRAGMA journal_mode=DELETE")
    _apply_room_schema(con, _load_schema(ACADEMIC_SCHEMA))
    for s in FTS_ACADEMIC:
        con.execute(s)
    con.commit()
    return con

def insert_resposta(con: sqlite3.Connection, r: dict):
    con.execute(
        """INSERT INTO resposta_relacionamento
        (relacaoTipo, subtema, perfilNdOuNt, texto, tom, tamanho, gatilhoDeUso, tags, fonte, nivelConfianca, dataGeracao, revisado)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""",
        (r["relacaoTipo"], r["subtema"], r["perfilNdOuNt"], r["texto"], r.get("tom","caloroso e direto"),
         r.get("tamanho","padrao"), r.get("gatilhoDeUso",""), r.get("tags",""),
         r.get("fonte","gemini-gerado"), r.get("nivelConfianca",0.6), r.get("dataGeracao",0), 0),
    )

def ensure_area(con: sqlite3.Connection, nome: str, cache: dict) -> int:
    if nome in cache: return cache[nome]
    row = con.execute("SELECT id FROM area WHERE nome=?", (nome,)).fetchone()
    if row:
        cache[nome] = row[0]; return row[0]
    con.execute("INSERT INTO area (nome, descricao, fonte, nivelConfianca, dataGeracao, revisado) VALUES (?,?,?,?,?,0)",
                (nome, "", "seed", 1.0, 0))
    rid = con.execute("SELECT id FROM area WHERE nome=?", (nome,)).fetchone()[0]
    cache[nome] = rid; return rid

def ensure_subarea(con: sqlite3.Connection, area_id: int, nome: str, cache: dict) -> int:
    key = (area_id, nome)
    if key in cache: return cache[key]
    row = con.execute("SELECT id FROM subarea WHERE areaId=? AND nome=?", (area_id, nome)).fetchone()
    if row:
        cache[key] = row[0]; return row[0]
    con.execute("INSERT INTO subarea (areaId, nome, descricao, fonte, nivelConfianca, dataGeracao, revisado) VALUES (?,?,?,?,?,0)",
                (area_id, nome, "", "seed", 1.0, 0))
    rid = con.execute("SELECT id FROM subarea WHERE areaId=? AND nome=?", (area_id, nome)).fetchone()[0]
    cache[key] = rid; return rid

def ensure_conceito(con: sqlite3.Connection, subarea_id: int, nome: str, cache: dict) -> int:
    key = (subarea_id, nome)
    if key in cache: return cache[key]
    row = con.execute("SELECT id FROM conceito WHERE subareaId=? AND nome=?", (subarea_id, nome)).fetchone()
    if row:
        cache[key] = row[0]; return row[0]
    con.execute("INSERT INTO conceito (subareaId, nome, definicaoCurta, tags, fonte, nivelConfianca, dataGeracao, revisado) VALUES (?,?,?,?,?,?,?,0)",
                (subarea_id, nome, "", "", "seed", 0.5, 0))
    rid = con.execute("SELECT id FROM conceito WHERE subareaId=? AND nome=?", (subarea_id, nome)).fetchone()[0]
    cache[key] = rid; return rid

def insert_frase(con: sqlite3.Connection, conceito_id: int, subarea_id: int, r: dict):
    con.execute(
        """INSERT INTO frase_explicativa
        (conceitoId, subareaId, angulo, texto, fonte, nivelConfianca, dataGeracao, revisado)
        VALUES (?,?,?,?,?,?,?,0)""",
        (conceito_id, subarea_id, r.get("angulo","definição"), r["texto"],
         r.get("fonte","gemini-gerado"), r.get("nivelConfianca",0.6), r.get("dataGeracao",0)),
    )

def insert_etimologia(con: sqlite3.Connection, r: dict):
    con.execute(
        """INSERT INTO etimologia (termo, raizOriginal, evolucaoFonetica, mudancaSemantica, fonte, nivelConfianca, dataGeracao, revisado)
        VALUES (?,?,?,?,?,?,?,0)""",
        (r["termo"], r.get("raizOriginal",""), r.get("evolucaoFonetica",""), r.get("mudancaSemantica",""),
         r.get("fonte","gemini-gerado"), r.get("nivelConfianca",0.6), r.get("dataGeracao",0)),
    )

def seed_relacao_tipos(con: sqlite3.Connection):
    for nome in ["amizade","amor","família","trabalho","colegas","vizinhos","desconhecidos","atendimento"]:
        con.execute("INSERT OR IGNORE INTO relacao_tipo (nome, descricao, fonte, nivelConfianca, dataGeracao, revisado) VALUES (?,?, 'seed', 1.0, 0, 0)", (nome, ""))
    con.commit()
