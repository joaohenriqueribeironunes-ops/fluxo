# Lambda

Assistente pessoal neurodivergente (Android, Kotlin + Jetpack Compose). PT-BR.  
Funciona **100% offline**, sem API, sem chave e sem conteúdo gerado por IA.

## O que faz
- **Tradutor social (ND ↔ NT)**: para cada situação, mostra duas leituras — como uma pessoa **ND** tende a perceber/sentir e como uma pessoa **NT** tende a interpretar. Nenhuma é “certa”: são traduções. Linguagem literal, “o que dizer/fazer”, sem infantilizar nem julgar. Fonte e confiança visíveis.
- **Situações da realidade humana**: catálogo curado manualmente com 40 situações detalhadas, cada uma com leitura ND (6 dimensões), leitura NT (4 dimensões) e ponte prática (scripts, o que fazer, o que evitar, como explicar seu funcionamento).
- **Modos**: Curto, Padrão, Detalhado. Relações: amizade, amor, família, trabalho, colegas, vizinhos, desconhecidos, atendimento.
- **Central de estudos**: busca offline (FTS5) em 2.900 frases explicativas e 326 etimologias; descarregamento cognitivo (separa tarefas/ideias/emoções/dúvidas).
- **Acessibilidade neurodivergente**: baixo estímulo, alto contraste, movimento reduzido, sem sons inesperados, fonte ajustável e navegação previsível.
- **Voz/TTS**: leitura em voz alta (TTS offline pt-BR) apenas quando você toca em “Ouvir”; ditado por intenção de reconhecimento de voz do sistema.
- **Privacidade**: local por padrão, sem telemetria e sem internet. Compromissos, marca-textos e preferências ficam só no aparelho. Apoio, não substitui profissional. Risco imediato: **CVV 188** / **SAMU 192**.

## Design
Interface própria inspirada em **iOS 26 Liquid Glass + visionOS**: fundo aurora em azul/turquesa, cartões translúcidos com bordas especulares, cantos arredondados, profundidade espacial e dock flutuante. O modo de alto contraste substitui o vidro por superfícies sólidas; o modo de baixo estímulo reduz saturação e efeitos.

## Requisitos
- JDK 17, Android SDK (API 35/34). minSdk 26, targetSdk 34. Alvo: Redmi Note 11S.

## Popular os bancos (obrigatório, **sem API**)
Os schemas Room existem, mas o conteúdo é gerado localmente por catálogo/templates determinísticos e verificado:
```powershell
cd Lambda
python tools/gerador/seed_local.py
python tools/verificar_volume.py
```
Metas atuais (todas verificadas):
- `behavior.db`: **≥ 3.000** respostas distintas (cosseno < 0,85 por relação + subtema + perfil), ND/NT e 8 relações equilibrados; **≥ 40 situações detalhadas** com leitura ND, leitura NT e ponte completas.
- `academic.db`: **≥ 2.500** frases explicativas distintas (**≥ 50 por subárea**, 58 subáreas) + **≥ 300** etimologias.
- Os `.db` são copiados para `app/src/main/assets/`.

## Build e testes
```powershell
cd Lambda
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease   # APK assinado (keystore local, ver docs/ASSINATURA.md)
.\gradlew.bat :app:compileDebugKotlin
```
APK debug: `app/build/outputs/apk/debug/` · APK release assinado: `app/build/outputs/apk/release/`.

## Estrutura
`app`, `core-ui` (design system Liquid Glass), `data-behavior` (`behavior.db` + FTS5), `data-academic` (`academic.db` + FTS5), `ai-engine` (recuperação/composição offline determinística), `tools/` (catálogo curado, seed local, verificação de volume).

## Documentação
- `tools/gerador/README.md` — catálogo curado e seed local (sem API)
- `docs/PENDENCIAS.md` — itens em evolução
- `docs/ASSINATURA.md` — keystore e build de release assinada
- `docs/COBERTURA.md` — áreas e contagens do conteúdo
- `docs/IA_LOCAL.md` — decisão sobre IA local (não usada)
- `docs/MIUI_BATERIA.md` — permissões/bateria no MIUI

## Ética
Sem diagnóstico, sem dose, sem plano prescritivo. Conteúdo sensível é informativo. ND/NT sem certo/errado. Nenhum texto do app é gerado por IA: o banco vem de catálogo curado e templates determinísticos.
