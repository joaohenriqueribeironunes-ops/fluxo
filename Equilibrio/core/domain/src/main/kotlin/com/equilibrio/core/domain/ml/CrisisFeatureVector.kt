package com.equilibrio.core.domain.ml

import com.equilibrio.core.model.CheckIn
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.LocalRisk
import com.equilibrio.core.model.SensorWindow
import com.equilibrio.core.domain.time.TimeUtils
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vetor de features estável para o modelo TFLite (Fase 2).
 * Ordem documentada em ml/train_crisis_model.py — não alterar sem retreinar.
 */
object CrisisFeatureVector {
    const val SIZE = 24

    fun build(
        risk: LocalRisk,
        windows: List<SensorWindow>,
        checkIns: List<CheckIn>,
        crises: List<CrisisEvent>,
        now: Long
    ): FloatArray {
        val lastHour = windows.filter { it.endMillis >= now - 3_600_000L }
        val recentCheckIns = checkIns.filter { it.timestamp >= now - 24 * 3_600_000L }
        val todayCheckIns = recentCheckIns.filter { it.timestamp >= TimeUtils.startOfDay(now) }
        val lastCheckIn = checkIns.maxByOrNull { it.timestamp }
        val lastMeal = checkIns.filter { it.consumed.isNotEmpty() }.maxByOrNull { it.timestamp }
        val lastCrisis = crises.filter { it.type.isActualCrisis }.maxByOrNull { it.startMillis }

        val sleep = todayCheckIns.mapNotNull { it.sleepHours.takeIf { h -> h > 0f } }.maxOrNull() ?: 8f
        val soundAvg = lastHour.map { it.soundOverloadIndex }.averageOrZero() / 100.0
        val agitationAvg = lastHour.map { it.agitationIndex }.averageOrZero() / 100.0
        val maxSound = (lastHour.maxOfOrNull { it.soundDbMax } ?: 0f) / 120.0
        val hour = TimeUtils.hourOfDay(now)
        val dow = TimeUtils.dayOfWeek(now)
        val sinceCheckInHours = lastCheckIn?.let { (now - it.timestamp) / 3_600_000f } ?: 24f
        val sinceCrisisDays = lastCrisis?.let { (now - it.startMillis) / 86_400_000f } ?: 30f
        val sinceMealHours = lastMeal?.let { (now - it.timestamp) / 3_600_000f } ?: 12f
        val crises7d = crises.count { it.startMillis >= now - 7 * 86_400_000L && it.type.isActualCrisis }
        val near7d = crises.count { it.startMillis >= now - 7 * 86_400_000L && !it.type.isActualCrisis }

        return floatArrayOf(
            (risk.score / 100f).coerceIn(0f, 1f),
            soundAvg.toFloat(),
            agitationAvg.toFloat(),
            maxSound.toFloat(),
            (sleep / 12f).coerceIn(0f, 1f),
            ((todayCheckIns.maxOfOrNull { it.hunger } ?: 0) / 10f),
            ((todayCheckIns.maxOfOrNull { it.tension } ?: 0) / 10f),
            ((todayCheckIns.maxOfOrNull { it.sensorySensitivity } ?: 0) / 10f),
            ((todayCheckIns.sumOf { it.abruptTransitions }).coerceAtMost(6) / 6f),
            if (todayCheckIns.any { it.caffeine }) 1f else 0f,
            sin(2 * Math.PI * hour / 24.0).toFloat(),
            cos(2 * Math.PI * hour / 24.0).toFloat(),
            sin(2 * Math.PI * dow / 7.0).toFloat(),
            cos(2 * Math.PI * dow / 7.0).toFloat(),
            (sinceCheckInHours / 24f).coerceIn(0f, 1f),
            (sinceCrisisDays / 30f).coerceIn(0f, 1f),
            (sinceMealHours / 12f).coerceIn(0f, 1f),
            (crises7d / 10f).coerceIn(0f, 1f),
            (near7d / 10f).coerceIn(0f, 1f),
            if (risk.baselineReady) 1f else 0f,
            ((lastHour.maxOfOrNull { it.highBandRatio } ?: 0f)),
            ((lastHour.sumOf { it.impulsivePeaks }).coerceAtMost(30) / 30f),
            (soundAvg * (1f - (sleep / 12f).coerceIn(0f, 1f))).toFloat(),
            ((lastHour.maxOfOrNull { it.riskScore } ?: 0) / 100f)
        )
    }

    private fun List<Float>.averageOrZero(): Double =
        if (isEmpty()) 0.0 else map { it.toDouble() }.average()
}
