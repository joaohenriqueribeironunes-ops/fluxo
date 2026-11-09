# Lambda

Assistente pessoal neurodivergente (Android, Kotlin + Jetpack Compose, Material3). PT-BR.  
Funciona **100% offline** por padrão — **sem depender da API do Gemini**.

## O que faz
- **Tradutor social (ND ↔ NT)**: para cada situação, mostra duas leituras — como uma pessoa **ND** tende a perceber/sentir e como uma pessoa **NT** tende a interpretar. Nenhuma é “certa”: são traduções. Linguagem literal, “o que dizer/fazer”, sem infantilizar/julgar. Mostra **fonte** e **nível de confiança**.
- **Modos**: Curto, Padrão, Detalhado. Relações: amizade, amor, família, trabalho, colegas, vizinhos, desconhecidos, atendimento.
- **Central de estudos**: busca offline (FTS5) em frases explicativas e etimologia; descarregamento cognitivo (separa tarefas/ideias/emoções/dúvidas).
- **Acessibilidade neurodivergente**: baixo estímulo, alto contraste, movimento reduzido, sem sons inesperados (padrões seguros), fonte ajustável, navegação previsível.
- **Voz/TTS**: leitura em voz alta (TTS offline pt-BR) quando sons não estiverem bloqueados; Quick Settings Tile; guia MIUI.
- **Privacidade**: local por padrão, sem telemetria. Chave Gemini (opcional) em armazenamento criptografado no aparelho. Apoio, não substitui profissional. Risco imediato: **CVV 188** / **SAMU 192**.

## Requisitos
- JDK 17, Android SDK (API 35/34). minSdk 26, targetSdk 34. Alvo: Redmi Note 11S.

## Popular os bancos (obrigatório, **sem API**)
Os schemas Room existem, mas o conteúdo precisa ser gerado localmente e verificado:
```powershell
cd Lambda
python tools/gerador/seed_local.py
python tools/verificar_volume.py
```
Metas: `behavior.db` ≥ 3000 `resposta_relacionamento` distintas (ND/NT e 8 relações equilibrados, cosseno < 0,85); `academic.db` ≥ 2500 `frase_explicativa` (≥ 50/subárea) + ≥ 300 `etimologia`. Os `.db` são copiados para `app/src/main/assets/`.

## Build e testes
```powershell
cd Lambda
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:compileDebugKotlin
```
APK debug: `app/build/outputs/apk/debug/`.

## Gemini (opcional, desativado)
O app **não precisa** do Gemini. A tela “Conectar Gemini” existe apenas como opção (chave criptografada, teste 401/403/429, revogar). O pipeline `tools/gerador/gerar.py` (com API) está presente mas **não é o caminho recomendado**; use `seed_local.py`.

## Estrutura
`app`, `core-ui`, `data-behavior` (`behavior.db` + FTS5), `data-academic` (`academic.db` + FTS5), `ai-engine` (recuperação/composição offline), `voice`, `study`, `tools/` (seed local, verificação de volume).

## Documentação
- `docs/PENDENCIAS.md` — itens em evolução
- `docs/COBERTURA.md` — áreas (gerado após seed completo)
- `docs/IA_LOCAL.md` — benchmark/decisão LLM local
- `docs/MIUI_BATERIA.md` — permissões/bateria no MIUI

## Ética
Sem diagnóstico, sem dose, sem plano prescritivo. Conteúdo sensível é informativo. ND/NT sem certo/errado.