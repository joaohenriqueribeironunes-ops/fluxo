# Equilíbrio

App Android pessoal de autoconhecimento para uma pessoa autista com TDAH. O app observa sinais do ambiente e do corpo, registra check-ins rápidos e ajuda a enxergar correlações com crises sensoriais/emocionais — sem diagnóstico, sem culpa e sem vender dados.

> **Não é dispositivo médico.** Este app não substitui avaliação, diagnóstico ou acompanhamento profissional. Em risco à segurança, ligue **CVV 188** (24 h, gratuito) ou **SAMU 192**.

---

## O que o app faz

- **Coleta passiva opcional** a cada 30 s (15 s coletando + 15 s pausado): acelerômetro, giroscópio, luz, proximidade e microfone processado em memória.
- **Check-in em até 3 toques**: emoções, intensidade, sono, fome, dor, tensão, sensibilidade, cafeína/refeições, medicação, atividades, transições e uma nota curta.
- **Botão de crise** sempre acessível, com opção de confirmação para evitar toque acidental.
- **Debrief pós-crise** agendado automaticamente para 30 minutos depois.
- **Previsão local** com baseline pessoal de 14 dias, regras + z-score e métricas honestas (recall, precisão, Brier e antecedência média).
- **Insights locais** de correlação, gatilhos mais citados, o que costuma ajudar e mapa de calor por horário.
- **Análise opcional com Gemini** usando apenas resumos agregados (sem áudio, sem amostras brutas de sensores e sem texto livre de check-in).
- **Relatório PDF** exportável e compartilhável.
- **Privacidade por padrão**: banco local criptografado com SQLCipher, chave no Android Keystore, backup desativado e opção de apagar todos os dados.
- **Acessibilidade**: reduzir movimento, reduzir transparência, alto contraste, fonte ajustável, tema claro/escuro e nenhum flash.
- **Notificações calmas** para lembretes, alertas preventivos e debrief, com limite diário configurável.

---

## Instalação do APK

1. Baixe o APK na página de **Releases** deste repositório.
2. No celular, permita “instalar apps desconhecidos” para o app de arquivos/navegador usado.
3. Abra o APK e instale.
4. No primeiro uso, o onboarding explica o que é coletado e pede as permissões necessárias.

Requisitos: **Android 11 ou superior** (minSdk 30). Testado para o Redmi Note 11S (MIUI/HyperOS); veja o checklist de bateria em [`docs/MIUI-BATTERY-CHECKLIST.md`](docs/MIUI-BATTERY-CHECKLIST.md).

### Assinatura

O APK de release é assinado com uma chave local. O arquivo `keystore.properties` e a pasta `keystore/` **não são versionados**. Para gerar sua própria chave:

```bash
keytool -genkeypair -v \
  -keystore keystore/equilibrio-release.jks \
  -alias equilibrio -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Equilibrio, OU=Mobile, O=Equilibrio, L=Sao Paulo, ST=SP, C=BR"
cp keystore.properties.example keystore.properties
# edite keystore.properties com as senhas escolhidas
```

---

## Build

Pré-requisitos: JDK 17 e Android SDK com plataforma 34.

```bash
./gradlew :core:domain:test          # testes de domínio
./gradlew :app:assembleDebug         # APK de debug
./gradlew :app:assembleRelease       # APK de release (usa keystore.properties)
```

Artefatos:

- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

> **Windows com caminho acentuado:** se o build falhar por causa do “ã” em `Projeto Padrão`, mapeie o projeto para uma unidade sem acento antes de rodar o Gradle:
>
> ```powershell
> subst E: "C:\caminho\para\Equilibrio"
> Set-Location E:\
> .\gradlew.bat :app:assembleRelease
> ```

### Configurar o proxy do Gemini

O APK **nunca contém a chave da API**. A análise com Gemini usa um proxy HTTP (Cloudflare Worker) que guarda a chave no servidor.

1. Faça o deploy de `server/cloudflare-worker` (instruções no README da pasta).
2. Configure a URL do Worker de uma destas formas:
   - em `gradle.properties`: `GEMINI_PROXY_URL=https://seu-worker.workers.dev`
   - ou dentro do app, em **Configurações → Análise com Gemini → URL do proxy**.
3. No app, ative o Gemini e dê consentimento explícito.

Sem proxy configurado, a análise Gemini fica indisponível; todo o resto do app funciona offline.

---

## Estrutura do projeto

```
Equilibrio/
├── app/                 # UI Compose, navegação, serviço de coleta, workers, DI
├── core/
│   ├── model/           # modelos de domínio e settings
│   ├── domain/          # regras, features, baseline, risco, previsão, insights
│   ├── data/            # Room + SQLCipher, repositórios, Gemini, PDF, TFLite
│   └── ui/              # tema Liquid Glass, componentes e acessibilidade
├── docs/                # arquitetura, privacidade e checklist MIUI
├── server/              # Cloudflare Worker opcional para o Gemini
└── ml/                  # script opcional de treino do modelo TFLite
```

Detalhes em [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) e [`docs/PRIVACY.md`](docs/PRIVACY.md).

---

## Testes

```bash
./gradlew :core:domain:test
```

O núcleo de domínio cobre FFT, estatísticas de sinal, acumuladores, baseline, risco, previsão, calibração, precisão e insights.

---

## Publicar uma release no GitHub

Com o APK de release gerado:

```bash
gh release create v1.0.0 \
  app/build/outputs/apk/release/app-release.apk \
  --title "Equilíbrio 1.0.0" \
  --notes "Primeira versão instalável. Leia o README e o checklist MIUI antes de ativar a coleta em segundo plano."
```

Também é possível anexar o APK manualmente em **Releases → Draft a new release**.

---

## Limitações conhecidas

- A previsão é estatística e pessoal; ela melhora depois de algumas semanas de uso e de crises registradas.
- O modelo TFLite é **opcional** e não vem incluído no APK. Sem ele, o app usa apenas as regras locais.
- A coleta em segundo plano depende das permissões e da política de bateria do fabricante. No MIUI/HyperOS, siga o checklist.
- A análise Gemini depende de um proxy configurado e de rede; ela é opcional e nunca substitui acompanhamento profissional.

---

## Licença

Uso pessoal. Não há garantia de adequação clínica. Se você for reutilizar o código, mantenha os avisos de segurança e privacidade.
