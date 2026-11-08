package com.equilibrio.core.domain.gemini

import com.equilibrio.core.model.AccuracyReport
import com.equilibrio.core.model.BaselineProfile
import com.equilibrio.core.model.CalendarEvent
import com.equilibrio.core.model.CheckIn
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.DailySummary
import com.equilibrio.core.model.ForecastSource
import com.equilibrio.core.model.RiskForecast
import com.equilibrio.core.model.SensorWindow
import com.equilibrio.core.domain.insights.InsightsBundle
import com.equilibrio.core.domain.time.TimeUtils

data class WindowAggregate(
    val startMillis: Long,
    val endMillis: Long,
    val agitation: Float,
    val soundOverload: Float,
    val soundDbMean: Float,
    val soundDbMax: Float,
    val highBandRatio: Float,
    val impulsivePeaks: Int,
    val lightLux: Float,
    val proximityNear: Boolean,
    val riskScore: Int
)

data class CheckInDigest(
    val timestamp: Long,
    val emotions: List<String>,
    val intensity: Int,
    val sleepHours: Float,
    val hunger: Int,
    val pain: Int,
    val tension: Int,
    val sensorySensitivity: Int,
    val consumed: List<String>,
    val caffeine: Boolean,
    val medication: List<String>,
    val activities: List<String>,
    val transitions: Int
) {
    companion object {
        fun from(checkIn: CheckIn) = CheckInDigest(
            timestamp = checkIn.timestamp,
            emotions = checkIn.emotions.map { it.label },
            intensity = checkIn.emotionIntensity,
            sleepHours = checkIn.sleepHours,
            hunger = checkIn.hunger,
            pain = checkIn.pain,
            tension = checkIn.tension,
            sensorySensitivity = checkIn.sensorySensitivity,
            consumed = checkIn.consumed,
            caffeine = checkIn.caffeine,
            medication = checkIn.medication,
            activities = checkIn.activities,
            transitions = checkIn.abruptTransitions
        )
    }
}

data class CrisisDigest(
    val startMillis: Long,
    val type: String,
    val intensity: Int,
    val durationMinutes: Int,
    val triggers: List<String>,
    val helped: List<String>,
    val recoveryMinutes: Int,
    val retroactive: Boolean
) {
    companion object {
        fun from(crisis: CrisisEvent) = CrisisDigest(
            startMillis = crisis.startMillis,
            type = crisis.type.label,
            intensity = crisis.intensity,
            durationMinutes = crisis.durationMinutes,
            triggers = crisis.triggers,
            helped = crisis.helped,
            recoveryMinutes = crisis.recoveryMinutes,
            retroactive = crisis.retroactive
        )
    }
}

data class DayDigest(
    val epochDay: Long,
    val averageRisk: Int,
    val maxRisk: Int,
    val crises: Int,
    val nearCrises: Int,
    val checkIns: Int,
    val sleepHours: Float,
    val soundOverload: Float,
    val agitation: Float,
    val emotions: List<String>,
    val triggers: List<String>
) {
    companion object {
        fun from(summary: DailySummary) = DayDigest(
            epochDay = summary.epochDay,
            averageRisk = summary.averageRisk,
            maxRisk = summary.maxRisk,
            crises = summary.crisisCount,
            nearCrises = summary.nearCrisisCount,
            checkIns = summary.checkInCount,
            sleepHours = summary.averageSleepHours,
            soundOverload = summary.averageSoundOverload,
            agitation = summary.averageAgitation,
            emotions = summary.dominantEmotions.map { it.label },
            triggers = summary.topTriggers
        )
    }
}

data class ForecastDigest(
    val windowStart: Long,
    val windowEnd: Long,
    val crisisType: String?,
    val probability: Float,
    val confidence: String,
    val factors: List<String>
) {
    companion object {
        fun from(forecast: RiskForecast) = ForecastDigest(
            windowStart = forecast.windowStart,
            windowEnd = forecast.windowEnd,
            crisisType = forecast.crisisType?.label,
            probability = forecast.probability,
            confidence = forecast.confidence.label,
            factors = forecast.factors
        )
    }
}

data class AccuracyDigest(
    val totalCrises: Int,
    val predictedCrises: Int,
    val falseAlarms: Int,
    val meanLeadMinutes: Int,
    val brierScore: Float,
    val recall: Float,
    val precision: Float
) {
    companion object {
        fun from(report: AccuracyReport) = AccuracyDigest(
            totalCrises = report.totalCrises,
            predictedCrises = report.predictedCrises,
            falseAlarms = report.falseAlarms,
            meanLeadMinutes = report.meanLeadMinutes,
            brierScore = report.brierScore,
            recall = report.recall,
            precision = report.precision
        )
    }
}

/**
 * Somente agregados. Nunca inclui amostras de acelerômetro/giroscópio,
 * formas de onda, áudio bruto, pensamentos ou notas livres do check-in.
 */
data class GeminiPayload(
    val generatedAt: Long,
    val periodStart: Long,
    val periodEnd: Long,
    val daysOfData: Int,
    val totalCrises: Int,
    val baselineReady: Boolean,
    val baselineDays: Int,
    val baselineSamples: Int,
    val windowAggregates: List<WindowAggregate>,
    val checkIns: List<CheckInDigest>,
    val crises: List<CrisisDigest>,
    val daily: List<DayDigest>,
    val localForecasts: List<ForecastDigest>,
    val localPatterns: List<String>,
    val localFindings: List<String> = emptyList(),
    val analysisDisclaimer: String = "Análise de padrões para autoconhecimento. Não é diagnóstico, não afirma causalidade e não substitui avaliação profissional.",
    val accuracy: AccuracyDigest?,
    val calendarEvents: List<CalendarEvent>
) {
    fun estimatedTokens(): Int {
        val chars = windowAggregates.size * 120 +
            checkIns.size * 180 +
            crises.size * 120 +
            daily.size * 160 +
            localForecasts.size * 100 +
            localPatterns.sumOf { it.length } +
            calendarEvents.size * 80 +
            600
        return chars / 4
    }
}

object GeminiPayloadBuilder {

    fun build(
        now: Long,
        windows: List<SensorWindow>,
        checkIns: List<CheckIn>,
        crises: List<CrisisEvent>,
        dailySummaries: List<DailySummary>,
        localForecasts: List<RiskForecast>,
        insights: InsightsBundle?,
        baseline: BaselineProfile?,
        accuracy: AccuracyReport?,
        calendarEvents: List<CalendarEvent>,
        periodStart: Long,
        periodEnd: Long,
        bucketMinutes: Int = 30
    ): GeminiPayload {
        val bucketMillis = bucketMinutes * 60_000L
        val aggregates = windows
            .filter { it.startMillis in periodStart..periodEnd }
            .groupBy { ((it.startMillis - periodStart) / bucketMillis).toInt() }
            .toSortedMap()
            .map { (bucket, group) ->
                WindowAggregate(
                    startMillis = periodStart + bucket * bucketMillis,
                    endMillis = group.maxOf { it.endMillis },
                    agitation = group.map { it.agitationIndex }.average().toFloat(),
                    soundOverload = group.map { it.soundOverloadIndex }.average().toFloat(),
                    soundDbMean = group.map { it.soundDbMean }.average().toFloat(),
                    soundDbMax = group.maxOf { it.soundDbMax },
                    highBandRatio = group.map { it.highBandRatio }.average().toFloat(),
                    impulsivePeaks = group.sumOf { it.impulsivePeaks },
                    lightLux = group.map { it.lightMeanLux }.average().toFloat(),
                    proximityNear = group.any { it.proximityNear },
                    riskScore = group.maxOf { it.riskScore }
                )
            }

        val findings = buildList {
            insights?.let { b ->
                addAll(b.correlations.map { formatFinding(it) })
                addAll(b.anomalies.map { formatFinding(it) })
                addAll(b.trends.map { formatFinding(it) })
                addAll(b.sequences.map { formatFinding(it) })
                addAll(b.dataLimitations.map { "Limitação: $it" })
            }
        }.take(24)

        return GeminiPayload(
            generatedAt = now,
            periodStart = periodStart,
            periodEnd = periodEnd,
            daysOfData = baseline?.daysObserved ?: 0,
            totalCrises = crises.size,
            baselineReady = baseline?.ready == true,
            baselineDays = baseline?.daysObserved ?: 0,
            baselineSamples = baseline?.sampleCount ?: 0,
            windowAggregates = aggregates,
            checkIns = checkIns.filter { it.timestamp in (periodStart - 12 * 3_600_000L)..periodEnd }
                .takeLast(30)
                .map { CheckInDigest.from(it) },
            crises = crises.filter { it.startMillis >= now - 14 * 86_400_000L }
                .takeLast(40)
                .map { CrisisDigest.from(it) },
            daily = dailySummaries.takeLast(14).map { DayDigest.from(it) },
            localForecasts = localForecasts.map { ForecastDigest.from(it) },
            localPatterns = insights?.recurringPatterns ?: emptyList(),
            localFindings = findings,
            analysisDisclaimer = insights?.disclaimer
                ?: "Análise de padrões para autoconhecimento. Não é diagnóstico, não afirma causalidade e não substitui avaliação profissional.",
            accuracy = accuracy?.let { AccuracyDigest.from(it) },
            calendarEvents = calendarEvents.filter {
                it.startMillis in (periodStart - 24 * 3_600_000L)..(periodEnd + 48 * 3_600_000L)
            }.take(30)
        )
    }

    private fun formatFinding(f: com.equilibrio.core.model.CorrelationInsight): String {
        val conf = f.confidence.label
        val lim = if (f.limitations.isEmpty()) "" else " Limitações: ${f.limitations.joinToString("; ")}"
        return "[${f.kind}/${f.associationStrength}/confiança $conf] ${f.description} (evidência: ${f.evidenceCount} registros).$lim"
    }

    fun isEmpty(payload: GeminiPayload): Boolean =
        payload.windowAggregates.isEmpty() && payload.checkIns.isEmpty() && payload.crises.isEmpty()
}
