package com.equilibrio.core.model

data class GeminiAnalysis(
    val id: Long = 0,
    val createdAt: Long,
    val periodStart: Long,
    val periodEnd: Long,
    val riskCurrent: Int = 0,
    val level: RiskLevel = RiskLevel.BAIXO,
    val triggers: List<String> = emptyList(),
    val patterns: List<String> = emptyList(),
    val analysisText: String = "",
    val suggestions: List<String> = emptyList(),
    val confidence: Float = 0f,
    val warnings: List<String> = emptyList(),
    val forecasts: List<RiskForecast> = emptyList(),
    val recurringPatterns: List<String> = emptyList(),
    val questions: List<String> = emptyList(),
    val model: String = "",
    val crisisCountUsed: Int = 0,
    val daysOfData: Int = 0
)

/** Item unificado da linha do tempo do dia. */
sealed interface TimelineItem {
    val timestamp: Long

    data class SensorItem(val window: SensorWindow) : TimelineItem {
        override val timestamp: Long get() = window.startMillis
    }

    data class CheckInItem(val checkIn: CheckIn) : TimelineItem {
        override val timestamp: Long get() = checkIn.timestamp
    }

    data class CrisisItem(val crisis: CrisisEvent) : TimelineItem {
        override val timestamp: Long get() = crisis.startMillis
    }

    data class AnalysisItem(val analysis: GeminiAnalysis) : TimelineItem {
        override val timestamp: Long get() = analysis.createdAt
    }

    data class ForecastItem(val forecast: RiskForecast) : TimelineItem {
        override val timestamp: Long get() = forecast.windowStart
    }
}

data class DailySummary(
    val epochDay: Long,
    val startMillis: Long,
    val endMillis: Long,
    val averageRisk: Int = 0,
    val maxRisk: Int = 0,
    val crisisCount: Int = 0,
    val nearCrisisCount: Int = 0,
    val checkInCount: Int = 0,
    val averageSleepHours: Float = 0f,
    val averageSoundOverload: Float = 0f,
    val averageAgitation: Float = 0f,
    val dominantEmotions: List<Emotion> = emptyList(),
    val topTriggers: List<String> = emptyList(),
    val timeline: List<TimelineItem> = emptyList()
)

data class WeeklySummary(
    val startMillis: Long,
    val endMillis: Long,
    val days: List<DailySummary>,
    val crisisCount: Int,
    val predictedCount: Int,
    val topPatterns: List<String>,
    val accuracy: AccuracyReport
)
