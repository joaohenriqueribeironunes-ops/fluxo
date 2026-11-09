from __future__ import annotations
import json, time
from pathlib import Path
from typing import Any

class Checkpoint:
    def __init__(self, path: Path):
        self.path = path
        self.data: dict[str, Any] = {"version": 1, "updated": 0, "done": {}, "stats": {}}
        if path.exists():
            try:
                self.data = json.loads(path.read_text(encoding="utf-8"))
            except Exception:
                self.data = {"version": 1, "updated": 0, "done": {}, "stats": {}, "corrupt_backup": True}

    def is_done(self, key: str) -> bool:
        return bool(self.data.get("done", {}).get(key))

    def mark_done(self, key: str, meta: dict | None = None):
        self.data.setdefault("done", {})[key] = {"ts": int(time.time()), **(meta or {})}
        self.save()

    def set_stat(self, k: str, v: Any):
        self.data.setdefault("stats", {})[k] = v
        self.save()

    def save(self):
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.data["updated"] = int(time.time())
        tmp = self.path.with_suffix(".tmp")
        tmp.write_text(json.dumps(self.data, ensure_ascii=False, indent=2), encoding="utf-8")
        tmp.replace(self.path)
