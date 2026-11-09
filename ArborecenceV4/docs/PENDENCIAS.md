# Pendências (Arborecence) — direção: **offline, sem API do Gemini**

## Feito (compila)
- Estrutura/módulos, Room `behavior.db` (v2, migração 1→2) + `academic.db` com FTS5, schemas/migrações, metadados.
- Seed local **sem API** (`tools/gerador/seed_local.py`, schema v2) + verificador de volume (`tools/verificar_volume.*`).
- Motor offline FTS5, composição ND/NT/Curto-Padrão-Detalhado, anti-repetição, **fonte+confiança** sempre.
- UI: Tradutor ND/NT, Estudos (busca/etimologia), **Compromissos** (marcar/salvar/concluir/remover), Ajustes.
- **Descarregamento cognitivo + marca-texto 4 cores** (lavanda=conceito, menta=exemplo, âmbar=atenção/erro comum, água=dúvida) com salvar destaque; entrada por **voz (STT intent, pt-BR)**; organização tarefas/ideias/emoções/dúvidas.
- **Checklists de início de tarefa** e apoio de **hiperfoco** (sem pomodoro).
- TTS offline (respeita “sem sons inesperados”), Quick Settings Tile, `VoiceAssistService` declarado.
- Acessibilidade: baixo estímulo, alto contraste, movimento reduzido, sem sons inesperados, fonte ajustável.
- Crise: CVV 188 / SAMU 192. Backup **export/import JSON** (compromissos/destaques/perfil resumo). Painel “o que o app aprendeu/tem disponível” (local).
- Gemini opcional/não bloqueante; chave só em `local.properties` (gitignored) / criptografada no app.

## Ação necessária (seu PC)
1. `python tools/gerador/seed_local.py`  2. `python tools/verificar_volume.py` (deve passar as metas)  3. `.\gradlew.bat :app:assembleDebug`.

## Restante (evolução)
- **Validar metas de volume** no seu PC e ajustar seed se o verificador apontar (áreas/subtemas específicos).
- **Estudos**: revisão espaçada, mapa de conexões, linhas do tempo causais (a partir dos bancos) — implementar persistência/algoritmo.
- **Voz/STT offline robusto**: fluxo completo de descarregamento por voz contínua com SpeechRecognizer/Vosk fallback; foreground service em uso ativo; respeitar MIUI (guia em `docs/MIUI_BATERIA.md`).
- **Criptografia reforçada**: SQLCipher ou EncryptedFile para bancos/exports sensíveis (avaliar custo/desempenho no alvo).
- **Aprendizado local 100%**: rastrear padrões de uso (modos/relações/temas) sem API, WorkManager e painel detalhado de lacunas/progresso.
- **Testes**: corrigir runner de testes unitários Android (Hilt ASM/classpath no Windows) e adicionar testes de DAOs/recuperação (<1,5 s no alvo) e casos de acessibilidade.
- **Design**: refinar Liquid Glass/visionOS (blur/camadas/ícone iOS 26) com fallback sólido e respeito a baixo estímulo/movimento reduzido.
- **Entrega**: gerar `docs/COBERTURA.md` final pós-seed, README/guia de instalação no Redmi, APK debug testado no aparelho.
