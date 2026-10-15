from dataclasses import dataclass


@dataclass(frozen=True)
class Wave:
    id: str
    label: str
    desc: str
    carrier: float
    beat: float


WAVES = [
    Wave("beta", "Beta", "Foco e alerta", 200, 20),
    Wave("gama", "Gama", "Alta cognição", 300, 40),
    Wave("teta", "Teta", "Meditação profunda", 150, 6),
    Wave("alfa", "Alfa", "Relaxamento", 220, 10),
    Wave("omega", "Ômega", "Expansão mental", 400, 70),
    Wave("epsilon", "Epsilon", "Repouso profundo", 100, 0.5),
    Wave("delta", "Delta", "Sono profundo e recuperação", 90, 2),
]

NOISES = [
    {"id": "brown", "label": "Ruído Marrom", "desc": "Som grave e envolvente"},
    {"id": "white", "label": "Ruído Branco", "desc": "Som uniforme de máscara"},
]

PRESETS = [
    ("Sprint 15", 15 * 60),
    ("Foco 25", 25 * 60),
    ("Hiperfoco 50", 50 * 60),
    ("Imersão 90", 90 * 60),
    ("Pausa 5", 5 * 60),
    ("Pausa 15", 15 * 60),
]

PADS = [
    {"id": 0, "freq": 329.6, "color": "#22c55e"},
    {"id": 1, "freq": 440.0, "color": "#ef4444"},
    {"id": 2, "freq": 261.6, "color": "#3b82f6"},
    {"id": 3, "freq": 196.0, "color": "#eab308"},
]

REPEAT = ["Sem repetir", "Repetir faixa", "Repetir lista"]

MODES = [
    {"id": "clear", "label": "Limpo", "hint": "Sem efeitos climáticos", "icon": "block"},
    {"id": "chuva", "label": "Chuva", "hint": "Gotas caindo por todo o app", "icon": "water_drop"},
    {"id": "sol", "label": "Ensolarado", "hint": "Luz translúcida reativa ao fundo", "icon": "wb_sunny"},
    {"id": "nevoa", "label": "Névoa", "hint": "Fumaça fluindo pela tela (cinza, marrom, vinho claro)", "icon": "foggy"},
]


def wave_by_id(wave_id):
    return next((w for w in WAVES if w.id == wave_id), None)
