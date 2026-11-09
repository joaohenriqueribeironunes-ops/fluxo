# Gerador (FASE 2)

Pipeline de geração em massa (Gemini → SQLite compatível com Room). A chave é lida **somente** de `Lambda/local.properties` (`GEMINI_API_KEY=...`) — **nunca** é logada/exibida/commitada.

## O que faz
- Lotes por subtema (comportamento) e subárea (acadêmico), prompts estruturados, saída **JSON validada/parseada**.
- **Retentativas com backoff** exponencial e **limite de RPM** (padrão conservador).
- **Checkpoint** retomável em `.checkpoints/` (interrompeu? rode de novo).
- **Estimativa de chamadas/custo** e **confirmação obrigatória** antes de consumir API (`SIM` interativo, ou `--yes` após revisar `--estimate-only`).
- **Qualidade**: deduplicação por cosseno < 0,85, tamanho mínimo, `revisado=false`, relatório em `reports/` (cobertura + sinalizações leves).
- **Segurança** (conteúdo sensível: saúde/nutrição/farmacocinética/relações abusivas): informativo, **sem diagnóstico, sem dose, sem plano prescritivo**; farmacocinética só conceitos gerais ADME; indica apoio profissional/canais seguros quando pertinente.
- **Empacota** `behavior.db`/`academic.db` em `app/src/main/assets/` (prontos para uso offline).

## Instalação
```powershell
cd Lambda
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r tools/gerador/requirements.txt
```

## Uso
```powershell
# 1) Ver estimativa (sem consumir API)
python tools/gerador/gerar.py --estimate-only --target all

# 2) Gerar (pede confirmação "SIM")
python tools/gerador/gerar.py --target all
# opções: --target behavior|academic|all  --rpm 10  --model gemini-1.5-flash  --no-assets  --yes
```

## Saídas
- `tools/gerador/out/behavior.db`, `out/academic.db`
- `app/src/main/assets/*.db` (copiados, exceto com `--no-assets`)
- `tools/gerador/reports/COBERTURA_GERADA.md` e `cobertura.json`
- `tools/gerador/.checkpoints/` (retomada)

## Validar metas
```powershell
python tools/verificar_volume.py
```
(behavior ≥ 3000 distintas, equilíbrio ND/NT e relações; academic ≥ 2500, ≥ 50/subárea).