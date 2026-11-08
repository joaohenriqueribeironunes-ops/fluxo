# IA local (pendente — Fase 3)

- Recuperação: FTS5 + embeddings pequenos multilíngues quantizados (ONNX/TFLite).
- LLM local pequeno (ex.: Gemma 2B int4 via MediaPipe LLM Inference) **somente** se rodar fluido no Redmi Note 11S (Helio G96, 6/8 GB). Caso contrário: retrieval + templates.
- Benchmark, latência, memória e decisão documentados aqui após implementação. Meta de resposta: < 1,5 s no aparelho alvo.
- Sempre exibir `fonte` e `nivel_confianca`.
