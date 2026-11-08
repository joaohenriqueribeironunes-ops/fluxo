# Arquitetura do Equilíbrio

## Visão geral

O Equilíbrio é um app Android nativo, offline-first, dividido em cinco módulos Gradle. A coleta em segundo plano é feita por um `ForegroundService`; a persistência é local e criptografada; a análise com Gemini é opcional e passa por um proxy.

```
┌────────────────────────────────────────────────────────────┐
│ app (Compose, Hilt, Navigation, WorkManager, Service)       │
│  ├── UI: onboarding, home, check-in, timeline, previsão...  │
│  ├── CollectionService: ciclo de 30 s                       │
│  ├── Workers: Gemini, lembretes, resolução, resumo, debrief │
│  └── DI: AppModule, DatabaseModule, RepositoryModule        │
├────────────────────────────────────────────────────────────┤
│ core:ui (tema Liquid Glass, componentes, acessibilidade)    │
├────────────────────────────────────────────────────────────┤
│ core:data (Room+SQLCipher, repositórios, Gemini, PDF, TFLite)│
├────────────────────────────────────────────────────────────┤
│ core:domain (features, baseline, risco, previsão, insights) │
├────────────────────────────────────────────────────────────┤
│ core:model (modelos de domínio e settings)                  │
└────────────────────────────────────────────────────────────┘
```

Dependências apontam para baixo: `app → core:ui / core:data → core:domain → core:model`. `core:domain` não conhece Android.

## Módulos

### `:core:model`
Modelos imutáveis: `SensorWindow`, `CheckIn`, `CrisisEvent`, `RiskForecast`, `BaselineProfile`, `PredictionRecord`, `AccuracyReport`, `TimelineItem`, `DailySummary`, `WeeklySummary`, `UserSettings`, `CalendarEvent` e enums (tipos de crise, emoções, níveis de risco etc.). Sem dependências de Android.

### `:core:domain`
Regras puras, testáveis em JVM:

- `feature/`: FFT, estatísticas de sinal, `SensorWindowAccumulator`, `SoundWindowAccumulator` e `SoundFeatures`.
- `prediction/`: `BaselineEngine` (baseline pessoal de 14 dias), `RiskEngine` (regras + z-score), `ForecastEngine`, `PlattCalibration`.
- `insights/`: correlações, gatilhos, o que ajudou, mapa de calor e padrões recorrentes.
- `accuracy/`: cálculo de recall, precisão, Brier e antecedência média.
- `gemini/`: payload estritamente agregado, prompts e montagem do JSON.
- `report/`: contrato do exportador de PDF.
- `repository/`: interfaces usadas pela UI e pelos workers.

Os testes ficam em `core/domain/src/test` e rodam com `./gradlew :core:domain:test`.

### `:core:data`
Implementações Android:

- **Room + SQLCipher**: entidades, DAOs e `DatabaseFactory`. A passphrase é gerada aleatoriamente e guardada no Android Keystore (`KeystoreSecretStore`); o arquivo do banco é criptografado com AES-256/GCM via SQLCipher.
- **Repositórios**: implementam os contratos de `core:domain` sobre os DAOs e DataStore.
- **Gemini**: `GeminiClientRouter` exige `geminiEnabled` e `geminiConsent`; `ProxyGeminiClient` chama o Worker; `DirectGeminiClient` é a alternativa de desenvolvimento. Nenhuma chave é empacotada no APK.
- **Relatório**: `PdfReportExporter` gera PDF em `filesDir/reports` e exporta via FileProvider.
- **TFLite opcional**: `TfliteCrisisModel` carrega `assets/crisis_model.tflite` se existir; sem o arquivo, o app usa apenas as regras locais.

### `:core:ui`
Design system “Liquid Glass” com Compose: gradientes por faixa de risco, superfícies translúcidas, botões, chips, toggles verdes, indicador de microfone, gráficos e barra de navegação em pílula. O tema respeita configurações de acessibilidade (reduzir movimento/transparência, alto contraste e escala de fonte).

### `:app`
Camada de aplicação:

- `MainActivity` + `AppNavHost` + 12 telas Compose.
- `MainViewModel` expõe estado (settings, risco, timeline, previsões, insights, precisão, resumos) e ações.
- `CollectionService`: serviço em primeiro plano com tipo `microphone|health`.
- `NotificationHelper` e receivers para ações de notificação, alertas e boot.
- `WorkScheduler` + workers Hilt.
- DI Hilt: `AppModule`, `DatabaseModule`, `RepositoryModule`.

## Ciclo de coleta

O ciclo padrão é de **30 segundos**:

1. **15 s coletando**: sensores a ~25 Hz; microfone a 16 kHz mono, processado em memória.
2. **15 s pausado**: sensores e microfone desligados.

Ao final da janela:

- O áudio já foi descartado; restam apenas agregados (dB médio/máximo, bandas, impulsividade, índice de sobrecarga).
- `SensorWindowAccumulator` e `SoundWindowAccumulator` produzem um `SensorWindow`.
- O `RiskEngine` calcula o risco local com baseline + regras + z-score.
- O `ForecastEngine` gera previsões para as próximas janelas.
- Os dados são persistidos no Room criptografado.
- Se o risco passar do limiar e o limite diário permitir, um alerta calmo é notificado.

A coleta pausa automaticamente quando a bateria fica abaixo do limiar configurável (padrão 15%) e quando o usuário desliga o toggle. O serviço precisa ser iniciado com o app em primeiro plano por causa das regras de microfone do Android 11+.

## Baseline e previsão

- O baseline usa os primeiros **14 dias** (configurável) de janelas para estimar média/desvio de cada feature.
- Features de som são ignoradas quando a janela não teve captura de microfone.
- O risco combina regras explícitas (sono, sobrecarga sonora, agitação, proximidade, luz, check-ins) com z-score em relação ao baseline pessoal.
- As previsões têm nível de confiança e ficam registradas para avaliação posterior.
- Quando a janela prevista passa, o `PredictionResolutionWorker` compara a previsão com o que aconteceu e atualiza as métricas.
- Métricas são exibidas de forma honesta, incluindo falsos alarmes e Brier; não há promessa de acurácia.

## Workers

| Worker | Frequência | Função |
|---|---|---|
| `GeminiBatchWorker` | 30–360 min (padrão 90) | Monta payload agregado, chama o proxy e salva a análise |
| `CheckInReminderWorker` | 60–480 min (padrão 240) | Lembrete gentil de check-in na janela configurada |
| `PredictionResolutionWorker` | 15 min | Resolve previsões vencidas e calcula acertos |
| `WeeklyMaintenanceWorker` | 7 dias | Manutenção e resumo semanal |
| `DailyDigestWorker` | 24 h | Resumo diário |
| `CrisisDebriefWorker` | 30 min após crise | Lembrete de debrief pós-crise |

Todos usam `HiltWorkerFactory`; o `WorkManager` é inicializado sob demanda em `EquilibrioApp`.

## Segurança e privacidade

- Banco SQLCipher com chave no Keystore.
- `allowBackup=false` e regras de extração de dados que excluem banco, arquivos e preferências.
- `FLAG_SECURE` opcional para ocultar o conteúdo no multitarefa.
- Áudio nunca é gravado em disco; amostras de sensores não são persistidas, só agregados.
- Payload do Gemini não inclui áudio, amostras brutas, pensamentos ou notas livres.
- Consentimento explícito para Gemini e para calendário; nada é enviado sem o toggle e o consentimento.
- Exportação e exclusão de dados disponíveis nas configurações.

Detalhes em [`PRIVACY.md`](PRIVACY.md).

## Build e variantes

- `debug`: sem minificação, para desenvolvimento.
- `release`: R8 + redução de recursos, assinado com a chave local definida em `keystore.properties`.
- `GEMINI_PROXY_URL` é um `BuildConfig` opcional; também pode ser configurado dentro do app.
- O modelo TFLite é opcional e deve ser colocado em `app/src/main/assets/crisis_model.tflite` antes do build.

## Decisões de projeto

- **Sem sensor cardíaco/barômetro**: foco no hardware do Redmi Note 11S.
- **Sem áudio persistido**: reduz risco de privacidade e tamanho do banco.
- **Proxy em vez de chave no APK**: uma chave embutida seria extraível; o Worker permite trocar/rotacionar a chave e aplicar limite de uso.
- **Domínio puro em JVM**: regras críticas testáveis sem emulador.
- **Glass + acessibilidade**: o visual não pode custar legibilidade; todas as camadas translúcidas têm modo de redução.
