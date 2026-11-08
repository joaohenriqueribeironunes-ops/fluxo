# Proxy Gemini (Cloudflare Worker)

Proxy mínimo para o app Equilíbrio. Ele guarda a chave da API Gemini no servidor, aplica limite de uso e devolve apenas o texto JSON produzido pelo modelo. O APK não contém nenhuma chave.

## Deploy

Pré-requisito: conta Cloudflare e [Wrangler](https://developers.cloudflare.com/workers/wrangler/).

```bash
cd server/cloudflare-worker
npm install -g wrangler
wrangler login

# 1. Chave da API Gemini (crie em https://aistudio.google.com/apikey)
wrangler secret put GEMINI_API_KEY

# 2. Deploy
wrangler deploy
```

O Wrangler mostra uma URL parecida com:

```
https://equilibrio-gemini-proxy.SEU-SUBDOMINIO.workers.dev
```

## Limite de uso (recomendado)

Sem limite, a URL pública pode ser usada por terceiros. Crie um namespace KV:

```bash
wrangler kv namespace create RATE_LIMIT
```

Copie o `id` retornado e descomente o bloco `[[kv_namespaces]]` em `wrangler.toml`. Depois faça o deploy novamente.

O Worker permite **30 requisições por IP por hora** quando o KV está configurado.

## Configurar no app

Em **Configurações → Análise com Gemini → URL do proxy**, cole a URL do Worker (sem barra no final). Alternativamente, defina `GEMINI_PROXY_URL` em `gradle.properties` antes do build:

```properties
GEMINI_PROXY_URL=https://equilibrio-gemini-proxy.SEU-SUBDOMINIO.workers.dev
```

O app só usa HTTPs (exceto `http://10.0.2.2` para desenvolvimento no emulador).

## Contrato

`POST /analyze`

```json
{
  "model": "gemini-2.5-flash",
  "system": "...",
  "prompt": "...",
  "temperature": 0.4,
  "responseSchema": { "type": "OBJECT", "properties": {} }
}
```

Resposta:

```json
{ "text": "{\"risco_atual\": 42, ...}", "model": "gemini-2.5-flash" }
```

## Segurança

- A chave fica em `GEMINI_API_KEY` (secret do Worker), nunca no código nem no APK.
- O Worker não registra o payload em logs por padrão.
- Para uso pessoal, considere restringir o Worker com Cloudflare Access ou um token compartilhado. Se adicionar autenticação, ajuste o header no `ProxyGeminiClient`.
- O limite por IP é uma proteção básica, não uma autenticação forte.
