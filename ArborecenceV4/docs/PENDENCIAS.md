# Pendências (Arborecence)

Itens fora do escopo da fase atual, sem placeholders no código entregue.

## Fase 1 (entregue: estrutura, schemas, tela Gemini, verificação de volume)
- Popular `behavior.db` com ≥ 3000 `resposta_relacionamento` distintas (ND/NT balanceados, tipos de relação balanceados), deduplicação por cosseno < 0,85 — pipeline `tools/gerador/`.
- Popular `academic.db` com ≥ 2500 `frase_explicativa` distintas (≥ 50 por subárea) e ≥ 300 termos em `etimologia` (raiz, evolução fonética, mudança semântica) — pipeline `tools/gerador/`.
- Cobrir todas as áreas acadêmicas obrigatórias e gerar `docs/COBERTURA.md`.
- Empacotar `.db` pré-populados em `app/src/main/assets/`.

## Próximas fases
- Fase 2: `tools/gerador/` (Python, lotes, JSON+schema, backoff, checkpoint, estimativa de custo/cota com confirmação, pipeline de qualidade e segurança para conteúdo sensível).
- Fase 3: Motor de IA offline (FTS5 + embeddings ONNX/TFLite), resposta composta (ND/NT, Curto/Padrão/Detalhado), sem repetição, benchmark LLM local e `docs/IA_LOCAL.md`.
- Fase 4: Voz (Quick Settings Tile, Foreground Service, STT/TTS offline pt-BR, descarregamento cognitivo, guia MIUI/bateria).
- Fase 5: Central de estudos (marca-texto 4 cores, revisão espaçada, mapa de conexões, linhas do tempo, hiperfoco, pomodoro adaptável, checklists).
- Fase 6: UI Liquid Glass + visionOS com acessibilidade (baixo estímulo, alto contraste, sem piscadas/sons inesperados, movimento reduzido, fonte ajustável).
- Fase 7: Privacidade/ética (SQLCipher/EncryptedFile, export/import JSON, plano de crise CVV 188/SAMU 192), WorkManager de aprendizado ~2 semanas, painel "O que o app aprendeu", testes de recuperação/DAOs e desempenho < 1,5 s.
- README completo (build, chave Gemini, uso) e APK debug para Redmi Note 11S.
