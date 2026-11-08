package com.equilibrio.core.model

/** Nível qualitativo de risco. Nunca usar vermelho agressivo na UI. */
enum class RiskLevel(val label: String) {
    BAIXO("Baixo"),
    MODERADO("Moderado"),
    ALTO("Alto");

    companion object {
        fun fromScore(score: Int): RiskLevel = when {
            score >= 70 -> ALTO
            score >= 40 -> MODERADO
            else -> BAIXO
        }
    }
}

/** Tipos de evento usados como rótulo de treino. */
enum class CrisisType(val label: String) {
    MELTDOWN("Meltdown"),
    SHUTDOWN("Shutdown"),
    SOBRECARGA_SENSORIAL("Sobrecarga sensorial"),
    SOBRECARGA_TDAH("Sobrecarga de TDAH / paralisia"),
    QUASE_CRISE("Quase-crise (consegui evitar)");

    val isActualCrisis: Boolean
        get() = this != QUASE_CRISE
}

enum class PredictionHorizon {
    CURTO,
    HOJE,
    SEMANA
}

enum class PredictedOutcome {
    PENDENTE,
    CRISE,
    QUASE_CRISE,
    NADA,
    ALERTA_UTIL,
    FALSO_ALARME
}

enum class ConfidenceLevel(val label: String) {
    BAIXA("baixa"),
    MEDIA("média"),
    ALTA("alta");

    companion object {
        fun fromValue(value: Float): ConfidenceLevel = when {
            value >= 0.66f -> ALTA
            value >= 0.33f -> MEDIA
            else -> BAIXA
        }
    }
}

/** Roda de emoções enxuta, com valência para análises de padrão. */
enum class Emotion(val label: String, val valence: Int) {
    ALEGRIA("Alegria", 1),
    CALMA("Calma", 1),
    ANSIEDADE("Ansiedade", -1),
    RAIVA("Raiva", -1),
    TRISTEZA("Tristeza", -1),
    MEDO("Medo", -1),
    SOBRECARGA("Sobrecarga", -1),
    VERGONHA("Vergonha", -1)
}

enum class DayPart(val label: String) {
    MADRUGADA("Madrugada"),
    MANHA("Manhã"),
    TARDE("Tarde"),
    NOITE("Noite");

    companion object {
        /** Hora do dia 0..23. */
        fun fromHour(hour: Int): DayPart = when (hour) {
            in 0..5 -> MADRUGADA
            in 6..11 -> MANHA
            in 12..17 -> TARDE
            else -> NOITE
        }
    }
}

enum class ForecastSource {
    LOCAL,
    GEMINI
}

enum class CollectionStatus {
    COLETANDO,
    PAUSADO,
    BATERIA_BAIXA,
    APRENDENDO,
    SEM_PERMISSAO
}

enum class AlertFeedback {
    ALERTA_UTIL,
    FALSO_ALARME,
    ADIADO
}
