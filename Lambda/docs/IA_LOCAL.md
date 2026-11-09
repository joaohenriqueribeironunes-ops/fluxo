# IA local — decisão: não usar

O Lambda **não usa LLM, embeddings, API ou qualquer modelo generativo**, nem no app nem na geração do banco.

## Como o conteúdo é produzido
- Catálogo curado manualmente em `tools/gerador/situacoes_curadas.py` (situações humanas detalhadas).
- Templates determinísticos em `tools/gerador/seed_local.py` para respostas de relacionamento e frases acadêmicas.
- Verificação por cosseno (< 0,85) no contexto real de cada célula.

## Como a resposta é escolhida no app
- Recuperação FTS5 no banco local embarcado.
- Composição determinística por perfil (ND/NT), modo (Curto/Padrão/Detalhado) e relação.
- Fonte e nível de confiança sempre exibidos.

## Por que não usar IA
- Privacidade e offline real (sem `INTERNET`).
- Previsibilidade e auditabilidade do conteúdo.
- Consistência em PT-BR e respeito às diretrizes éticas (sem diagnóstico, sem dose).
- Desempenho adequado ao Redmi Note 11S (Helio G96, 6/8 GB).

Uma eventual IA local só seria considerada se mantivesse o app 100% offline e passasse por benchmark de latência/memória; no momento, a decisão é **não usar**.
