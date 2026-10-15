# Fluxo (Python)

Reescrita em Python do **Fluxo Focus Lab** — app de foco, som e regulação sensorial para
cérebro TEA/TDAH. Local-first: nada de conta, nada de servidor. Os dados ficam num SQLite
no aparelho; a rede só é usada quando você pede uma pesquisa na Wikipédia.

É um *clean-room rebuild*: o app original roda na Base44 e não dá exportar o código, então
as telas foram reimplementadas a partir do comportamento observado, sem copiar código de
terceiros.

## As cinco telas

| Aba | O que faz |
| --- | --- |
| **Foco** | Timer com presets (Sprint 15, Foco 25, Hiperfoco 50, Imersão 90, pausas), ondas binaurais e ruído, respiração em caixa, 5-4-3-2-1 e compromentos do dia. |
| **Jogo** | Memória sensorial de 4 pads (Simon) com toques curtos e recorde salvo no aparelho. |
| **Frisson** | Player da sua biblioteca de faixas com o efeito de frequência: a faixa é afinada pela onda escolhida (`taxa = 1 + batimento/440`). |
| **Ideias** | Analisador determinístico em pt-BR: extração de tópicos, ângulos (Inverta/Reduza/Amplie/Conecte/Atrito) e congruência contra os artigos salvos da Wikipédia. |
| **Ajustes** | Clima visual, volume, e apagar todos os dados. |

## Rodar

```bash
python -m venv .venv && source .venv/bin/activate    # Windows: .venv\Scripts\activate
pip install -r requirements.txt
flet run main.py            # janela desktop
flet run --web main.py      # no navegador
```

## Gerar o áudio

As ondas e os toques não são sintetizados em tempo real como no site (Web Audio não existe
em Python). Eles são pré-renderizados por `fluxo/synth.py`, que é só biblioteca padrão:
senóides duro-panorâmicas L/R com batimento inteiro (loop sem emenda por crossfade das
pontas) e ruído marrom/branco com a mesma recorrência do original.

```bash
python tools/gen_audio.py     # escreve assets/audio/*.wav
```

## Build Android

```bash
flet build apk     # build de release; sai em build/apk/fluxo.apk
```

Precisa de JDK 17 e Android SDK. O workflow `.github/workflows/android.yml` faz isso no
GitHub Actions e publica o APK como asset de uma Release, então dá para compilar sem ter
nada instalado.

Sem segredos no repositório, o APK de release sai **assinado com a chave de depuração** —
instala no aparelho normalmente, mas não sobe na Play Store. Para assinar de verdade, crie
um keystore e grave quatro secrets no repositório: `ANDROID_KEYSTORE_BASE64`,
`ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS` e `ANDROID_KEY_PASSWORD`. Guarde o
keystore fora do GitHub: sem ele não dá para publicar atualização do mesmo app.

```bash
keytool -genkeypair -v -keystore fluxo.keystore -alias fluxo \
  -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 fluxo.keystore   # vai no ANDROID_KEYSTORE_BASE64
```

## O que **não** foi portado (e por quê)

- **Alternância por microfone.** O site mede o RMS do ambiente para sugerir troca de
  atividade. Não há gravador de áudio no Flet para Android hoje, então a tela não tem esse
  bloco — em vez de simular com números falsos.
- **Notificação agendada pelo Android.** O alarme toca e abre um diálogo **enquanto o app
  está aberto**. Notificar com o app fechado exige um plugin de terceiros e serviço em
  segundo plano; ficou de fora de propósito.
- **Chuva/névoa animados.** O modo "clima" tinge o fundo; a camada animada do CSS não foi
  recriada.
- **Tremolo por LFO no player.** Só a parte de afinação do efeito da faixa sobreviveu.
- **Suas músicas.** A biblioteca importa áudio do aparelho. Faixas comerciais não entram no
  repositório.

## Licença

MIT — veja `LICENSE`.
