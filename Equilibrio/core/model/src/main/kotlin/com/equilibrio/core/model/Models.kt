package com.equilibrio.core.model

/**
 * Uma janela de 15 s de coleta passiva. Nenhum dado bruto de sensor ou áudio
 * chega a este modelo: apenas agregados calculados no aparelho.
 */
data class SensorWindow(
    val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long,
    val accelMean: Float = 0f,
    val accelStd: Float = 0f,
    val accelRms: Float = 0f,
    val accelPeaks: Int = 0,
    val gyroMean: Float = 0f,
    val gyroStd: Float = 0f,
    val gyroRms: Float = 0f,
    val gyroPeaks: Int = 0,
    val dominantFrequencyHz: Float = 0f,
    val spectralEnergy: Float = 0f,
    val lightMeanLux: Float = 0f,
    val proximityNear: Boolean = false,
    val agitationIndex: Float = 0f,
    val micCaptured: Boolean = false,
    val soundDbMean: Float = 0f,
    val soundDbMax: Float = 0f,
    val soundDbVariability: Float = 0f,
    val impulsivePeaks: Int = 0,
    val lowBandRatio: Float = 0f,
    val midBandRatio: Float = 0f,
    val highBandRatio: Float = 0f,
    val soundOverloadIndex: Float = 0f,
    val batteryPercent: Int = 100,
    val riskScore: Int = 0
) {
    val durationMillis: Long get() = (endMillis - startMillis).coerceAtLeast(0L)
}

data class CheckIn(
    val id: Long = 0,
    val timestamp: Long,
    val emotions: List<Emotion> = emptyList(),
    val emotionIntensity: Int = 5,
    val hunger: Int = 0,
    val sleepHours: Float = 0f,
    val pain: Int = 0,
    val tension: Int = 0,
    val sensorySensitivity: Int = 0,
    val consumed: List<String> = emptyList(),
    val caffeine: Boolean = false,
    val medication: List<String> = emptyList(),
    val supplements: List<String> = emptyList(),
    val activities: List<String> = emptyList(),
    val location: String = "",
    val people: List<String> = emptyList(),
    val abruptTransitions: Int = 0,
    val thoughts: String = "",
    val note: String = "",
    val quick: Boolean = false,
    val retroactive: Boolean = false
)

data class CrisisEvent(
    val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long? = null,
    val type: CrisisType,
    val intensity: Int = 5,
    val triggers: List<String> = emptyList(),
    val helped: List<String> = emptyList(),
    val worsened: List<String> = emptyList(),
    val recoveryMinutes: Int = 0,
    val notes: String = "",
    val retroactive: Boolean = false,
    val debriefDone: Boolean = false,
    val sensorWindowId: Long? = null
) {
    val durationMinutes: Int
        get() = endMillis?.let { ((it - startMillis) / 60_000L).toInt().coerceAtLeast(0) } ?: 0
}

data class RiskFactor(
    val label: String,
    val detail: String,
    val contribution: Float
)

data class RiskForecast(
    val windowStart: Long,
    val windowEnd: Long,
    val crisisType: CrisisType?,
    val probability: Float,
    val confidence: ConfidenceLevel,
    val factors: List<String> = emptyList(),
    val preventivePlan: List<String> = emptyList(),
    val source: ForecastSource = ForecastSource.LOCAL
) {
    val percent: Int get() = (probability.coerceIn(0f, 1f) * 100).toInt()
}

data class LocalRisk(
    val score: Int,
    val level: RiskLevel,
    val factors: List<RiskFactor>,
    val baselineReady: Boolean,
    val baselineProgress: Float,
    val baselineDays: Int,
    val updatedAt: Long,
    val micActive: Boolean,
    val status: CollectionStatus
) {
    companion object {
        fun learning(now: Long, progress: Float, days: Int) = LocalRisk(
            score = 0,
            level = RiskLevel.BAIXO,
            factors = emptyList(),
            baselineReady = false,
            baselineProgress = progress,
            baselineDays = days,
            updatedAt = now,
            micActive = false,
            status = CollectionStatus.APRENDENDO
        )
    }
}

data class BaselineStat(
    val mean: Double,
    val std: Double,
    val count: Int
)

data class BaselineProfile(
    val startedAt: Long,
    val daysObserved: Int,
    val sampleCount: Int,
    val stats: Map<String, BaselineStat>,
    val ready: Boolean
) {
    val progress: Float
        get() = (daysObserved / 14f).coerceIn(0f, 1f)
}

data class PredictionRecord(
    val id: Long = 0,
    val createdAt: Long,
    val horizon: PredictionHorizon,
    val windowStart: Long,
    val windowEnd: Long,
    val probability: Float,
    val crisisType: CrisisType?,
    val confidence: ConfidenceLevel,
    val factors: List<String> = emptyList(),
    val plan: List<String> = emptyList(),
    val outcome: PredictedOutcome = PredictedOutcome.PENDENTE,
    val feedbackAt: Long? = null,
    val riskScore: Int = 0,
    val source: ForecastSource = ForecastSource.LOCAL
)

data class AccuracyReport(
    val periodStart: Long,
    val periodEnd: Long,
    val totalCrises: Int = 0,
    val predictedCrises: Int = 0,
    val missedCrises: Int = 0,
    val falseAlarms: Int = 0,
    val meanLeadMinutes: Int = 0,
    val brierScore: Float = 0f,
    val precision: Float = 0f,
    val recall: Float = 0f,
    val evaluatedWindows: Int = 0
) {
    val hasData: Boolean get() = evaluatedWindows > 0 || totalCrises > 0
}

data class CorrelationInsight(
    val key: String,
    val label: String,
    val correlation: Double,
    val strength: Int,
    val description: String,
    val evidenceCount: Int = 0,
    val evidenceDates: List<Long> = emptyList(),
    val confidence: ConfidenceLevel = ConfidenceLevel.BAIXA,
    val limitations: List<String> = emptyList(),
    val kind: String = "correlacao"
) {
    val associationStrength: String
        get() = when {
            strength >= 4 -> "forte"
            strength >= 2 -> "moderada"
            else -> "fraca"
        }
}

data class HeatmapCell(
    val dayOfWeek: Int,
    val dayPart: DayPart,
    val count: Int,
    val intensity: Float,
    val hour: Int = -1
)

data class CalendarEvent(
    val id: Long = 0,
    val calendarEventId: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val importedAt: Long
)
