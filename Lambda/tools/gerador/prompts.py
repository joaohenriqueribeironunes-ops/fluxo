from __future__ import annotations

TOM_BASE = """Tom: amigo muito próximo, caloroso, direto, sem infantilizar, sem julgar, sem sarcasmo ambíguo.
Frases curtas e concretas, linguagem literal. Explique o "porquê" implícito das regras sociais.
Ofereça "o que dizer" e "o que fazer" com exemplos quando couber. PT-BR."""

SAFETY_SENSITIVE = """CONTEÚDO SENSÍVEL (obrigatório): apenas informativo e geral. NUNCA diagnostique, NUNCA recomende dose de medicamento,
NUNCA prescreva plano alimentar. Em sinais de risco/abuso, oriente buscar ajuda profissional/apoio seguro.
Para farmacocinética: só conceitos gerais de absorção, distribuição, metabolismo e excreção (ADME), sem dose."""

SAFETY_ABUSE = """Se o subtema envolver relacionamento abusivo/violência: valide a segurança, evite culpar a vítima,
sugira buscar rede de apoio e canais de ajuda (ex.: CVV 188, serviços locais), sem instruções perigosas."""

def prompt_respostas(relacao: str, subtema: str, perfil: str, n: int, evitar: list[str]) -> str:
    sensivel = any(k in subtema.lower() for k in ["abusiv", "violên", "crise", "suicid"])
    safety = (SAFETY_ABUSE if sensivel else "")
    avoid_block = "\n".join(f"- {t[:140]}" for t in evitar[:12]) or "(nenhuma)"
    return f"""{TOM_BASE}
Gere {n} respostas DISTINTAS para:
- tipo de relação: {relacao}
- subtema: {subtema}
- perfil de leitura: {perfil} ({"como uma pessoa neurodivergente tende a perceber/sentir; foque em regulação, clareza e autonomia" if perfil=="ND" else "como uma pessoa neurotípica tende a perceber/interpretar; foque em intenção implícita e expectativas sociais, sem culpar o ND"})

Regras:
- Nenhuma leitura é "certa": são traduções entre dois sistemas.
- Inclua, quando natural, "o que dizer" e/ou "o que fazer" com exemplo concreto.
- Sem jargão não explicado. Sem duplicar nem parafrasear de forma óbvia.
- Evite repetir ideias destas respostas já geradas:
{avoid_block}
{safety}

Responda SOMENTE com JSON (array), sem texto fora:
[
  {{"relacaoTipo":"{relacao}","subtema":"{subtema}","perfilNdOuNt":"{perfil}","texto":"...","tom":"caloroso e direto","tamanho":"curto|padrao|detalhado","gatilhoDeUso":"...","tags":"tag1,tag2"}}
]"""

def prompt_frases(area: str, subarea: str, n: int, angulos: list[str], sensivel: bool, evitar: list[str]) -> str:
    safety = ("\n" + SAFETY_SENSITIVE) if sensivel else ""
    avoid_block = "\n".join(f"- {t[:140]}" for t in evitar[:12]) or "(nenhuma)"
    return f"""{TOM_BASE}
Gere {n} frases explicativas DETALHADAS e DIFERENTES sobre a subárea "{subarea}" (área: {area}).
Cada frase deve usar um ÂNGULO diferente dentre: {', '.join(angulos)}. Cubra os ângulos de forma equilibrada.
Regras:
- Linguagem clara; se usar termo técnico, explique no próprio texto.
- Sem duplicatas nem paráfrases óbvias.
- Cada texto com 1-4 frases, concreto e útil para estudo.
- Evite repetir ideias destas frases já geradas:
{avoid_block}
{safety}

Responda SOMENTE com JSON (array):
[
  {{"subarea":"{subarea}","angulo":"...","texto":"..."}}
]"""

def prompt_etimologia(n: int, evitar: list[str]) -> str:
    avoid_block = ", ".join(evitar[:40]) or "(nenhum)"
    return f"""{TOM_BASE}
Gere {n} entradas de ETIMOLOGIA de termos usados nas áreas do app (ciência, psicologia/neuro, estudo, relações, linguagem etc.), priorizando termos úteis.
Para cada termo registre EXATAMENTE:
- termo
- raizOriginal (latim, grego, sânscrito, proto-indo-europeu ou outra; indique a língua)
- evolucaoFonetica (como a forma mudou ao longo dos séculos)
- mudancaSemantica (como o sentido mudou ao longo do tempo)

Evite termos já usados: {avoid_block}
Linguagem clara, informativa. Responda SOMENTE com JSON (array):
[
  {{"termo":"...","raizOriginal":"...","evolucaoFonetica":"...","mudancaSemantica":"..."}}
]"""
