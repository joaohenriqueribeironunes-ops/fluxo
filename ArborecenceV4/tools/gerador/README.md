# Gerador (Fase 2)

Pipeline de geração em massa usando a API do Gemini. A chave é lida **somente** de `../../local.properties` (ou `ArborecenceV4/local.properties`) — nunca hardcoded.

Planejado: lotes por subárea/subtema, JSON validado por schema, retentativas com backoff, checkpoint (retomar), estimativa de custo/cota com confirmação, deduplicação, marcação `revisado=false`, relatório de cobertura, prompt de segurança para conteúdo sensível. Saída empacotada como `.db` para `app/src/main/assets/`.
