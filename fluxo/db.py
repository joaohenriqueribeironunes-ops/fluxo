"""Persistência local-first em SQLite — mesmas tabelas do app web (Dexie/IndexedDB)."""

import json
import os
import sqlite3
import threading
import time

SCHEMA = """
CREATE TABLE IF NOT EXISTS ideas (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  text TEXT NOT NULL,
  mode TEXT NOT NULL,
  result TEXT NOT NULL,
  created INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS articles (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  url TEXT NOT NULL,
  text TEXT NOT NULL,
  saved_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS appointments (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  "when" INTEGER NOT NULL,
  remind_before_min INTEGER NOT NULL DEFAULT 0,
  notified INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS tracks (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  title TEXT NOT NULL,
  path TEXT NOT NULL,
  duration_sec REAL NOT NULL DEFAULT 0,
  added_at INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS kv (
  key TEXT PRIMARY KEY,
  value TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS ideas_created ON ideas (created DESC);
CREATE INDEX IF NOT EXISTS tracks_added ON tracks (added_at DESC);
CREATE INDEX IF NOT EXISTS appointments_when ON appointments ("when");
"""


def now_ms():
    return int(time.time() * 1000)


class FluxoDB:
    def __init__(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self._lock = threading.RLock()
        self._conn = sqlite3.connect(path, check_same_thread=False)
        self._conn.row_factory = sqlite3.Row
        with self._lock:
            self._conn.executescript(SCHEMA)
            self._conn.commit()

    def close(self):
        with self._lock:
            self._conn.close()

    def _rows(self, sql, params=()):
        with self._lock:
            return [dict(r) for r in self._conn.execute(sql, params).fetchall()]

    def _run(self, sql, params=()):
        with self._lock:
            cur = self._conn.execute(sql, params)
            self._conn.commit()
            return cur.lastrowid

    # settings
    def get_setting(self, key, fallback):
        rows = self._rows("SELECT value FROM kv WHERE key = ?", (key,))
        if not rows:
            return fallback
        try:
            return json.loads(rows[0]["value"])
        except (TypeError, ValueError):
            return fallback

    def set_setting(self, key, value):
        self._run(
            "INSERT INTO kv (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value",
            (key, json.dumps(value)),
        )

    # ideias
    def add_idea(self, text, mode, result):
        return self._run(
            "INSERT INTO ideas (text, mode, result, created) VALUES (?, ?, ?, ?)",
            (text, mode, result, now_ms()),
        )

    def recent_ideas(self, limit=12):
        return self._rows("SELECT * FROM ideas ORDER BY created DESC LIMIT ?", (limit,))

    def delete_idea(self, idea_id):
        self._run("DELETE FROM ideas WHERE id = ?", (idea_id,))

    # acervo
    def add_article(self, title, url, text):
        return self._run(
            "INSERT INTO articles (title, url, text, saved_at) VALUES (?, ?, ?, ?)",
            (title, url, text, now_ms()),
        )

    def articles(self):
        return self._rows("SELECT * FROM articles ORDER BY saved_at DESC")

    def delete_article(self, article_id):
        self._run("DELETE FROM articles WHERE id = ?", (article_id,))

    # compromissos
    def add_appointment(self, title, when_ms, remind_before_min):
        return self._run(
            'INSERT INTO appointments (title, "when", remind_before_min, notified) VALUES (?, ?, ?, 0)',
            (title, when_ms, remind_before_min),
        )

    def upcoming_appointments(self, hours=24):
        since = now_ms() - hours * 3_600_000
        return self._rows(
            'SELECT * FROM appointments WHERE "when" >= ? ORDER BY "when" ASC', (since,)
        )

    def due_appointments(self):
        return self._rows(
            'SELECT * FROM appointments WHERE notified = 0 AND "when" <= ?', (now_ms(),)
        )

    def mark_notified(self, appointment_id):
        self._run("UPDATE appointments SET notified = 1 WHERE id = ?", (appointment_id,))

    def delete_appointment(self, appointment_id):
        self._run("DELETE FROM appointments WHERE id = ?", (appointment_id,))

    # faixas
    def add_track(self, title, path, duration_sec=0):
        return self._run(
            "INSERT INTO tracks (title, path, duration_sec, added_at) VALUES (?, ?, ?, ?)",
            (title, path, duration_sec, now_ms()),
        )

    def tracks(self):
        return self._rows("SELECT * FROM tracks ORDER BY added_at DESC")

    def delete_track(self, track_id):
        self._run("DELETE FROM tracks WHERE id = ?", (track_id,))

    def wipe(self):
        with self._lock:
            for table in ("ideas", "articles", "appointments", "tracks", "kv"):
                self._conn.execute(f"DELETE FROM {table}")
            self._conn.commit()
