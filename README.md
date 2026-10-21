# Fluxo — Ambiente de Foco e Repouso Sensorial

App React/TypeScript para mentes TEA/TDAH: foco, regulação sensorial e captura de ideias.
Todo o processamento (áudio, análise, acervo) acontece **no aparelho**, sem IA e sem enviar dados.

## 🚀 Rodar localmente

```bash
npm install
npm run dev
# http://localhost:5173
```

Build de produção:

```bash
npm run build
# saída em dist/
```

## 🧭 Telas

O app tem 5 modos, acessíveis pela dock inferior:

| Rota | Tela | O que faz |
|------|------|-----------|
| `/focus` | **Foco** | Timer circular (Sprint 15, Foco 25, Hiperfoco 50, Imersão 90, Pausas), sons binaurais e ambientes com volume, Alternância por microfone, Repouso sensorial (Respiração 4-4-4-4, Ancoragem 5-4-3-2-1, Dicas) e Meus compromissos |
| `/game` | **Jogo** | Sequência de cores — memória auditiva/visual com pontuação e recorde local |
| `/frisson` | **Frisson** | Biblioteca de músicas com reinterpretação por frequências binaurais |
| `/ideas` | **Ideias** | Modo Divergente: captura de ideias com análise local, acervo Wikipedia e análise de temas |
| `/settings` | **Ajustes** | Modo visual (Escuro / Névoa / Ensolarado), Alternância e lembretes |

Páginas de conta: `/login`, `/register`, `/forgot-password`, `/reset-password`.

## 🎨 Design

- **Glassmorphism** — superfícies translúcidas com blur e brilho interno
- **Fundo** — gradiente radial esverdeado (`#1f3a31 → #11201b → #0c1814`)
- **Acento** — emerald (`#6ee7b7`) e branco
- **Mobile first** — dock inferior com safe areas, alvos de 44px+
- **Tokens** — cores em HSL em `src/index.css`, utilitários `glass`, `glass-strong`, `glass-dock`, `glass-chip-active`, `glass-orb`

## 🛠️ Stack

- **React 18** + **TypeScript** + **Vite**
- **Tailwind CSS** (tema customizado)
- **React Router v6**
- **lucide-react** (ícones)
- **Web Audio API** (sons binaurais, ruídos, tom dos pads)
- **MediaDevices** (Alternância por microfone — só monitora, não grava)
- **localStorage** (ideias, acervo, músicas, compromissos, recorde)

## 📁 Estrutura

```
src/
├── components/
│   ├── layout.tsx              # Casca do app, logo e dock inferior
│   └── ui/                     # Button, Card, Input, AuthLayout, etc.
├── lib/
│   ├── audio.ts                # Motor de sons binaurais e ruídos
│   └── utils.ts                # cn()
├── pages/
│   ├── Focus.tsx               # Timer, sons, alternância, repouso, compromissos
│   ├── Game.tsx                # Sequência de cores
│   ├── Frisson.tsx             # Biblioteca musical + frequências
│   ├── Ideas.tsx               # Ideias, análise local e Wikipedia
│   ├── Settings.tsx            # Modo visual, alternância, lembretes
│   ├── Login.tsx / Register.tsx / ForgotPassword.tsx / ResetPassword.tsx
│   └── ...
├── App.tsx                     # Rotas
├── index.css                   # Tema e utilitários glass
└── main.tsx
```

## 🔐 Privacidade

- Sons gerados no aparelho via Web Audio API
- Análise de ideias local (contagem de temas por frequência de palavras)
- Microfone usado apenas para medir nível sonoro — nenhum áudio é gravado ou enviado
- Sem IA, sem sites externos (exceto a busca que você pedir na Wikipedia)

## 📄 Licença

MIT — feito para mentes que pensam diferente.
