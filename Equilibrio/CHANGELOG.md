# Changelog

## v1.0.2 - 2026-11-08

### Estabilidade
- Handler global de exceções com log local exportável em Configurações (sem travar o app silenciosamente).
- Correções de robustez: remoção de bloqueio na thread principal no encerramento do serviço de coleta, tratamento defensivo em caminhos críticos e fallback local quando a análise com IA falha ou retorna JSON inválido.
- Testes de unidade existentes + novos testes do motor de padrões mantidos em execução.

### Controles e experiência
- Botão de coleta na tela inicial agora deixa explícito **Ligar/Desligar coleta** (mesmo controle).
- Quick Settings Tile (painel de controle rápido): liga/desliga a coleta com estado sincronizado (DataStore + serviço) e relê o estado ao ouvir o tile.
- Onboarding com ação para adicionar o controle no painel rápido.
- Configurações com diagnóstico: exportar/apagar log de erros.

### Análise de padrões (motor local, sem diagnóstico)
- Agregações por hora/dia/semana.
- Correlações observacionais entre sono, ruído, agitação motora, consumo/cafeína, humor, tarefas/transições/desconforto e eventos de crise, com evidência, força da associação, confiança e limitações.
- Detecção de anomalias por z-score contra o baseline pessoal.
- Detecção de tendências e sequências que antecedem eventos.
- Mapa de calor dia da semana × hora.
- Achados locais enviados ao Gemini apenas como resumos agregados; aviso fixo de que é análise de padrões e não substitui avaliação profissional.

### Build e release
- Assinatura de release suporta `keystore.properties` local **ou** segredos de CI (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Keystore/senhas nunca devem ser commitados.
- Workflow GitHub Actions para tag `v*`: testes, `assembleRelease` assinado, release com APK e SHA-256.

## v1.0.1
- Novo ícone do app (liquid glass).
- Botão de coleta com texto Ligar/Desligar.

## v1.0.0
- Primeira release instalável.
