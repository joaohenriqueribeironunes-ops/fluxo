# Cobertura de conteúdo

Estado após o seed local de 08/11/2026 (sem API e sem IA):

## behavior.db
- **4.992 respostas distintas** em `resposta_relacionamento`, 8 relações × 39 subtemas × 2 perfis (624 células), 8 respostas por célula.
- Deduplicação por contexto real (`relacaoTipo + subtema + perfilNdOuNt`), cosseno < 0,85; zero duplicata exata global.
- Distribuição: ND 2.496 · NT 2.496 · cada relação com 624 respostas.
- **40 situações curadas** em `situacao`, com 40 leituras ND, 40 leituras NT e 40 pontes completas.
- Categorias de situação: amizade (5), amor (5), família (5), trabalho (5), colegas (5), vizinhos (5), desconhecidos (5), atendimento (5).

## academic.db
- **2.900 frases explicativas** em `frase_explicativa`, 58 subáreas com exatamente 50 frases distintas cada.
- **326 etimologias** em `etimologia` (raiz original, evolução fonética e mudança semântica).
- Áreas obrigatórias presentes: astronomia, astrofísica, ciências da natureza, física, matemática visual, matemática pura e lógica, química, biologia, geologia, meteorologia, Língua Portuguesa, Língua Inglesa, filosofia, psicologia, sociologia, fenomenologia de pessoas superdotadas, TDAH e autismo (ciência e vivência), neurociência, neurofisiologia, neuroanatomia, conexões biológicas sistêmicas, análise histórica linear, fotografia conceitual, teoria do design e arquitetura, micologia e microbiologia, nutrição funcional e dietética, farmacocinética (conceitos gerais, sem dose), sistemas de padronização (ABNT/APA/Vancouver/ISO) e etimologia.

## Verificação
```powershell
python tools/verificar_volume.py
```
Saída esperada: `RESULTADO: PASSOU (todas as metas atendidas).`
