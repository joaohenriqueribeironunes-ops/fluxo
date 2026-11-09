from __future__ import annotations
from dataclasses import dataclass, field
from typing import List

# Tipos de relação (obrigatórios) e subtemas comportamentais
RELACOES = ["amizade", "amoroso", "família", "trabalho", "colegas", "vizinhos", "desconhecidos", "atendimento"]
# Mapeamento para o nome canônico usado no app (spec lista "amor")
RELACAO_DB = {
    "amizade": "amizade", "amoroso": "amor", "família": "família", "trabalho": "trabalho",
    "colegas": "colegas", "vizinhos": "vizinhos", "desconhecidos": "desconhecidos", "atendimento": "atendimento",
}

SUBTEMAS_COMPORTAMENTO = [
    "comunicação literal vs implícita", "contato visual", "small talk", "ironia e sarcasmo",
    "limites pessoais", "dizer não", "conflito", "ghosting", "ciúmes", "flerte",
    "consentimento", "término", "luto", "fazer amizade", "manter amizade", "encerrar amizade",
    "masking", "burnout autista", "meltdown e shutdown", "stimming", "RSD disforia sensível à rejeição",
    "sobrecarga sensorial", "hiperfoco", "cegueira temporal", "normas em ambiente escolar",
    "normas no trabalho", "normas em transporte", "normas em festa", "normas em consulta",
    "pedir ajuda", "pedir esclarecimento", "reparar mal-entendido", "elogios e críticas",
    "mensagens e tempo de resposta", "convites e recusas", "iniciar conversa", "encerrar conversa",
    "privacidade e confiança", "sinais de relacionamento abusivo",
]

PERFIS = ["ND", "NT"]
TAMANHOS = ["curto", "padrao", "detalhado"]  # usados como variação; meta conta respostas distintas

@dataclass
class SubareaPlan:
    nome: str
    descricao: str = ""
    frases_alvo: int = 50
    angulos: List[str] = field(default_factory=lambda: [
        "definição", "analogia", "exemplo cotidiano", "causa e efeito", "erro comum",
        "curiosidade", "conexão com outra área", "versão visual", "versão para hiperfoco", "versão curta e direta",
    ])

@dataclass
class AreaPlan:
    nome: str
    subareas: List[SubareaPlan]
    sensivel: bool = False  # saúde/nutrição/farmacocinética/relações abusivas

# Todas as áreas obrigatórias (spec seção 4)
ACADEMIC_AREAS: List[AreaPlan] = [
    AreaPlan("Astronomia", [SubareaPlan("Sistema Solar"), SubareaPlan("Estrelas e evolução estelar")]),
    AreaPlan("Astrofísica", [SubareaPlan("Gravitação e relatividade"), SubareaPlan("Cosmologia observacional")]),
    AreaPlan("Ciências da natureza", [SubareaPlan("Método científico"), SubareaPlan("Matéria e energia na natureza")]),
    AreaPlan("Física", [SubareaPlan("Mecânica clássica"), SubareaPlan("Eletricidade e magnetismo")]),
    AreaPlan("Matemática visual", [SubareaPlan("Geometria e formas"), SubareaPlan("Gráficos e padrões visuais")]),
    AreaPlan("Matemática pura e lógica", [SubareaPlan("Lógica proposicional"), SubareaPlan("Conjuntos e demonstrações")]),
    AreaPlan("Química", [SubareaPlan("Estrutura atômica e ligações"), SubareaPlan("Reações e equilíbrio")]),
    AreaPlan("Biologia", [SubareaPlan("Célula e genética"), SubareaPlan("Ecologia e evolução")]),
    AreaPlan("Geologia", [SubareaPlan("Rochas e ciclo geológico"), SubareaPlan("Tectônica de placas")]),
    AreaPlan("Meteorologia", [SubareaPlan("Atmosfera e tempo"), SubareaPlan("Clima e fenômenos extremos")]),
    AreaPlan("Língua Portuguesa", [SubareaPlan("Sintaxe e interpretação"), SubareaPlan("Semântica e coesão")]),
    AreaPlan("Língua Inglesa", [SubareaPlan("Gramática essencial"), SubareaPlan("Vocabulário e pragmática")]),
    AreaPlan("Filosofia", [SubareaPlan("Ética e argumento"), SubareaPlan("Epistemologia")]),
    AreaPlan("Psicologia", [SubareaPlan("Cognição e emoção"), SubareaPlan("Comportamento e aprendizagem")], sensivel=False),
    AreaPlan("Sociologia", [SubareaPlan("Normas e instituições"), SubareaPlan("Identidade e grupos")]),
    AreaPlan("Fenomenologia de pessoas superdotadas", [SubareaPlan("Experiência subjetiva e intensidade"), SubareaPlan("Assincronia e sobre-excitabilidades")]),
    AreaPlan("TDAH e autismo (ciência e vivência)", [
        SubareaPlan("Neurodesenvolvimento e evidências"), SubareaPlan("Vivência, suporte e autonomia"),
    ], sensivel=True),
    AreaPlan("Neurociência", [SubareaPlan("Neurônios e sinapses"), SubareaPlan("Circuitos e plasticidade")]),
    AreaPlan("Neurofisiologia", [SubareaPlan("Potenciais e transmissão"), SubareaPlan("Sistemas sensório-motores")]),
    AreaPlan("Neuroanatomia", [SubareaPlan("Estruturas cerebrais"), SubareaPlan("Vias e conexões anatômicas")]),
    AreaPlan("Conexões biológicas sistêmicas", [SubareaPlan("Eixos corpo-cérebro"), SubareaPlan("Homeostase e integração")]),
    AreaPlan("Análise histórica linear (linhas do tempo causais)", [SubareaPlan("Causalidade histórica"), SubareaPlan("Leitura de linhas do tempo")]),
    AreaPlan("Fotografia conceitual", [SubareaPlan("Luz, composição e intenção"), SubareaPlan("Narrativa visual")]),
    AreaPlan("Teoria do design e arquitetura", [SubareaPlan("Princípios de design"), SubareaPlan("Forma, função e espaço")]),
    AreaPlan("Micologia e microbiologia", [SubareaPlan("Microrganismos e métodos"), SubareaPlan("Fungos e ecologia microbiana")]),
    AreaPlan("Nutrição funcional e dietética", [SubareaPlan("Nutrientes e metabolismo (conceitos)"), SubareaPlan("Padrões alimentares e evidências")], sensivel=True),
    AreaPlan("Farmacocinética (conceitos gerais)", [
        SubareaPlan("ADME: absorção, distribuição, metabolismo e excreção"),
        SubareaPlan("Variabilidade, interações e limites (sem dose)"),
    ], sensivel=True),
    AreaPlan("Sistemas de padronização", [
        SubareaPlan("ABNT NBR 6023/10520/14724"), SubareaPlan("APA, Vancouver, ISO e normas industriais"),
    ]),
    AreaPlan("Etimologia", [SubareaPlan("Raízes greco-latinas"), SubareaPlan("Mudança semântica e fonética")]),
]
# 29 áreas * 2 subáreas = 58 subáreas * 50 = 2900 frases (> 2500)

ETIMOLOGIA_ALVO = 300

def estimate_behavior(cfg_batch: int = 6) -> dict:
    # 8 relações * ~39 subtemas * 2 perfis = 624 células; gerar várias respostas por célula.
    rels = len(RELACAO_DB)
    cells = rels * len(SUBTEMAS_COMPORTAMENTO) * len(PERFIS)  # 624
    # Precisamos de 3000 distintas; ~5 por célula = 3120, em lotes
    alvo = 3200
    per_cell = max(1, (alvo + cells - 1) // cells)  # 6
    calls = (cells * per_cell + cfg_batch - 1) // cfg_batch
    return {"alvo": alvo, "celulas": cells, "por_celula": per_cell, "chamadas": calls}

def estimate_academic(cfg_batch: int = 1) -> dict:
    # 1 chamada por (subárea, angulo) gerando ~5 frases => 10 chamadas/subárea * 58 = 580
    # abordagem: por subárea, pedir 50 frases cobrindo todos os ângulos em 2-3 chamadas
    subareas = sum(len(a.subareas) for a in ACADEMIC_AREAS)
    calls_frases = subareas * 3  # 174
    calls_etim = (ETIMOLOGIA_ALVO + 19) // 20  # 15
    calls = calls_frases + calls_etim
    alvo_frases = subareas * 50
    return {"alvo_frases": alvo_frases, "subareas": subareas, "alvo_etimologia": ETIMOLOGIA_ALVO, "chamadas": calls}

def total_estimate(cfg_batch_behavior: int = 6) -> dict:
    b = estimate_behavior(cfg_batch_behavior)
    a = estimate_academic()
    return {"behavior": b, "academic": a, "total_chamadas": b["chamadas"] + a["chamadas"]}
