# Gerador local (sem API, sem IA)

Este diretório contém o seed determinístico dos bancos do Lambda. **Nenhuma chamada de rede, nenhuma chave e nenhum modelo de linguagem** são usados.

## Arquivos principais
- `situacoes_curadas.py` — catálogo escrito e revisado manualmente com 40 situações humanas detalhadas (amizade, amor, família, trabalho, colegas, vizinhos, desconhecidos e atendimento). Cada uma tem leitura ND, leitura NT e ponte prática.
- `content_plan.py` — relações, subtemas, perfis e áreas acadêmicas.
- `seed_local.py` — monta `behavior.db` e `academic.db` a partir do catálogo e de templates com variação determinística.
- `db_writer.py` — cria o schema compatível com Room, tabelas FTS5 e inserções (incluindo situação + leituras + ponte).
- `quality.py` — limpeza de texto e detecção de quase-duplicatas por cosseno (< 0,85).
- `../verificar_volume.py` — audita as metas e falha com código 1 se algo estiver abaixo.

## Uso
```powershell
cd Lambda
python tools/gerador/seed_local.py              # gera os dois bancos e copia para assets
python tools/gerador/seed_local.py --only behavior
python tools/gerador/seed_local.py --only academic
python tools/verificar_volume.py
```

## Metas verificadas
- `behavior.db`: ≥ 3.000 respostas distintas, com deduplicação pelo contexto real (`relacaoTipo + subtema + perfilNdOuNt`); ≥ 40 situações detalhadas com leitura ND, leitura NT e ponte completas.
- `academic.db`: ≥ 2.500 frases explicativas distintas, ≥ 50 por subárea (58 subáreas) e ≥ 300 etimologias.

## Saídas
- `tools/gerador/out/behavior.db` e `out/academic.db`
- `app/src/main/assets/*.db` (cópia automática)

## Princípios de qualidade
- PT-BR literal, sem julgamento e sem diagnóstico.
- ND e NT são leituras hipotéticas, não verdades sobre pessoas.
- Conteúdo sensível é informativo; farmacocinética só em conceitos gerais (sem dose).
- Quando houver risco imediato, o app orienta CVV 188 / SAMU 192.
