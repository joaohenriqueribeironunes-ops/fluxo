# Pendências (Arborecence)

Itens fora do escopo da fase atual, sem placeholders no código entregue.

## Fase 1 (entregue)
- Estrutura multi-módulo, `behavior.db`/`academic.db` (Room + FTS5 + schemas/migrações), tela "Conectar ao Gemini" (chave criptografada, teste 401/403/429, revogar), script `tools/verificar_volume`.

## Fase 2 (implementada — execução pendente de confirmação/cota)
- `tools/gerador/` implementado: lê chave só de `local.properties`; lotes por subtema/subárea; prompts estruturados com saída JSON; retentativas com backoff exponencial + limite de RPM; **checkpoint** retomável (`.checkpoints/`); **estimativa de chamadas/custo** com **confirmação obrigatória** (`SIM`/`--yes` após revisar) antes de consumir API; deduplicação (cosseno < 0,85), tamanho mínimo, `revisado=false`, relatório de cobertura (`reports/`); prompt de segurança para conteúdo sensível (informativo, sem diagnóstico, sem dose, sem plano prescritivo; farmacocinética só ADME geral); empacota `.db` em `app/src/main/assets/`.
- **Pendente (requer ação sua)**: instalar deps (`pip install -r tools/gerador/requirements.txt`), rodar `python tools/gerador/gerar.py --estimate-only`, revisar estimativa/cota e confirmar a execução para gerar/popular os bancos. (Neste ambiente não há runtime Python funcional; rode no seu computador.)
- Corrigir execução de testes unitários Android (Hilt ASM × classpath de teste no Windows) e ampliar testes de DAOs/recuperação.

## Próximas fases
- Popular e validar metas: `behavior.db` ≥ 3000 respostas distintas (ND/NT e 8 relações equilibrados, cosseno < 0,85) + `academic.db` ≥ 2500 `frase_explicativa` (≥ 50/subárea) + ≥ 300 `etimologia`; cobrir todas as áreas obrigatórias; gerar `docs/COBERTURA.md` final; empacotar `.db` em assets; `python tools/verificar_volume.py` passando.
- Fase 3: Motor de IA offline (FTS5 + embeddings ONNX/TFLite), resposta composta (ND/NT, Curto/Padrão/Detalhado), sem repetição, benchmark LLM local e `docs/IA_LOCAL.md`.
- Fase 4: Voz (Quick Settings Tile, Foreground Service, STT/TTS offline pt-BR, descarregamento cognitivo, guia MIUI/bateria).
- Fase 5: Central de estudos (marca-texto 4 cores, revisão espaçada, mapa de conexões, linhas do tempo, hiperfoco, pomodoro adaptável, checklists).
- Fase 6: UI Liquid Glass + visionOS com acessibilidade (baixo estímulo, alto contraste, sem piscadas/sons inesperados, movimento reduzido, fonte ajustável).
- Fase 7: Privacidade/ética (SQLCipher/EncryptedFile, export/import JSON, plano de crise CVV 188/SAMU 192), WorkManager de aprendizado ~2 semanas, painel "O que o app aprendeu", testes de recuperação/DAOs e desempenho < 1,5 s.
- README completo (build, chave Gemini, uso) e APK debug para Redmi Note 11S.
