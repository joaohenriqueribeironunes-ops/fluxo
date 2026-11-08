# Arborecence (FASE 1)

Assistente pessoal neurodivergente (Android, Kotlin + Jetpack Compose, Material3). PT-BR.

## Estado atual (FASE 1)
- Estrutura multi-módulo: `app`, `core-ui`, `data-behavior`, `data-academic`, `ai-engine`, `voice`, `study`.
- Room com dois bancos separados e schemas exportáveis:
  - `behavior.db`: situações, leituras ND/NT, ponte, tipos de relação, `resposta_relacionamento`, emoção, alexitimia, sensorial, função executiva, perfil do usuário. Índice FTS5 em `resposta_relacionamento`.
  - `academic.db`: áreas/subáreas/conceitos, explicações por nível, `frase_explicativa` (com `angulo`), exemplos, analogias, erros comuns, exercícios, glossário, etimologia, normas técnicas, referências. Índice FTS5 em `frase_explicativa`.
  - Metadados: `fonte`, `nivel_confianca`, `data_geracao`, `revisado`. Migrações estruturadas.
- Tela de primeira execução **Conectar ao Gemini**: chave em `EncryptedSharedPreferences` (Keystore), testar conexão (erros 401/403/429 tratados com mensagem clara), revogar/apagar, usar offline. Sem chave no código/logs/assets.
- Script `tools/verificar_volume.py` (`.bat`/`.ps1`): conta, deduplica (cosseno < 0,85) e **falha** se metas não forem atingidas.

## Requisitos
- JDK 17, Android SDK (API 35/34), minSdk 26, targetSdk 34.
- Dispositivo alvo: Redmi Note 11S (Android 13/MIUI).

## Configuração da chave do Gemini (seguro)
- **App**: colar/trocar na tela "Conectar ao Gemini" (armazenamento criptografado). Nunca versionar.
- **Pipeline de geração (`tools/gerador/`, fases seguintes)**: criar `local.properties` na raiz do projeto com `GEMINI_API_KEY=...`. O arquivo é ignorado pelo git. **Nunca** commitar.

## Build
```powershell
cd ArborecenceV4
.\gradlew.bat :app:assembleDebug
.\gradlew.bat test
```

## Verificação de volume (obrigatória quando os bancos forem gerados)
```powershell
python tools/verificar_volume.py
# ou
.\gradlew.bat verifyVolume
```
Metas: `behavior.db` ≥ 3000 `resposta_relacionamento` distintas (ND/NT e relações equilibrados); `academic.db` ≥ 2500 `frase_explicativa` distintas (≥ 50 por subárea). Sai com erro se falhar.

## Pendências
Ver `docs/PENDENCIAS.md`.
