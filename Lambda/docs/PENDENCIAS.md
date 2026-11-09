# Pendências (Lambda) — offline, sem API e sem IA

## Feito
- Estrutura/módulos enxutos: `app`, `core-ui`, `data-behavior`, `data-academic`, `ai-engine`. Módulos `voice` e `study` removidos do build.
- Room `behavior.db` (v2) + `academic.db` com FTS5, schemas/migrações e metadados.
- Catálogo curado `tools/gerador/situacoes_curadas.py`: **40 situações** com leitura ND (6 dimensões), leitura NT (4 dimensões) e ponte prática (scripts, fazer, evitar, explicar funcionamento).
- Seed local determinístico (`seed_local.py`) + verificador (`verificar_volume.py`). Resultado atual: **4.992 respostas distintas**, **2.900 frases acadêmicas**, **326 etimologias**, todas as metas atendidas.
- Motor offline FTS5, composição ND/NT/Curto-Padrão-Detalhado, anti-repetição, fonte + confiança sempre visíveis.
- Design system Liquid Glass/visionOS: `core-ui/design/Glass.kt` (aurora, cartões translúcidos, bordas especulares, dock flutuante) e `Theme.kt` (azul/turquesa/lavanda, claro/escuro/alto contraste, tipografia escalável).
- Telas em vidro: Tradutor (duas leituras + situações detalhadas), Estudos, Compromissos, Ajustes.
- **Descarregamento cognitivo + marca-texto 4 cores** (lavanda=conceito, menta=exemplo, âmbar=atenção/erro comum, água=dúvida) com salvar destaque; entrada por voz do sistema (STT intent, pt-BR); organização determinística em tarefas/ideias/emoções/dúvidas.
- Checklists de início de tarefa e apoio de hiperfoco (sem pomodoro).
- TTS offline (só quando o usuário toca em “Ouvir”), Quick Settings Tile.
- Acessibilidade: baixo estímulo, alto contraste, movimento reduzido, sem sons inesperados, fonte ajustável.
- Crise: CVV 188 / SAMU 192. Backup export/import JSON. Painel local com contagens do que está disponível.
- Gemini e qualquer API removidos: sem `INTERNET`, sem chaves, sem permissões de microfone/foreground service.
- DAOs com `Flow`, telas com `collectAsStateWithLifecycle`, ViewModels com `stateIn`/`combine`.

## Ação necessária (PC)
1. `python tools/gerador/seed_local.py`
2. `python tools/verificar_volume.py` (deve passar)
3. `.\gradlew.bat :app:assembleDebug`

## Restante (evolução)
- **Estudos**: revisão espaçada e linhas do tempo causais sobre os bancos locais.
- **Voz/STT**: fluxo contínuo com `SpeechRecognizer` e fallback; respeitar MIUI (ver `docs/MIUI_BATERIA.md`).
- **Criptografia reforçada**: SQLCipher ou `EncryptedFile` para bancos/exports sensíveis.
- **Aprendizado local**: padrões de uso (modos/relações/temas) sem API e sem telemetria.
- **Testes**: corrigir runner de testes unitários Android (Hilt ASM/classpath no Windows) e adicionar testes de DAOs/recuperação e acessibilidade.
- **Design**: refinar blur/camadas do Liquid Glass e criar ícone adaptativo próprio; manter fallback sólido em alto contraste/baixo estímulo.
- **Entrega**: APK release assinado, tag `v0.2.0`, release no repositório privado e teste no Redmi Note 11S.
