from __future__ import annotations
import json, random, re, sys, time
from typing import Any
from config import Config
from rate_limit import RateLimiter

class GeminiError(Exception):
    def __init__(self, message: str, status: int | None = None):
        super().__init__(message)
        self.status = status

def _extract_json(text: str) -> Any:
    text = (text or "").strip()
    # strip markdown fences
    m = re.search(r"```(?:json)?\s*(\{.*\}|\[.*\])\s*```", text, re.S | re.I)
    if m:
        text = m.group(1).strip()
    # try direct parse
    try:
        return json.loads(text)
    except Exception:
        pass
    # find first { ... } or [ ... ]
    for open_c, close_c in [('{', '}'), ('[', ']')]:
        i = text.find(open_c)
        j = text.rfind(close_c)
        if i != -1 and j > i:
            try:
                return json.loads(text[i:j+1])
            except Exception:
                continue
    raise GeminiError("Resposta não é JSON válido.")

class GeminiClient:
    def __init__(self, api_key: str, cfg: Config):
        self.cfg = cfg
        self.limiter = RateLimiter(cfg.rpm_limit)
        try:
            import google.generativeai as genai
        except Exception as e:
            print("ERRO: instale dependências: pip install -r tools/gerador/requirements.txt", file=sys.stderr)
            raise
        genai.configure(api_key=api_key)
        self._genai = genai
        self.model = genai.GenerativeModel(
            cfg.model,
            generation_config={
                "temperature": 0.7,
                "top_p": 0.95,
                "max_output_tokens": 8192,
                "response_mime_type": "application/json",
            },
        )

    def generate_json(self, prompt: str) -> Any:
        last_err: Exception | None = None
        for attempt in range(1, self.cfg.max_retries + 1):
            self.limiter.wait()
            try:
                resp = self.model.generate_content(prompt)
                text = getattr(resp, "text", None) or ""
                if not text and getattr(resp, "candidates", None):
                    # best-effort extraction
                    parts = resp.candidates[0].content.parts
                    text = "".join(getattr(p, "text", "") for p in parts)
                return _extract_json(text)
            except GeminiError:
                raise
            except Exception as e:
                last_err = e
                msg = str(e)
                status = None
                if "401" in msg or "API_KEY_INVALID" in msg or "UNAUTHENTICATED" in msg:
                    raise GeminiError("Chave inválida/sem permissão (401/403). Atualize GEMINI_API_KEY em local.properties.", 401)
                if "403" in msg or "PERMISSION_DENIED" in msg:
                    raise GeminiError("Permissão negada (403). Verifique a chave/cota no Google AI Studio.", 403)
                if "429" in msg or "RESOURCE_EXHAUSTED" in msg or "quota" in msg.lower():
                    status = 429
                if "503" in msg or "UNAVAILABLE" in msg:
                    status = 503
                if status in (429, 503) or attempt < self.cfg.max_retries:
                    sleep_for = min(self.cfg.backoff_max, self.cfg.backoff_base ** attempt) + random.uniform(0, 0.5)
                    print(f"[retry {attempt}/{self.cfg.max_retries}] {msg[:160]} -> aguardando {sleep_for:.1f}s")
                    time.sleep(sleep_for)
                    continue
                raise GeminiError(f"Falha após {attempt} tentativas: {msg}")
        raise GeminiError(f"Falha: {last_err}")
