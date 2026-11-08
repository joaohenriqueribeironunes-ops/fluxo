# Equilibrio v1.0 — Release

APK de release assinado do app nativo **Equilíbrio**, primeira versão instalável.

## O que é

App Android pessoal de autoconhecimento para pessoa autista com TDAH: coleta passiva de sensores e microfone, check-ins rápidos, previsão local, insights, alertas, debrief, análise opcional com Gemini e relatório.

**Não é dispositivo médico.** Não substitui avaliação, diagnóstico ou acompanhamento profissional. Em risco à segurança: **CVV 188** ou **SAMU 192**.

## Pacote

- Versão: `1.0.0` (versionCode 1)
- Mínimo Android: 11 (API 30+)
- Build: release com R8, assinada com keystore local
- Tamanho aproximado: 42 MB
- Assinatura: keystore local (`keystore.properties`, não versionado)

## Recursos principais

- Ciclo de coleta de 30 s (15 s coleta / 15 s pausa), sensores ~25 Hz, microfone 16 kHz mono processado em memória.
- Check-in máximo de 3 toques; botão de crise e debrief pós-crise.
- Baseline pessoal de 14 dias, risco local, previsões, feedback e métricas honestas.
- Análise com Gemini opcional, via proxy Cloudflare Worker, apenas com dados agregados.
- Privacidade: SQLCipher + Keystore, áudio nunca persistido, backup desativado.

## Instalação

1. Permita instalação de apps desconhecidos no aparelho.
2. Instale o APK abaixo.
3. Siga o onboarding e conceda notificações/permissões.

## Notas de instalação no MIUI/HyperOS (Redmi Note 11S)

Veja [`docs/MIUI-BATTERY-CHECKLIST.md`](docs/MIUI-BATTERY-CHECKLIST.md). Em resumo: permitir autostart, sem restrição de bateria, notificações, microfone e fixar o app nos recentes.

## Release

- Asset: [`equilibrio-v1.0-release.apk`](../dist/equilibrio-v1.0-release.apk)

- Changelog:

  - Primeira versão release do app Equilíbrio.
  - Correções de compilação e de navegação nas telas da UI.
  - Remoção de texto livre de check-in do payload enviado ao Gemini.
  - Correção dos requisitos de dependência para o módulo `app` (KSP/Hilt).

## Como buildar localmente

```bash
./gradlew :app:assembleRelease
```
