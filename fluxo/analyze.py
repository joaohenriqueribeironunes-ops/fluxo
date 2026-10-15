"""Análise local de ideias — sem IA externa, usa o acervo baixado no aparelho."""

import re
import unicodedata

STOPWORDS = set(
    """a o e é de da do das dos em no na nos nas um uma uns umas por para com sem sob sobre
    ao aos à às que quem qual quais como quando onde porque pois mas mais menos muito muita
    muitos muitas pouco pouca ser sou fui foi são era eram ter tem tenho tinha este esta
    isto esse essa isso aquele aquela aquilo seu sua seus suas meu minha nosso nossa ele ela
    eles elas eu tu você nos vós lo la las los se me te lhe sim não nem já tão só tudo todos
    toda todas nada algo cada entre até desde depois antes agora aqui ali lá então também
    ainda apenas dentro fora junto lado forma coisa modo""".split()
)

ACCENTS = re.compile(r"[̀-ͯ]")


def _slug(word):
    return unicodedata.normalize("NFD", word.lower())


def topics(text, limit=6):
    words = [
        w
        for w in (ACCENTS.sub("", _slug(c)) for c in re.split(r"[^0-9a-zA-Z]+", text))
        if len(w) > 3 and w not in STOPWORDS
    ]
    seen = []
    for w in words:
        if w not in seen:
            seen.append(w)
        if len(seen) >= limit:
            break
    return seen


def _congruence(topics_list, articles):
    if not topics_list or not articles:
        return 0, []
    corpus = " ".join(
        f"{a['title']} {a['text']}".lower() for a in articles
    )
    corpus = ACCENTS.sub("", _slug(corpus))
    matched = [t for t in topics_list if t in corpus]
    return round(100 * len(matched) / len(topics_list)), matched


ANGLES = {
    "divergente": lambda t: [
        f'Inverta: e se o contrário de "{t[0]}" fosse o ponto de partida?',
        f"Reduza: qual é a menor versão de \"{t[0]}\" que ainda funciona?",
        f'Amplie: onde "{t[0]}" já existe em outra área?',
        f'Conecte: como "{t[0]}" e "{t[1]}" se ligam?'
        if t[1]
        else f'Conecte: com o que "{t[0]}" se parece?',
        f'Atrito: o que faria "{t[0]}" falhar rápido?',
    ],
    "profundo": lambda t: [
        f'O que você está assumindo como óbvio em "{t[0]}"?',
        f'Camada 1: por que "{t[0]}" importa, pra você?',
        f'Tensão: o que puxa "{t[0]}" contra "{t[1]}"?'
        if t[1]
        else f'Limite: onde "{t[0]}" para de valer?',
        f'Reformula: como você explicaria "{t[0]}" em uma frase para alguém de fora?',
    ],
    "dados": lambda t: [
        f'Como você mediria "{t[0]}" em um número?',
        f'Amostra: quantos casos de "{t[0]}" você já viu?',
        f'Baseline: o que acontece hoje sem "{t[0]}"?',
        f'Correlação: "{t[0]}" e "{t[1]}" variam juntos?'
        if t[1]
        else f'Ruído: o que distorce a leitura de "{t[0]}"?',
    ],
}


def analyze(idea, articles, mode):
    t = topics(idea)
    pct, matched = _congruence(t, articles)
    keys = t + [""] * 2

    lines = [
        "Análise local — feita aqui no aparelho, sem IA externa, com o que você já baixou.",
        "",
        f"Temas da ideia: {', '.join(t)}"
        if t
        else "Sem temas: escreva uma ideia um pouco mais longa.",
    ]

    if not articles:
        lines.append(
            ""
            "\nVocê ainda não guardou artigos. Baixe material na seção abaixo — a análise passa a usar esse conteúdo."
        )
    else:
        tail = (
            f"Temas encontrados: {', '.join(matched)}"
            if matched
            else "Nenhum tema da ideia aparece no acervo."
        )
        lines += ["", f"Acervo: {len(articles)} artigo(s). {tail}"]

    lines += ["", "Ângulos para pensar:"]
    lines += [f"• {a}" for a in ANGLES[mode](keys if t else ["sua ideia", ""])]

    level = "alta" if pct >= 60 else "média" if pct >= 25 else "baixa"
    lines += [
        "",
        f"Congruência {level}: {pct}% dos temas aparecem no seu acervo ({len(articles)} artigos guardados).",
    ]
    return "\n".join(lines)
