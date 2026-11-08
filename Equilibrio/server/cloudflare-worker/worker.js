/**
 * Proxy opcional do Equilíbrio para a API Gemini.
 *
 * Contrato usado pelo app:
 *   POST /analyze
 *   { "model": "...", "system": "...", "prompt": "...",
 *     "temperature": 0.4, "responseSchema": { ... } }
 *
 * Resposta:
 *   { "text": "<JSON retornado pelo Gemini>" }
 *
 * A chave da API fica em `GEMINI_API_KEY` (secret do Worker) e nunca é
 * enviada ao APK. O Worker também aplica um limite simples por IP quando o
 * binding KV `RATE_LIMIT` está configurado.
 */

const DEFAULT_MODEL = "gemini-2.5-flash";
const MAX_BODY_BYTES = 256 * 1024;
const MAX_REQUESTS_PER_HOUR = 30;

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, X-Equilibrio-Client",
};

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      ...corsHeaders,
    },
  });
}

function error(message, status = 400) {
  return json({ error: message }, status);
}

async function checkRateLimit(request, env) {
  if (!env.RATE_LIMIT) return true;

  const ip =
    request.headers.get("CF-Connecting-IP") ||
    request.headers.get("X-Forwarded-For") ||
    "unknown";
  const hour = Math.floor(Date.now() / 3_600_000);
  const key = `rl:${ip}:${hour}`;

  const current = Number.parseInt((await env.RATE_LIMIT.get(key)) || "0", 10);
  if (current >= MAX_REQUESTS_PER_HOUR) return false;

  await env.RATE_LIMIT.put(key, String(current + 1), {
    expirationTtl: 7_200,
  });
  return true;
}

function extractText(data) {
  const candidates = data?.candidates;
  if (!Array.isArray(candidates) || candidates.length === 0) {
    const reason = data?.promptFeedback?.blockReason;
    throw new Error(
      reason ? `Conteúdo bloqueado pelo Gemini: ${reason}` : "Resposta vazia do Gemini."
    );
  }

  const parts = candidates[0]?.content?.parts;
  if (!Array.isArray(parts)) throw new Error("Resposta do Gemini sem partes de conteúdo.");

  return parts
    .map((part) => (typeof part?.text === "string" ? part.text : ""))
    .join("")
    .trim();
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    if (request.method !== "POST") {
      return error("Use POST /analyze.", 405);
    }

    const url = new URL(request.url);
    if (url.pathname !== "/analyze") {
      return error("Rota não encontrada.", 404);
    }

    if (!env.GEMINI_API_KEY) {
      return error("GEMINI_API_KEY não configurada no Worker.", 500);
    }

    const allowed = await checkRateLimit(request, env);
    if (!allowed) {
      return error("Limite de uso atingido. Tente novamente mais tarde.", 429);
    }

    const contentLength = Number(request.headers.get("Content-Length") || "0");
    if (contentLength > MAX_BODY_BYTES) {
      return error("Payload muito grande.", 413);
    }

    let body;
    try {
      const raw = await request.text();
      if (raw.length > MAX_BODY_BYTES) return error("Payload muito grande.", 413);
      body = JSON.parse(raw);
    } catch {
      return error("JSON inválido.", 400);
    }

    const system = typeof body.system === "string" ? body.system : "";
    const prompt = typeof body.prompt === "string" ? body.prompt : "";
    const responseSchema = body.responseSchema;
    const temperature = Number.isFinite(body.temperature)
      ? Math.min(Math.max(Number(body.temperature), 0), 1)
      : 0.4;

    if (!prompt.trim()) return error("Campo 'prompt' ausente.", 400);
    if (!responseSchema || typeof responseSchema !== "object") {
      return error("Campo 'responseSchema' ausente.", 400);
    }

    const requestedModel = typeof body.model === "string" ? body.model.trim() : "";
    const model = /^[a-zA-Z0-9._-]+$/.test(requestedModel)
      ? requestedModel
      : env.GEMINI_MODEL || DEFAULT_MODEL;

    const payload = {
      systemInstruction: system ? { parts: [{ text: system }] } : undefined,
      contents: [{ role: "user", parts: [{ text: prompt }] }],
      generationConfig: {
        temperature,
        responseMimeType: "application/json",
        responseSchema,
        maxOutputTokens: 4096,
      },
    };

    let upstream;
    try {
      upstream = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(
          model
        )}:generateContent?key=${encodeURIComponent(env.GEMINI_API_KEY)}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(payload),
        }
      );
    } catch {
      return error("Falha de rede ao chamar o Gemini.", 502);
    }

    const text = await upstream.text();
    if (!upstream.ok) {
      // Não repassa a URL/chave; só o suficiente para diagnóstico.
      return error(
        `Gemini respondeu ${upstream.status}: ${text.slice(0, 300)}`,
        502
      );
    }

    let data;
    try {
      data = JSON.parse(text);
    } catch {
      return error("Gemini devolveu uma resposta inválida.", 502);
    }

    try {
      const analysis = extractText(data);
      if (!analysis) return error("Gemini devolveu conteúdo vazio.", 502);
      return json({ text: analysis, model });
    } catch (e) {
      return error(e instanceof Error ? e.message : "Falha ao ler a resposta.", 502);
    }
  },
};
