package com.equilibrio.core.domain.prediction

import com.equilibrio.core.model.BaselineProfile
import com.equilibrio.core.model.CheckIn
import com.equilibrio.core.model.CollectionStatus
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.Emotion
import com.equilibrio.core.model.LocalRisk
import com.equilibrio.core.model.RiskFactor
import com.equilibrio.core.model.RiskLevel
import com.equilibrio.core.model.SensorWindow
import com.equilibrio.core.domain.time.TimeUtils
import kotlin.math.abs

/**
 * Predição local Fase 1: regras explícitas + z-score sobre o baseline pessoal.
 * Não emite alerta antes de 14 dias de baseline (Fase 0).
 */
class RiskEngine(
    private val baselineDays: Int = 14,
    private val minSamples: Int = 200
) {
    private val baseline = BaselineEngine(minSamples = minSamples, minDays = baselineDays)

    fun evaluate(
        recentWindows: List<SensorWindow>,
        todayCheckIns: List<CheckIn>,
        recentCrises: List<CrisisEvent>,
        profile: BaselineProfile?,
        now: Long,
        micActive: Boolean
    ): LocalRisk {
        val days = profile?.daysObserved ?: 0
        val progress = (days / baselineDays.toFloat()).coerceIn(0f, 1f)

        if (profile == null || !profile.ready || recentWindows.isEmpty()) {
            return LocalRisk.learning(now, progress, days).copy(
                micActive = micActive,
                status = if (micActive) CollectionStatus.COLETANDO else CollectionStatus.APRENDENDO
            )
        }

        val recent = recentWindows
            .filter { it.endMillis >= now - 3_600_000L }
            .ifEmpty { recentWindows.takeLast(8) }
        val last = recent.lastOrNull() ?: return LocalRisk.learning(now, progress, days)

        val z = baseline.zScores(last, profile)
        val factors = mutableListOf<RiskFactor>()
        var raw = 12.0

        fun factor(label: String, detail: String, contribution: Double) {
            if (contribution <= 0.4) return
            raw += contribution
            factors += RiskFactor(label, detail, contribution.toFloat())
        }

        val agitationZ = z[FeatureKeys.AGITATION] ?: 0.0
        if (agitationZ > 0.6) {
            factor(
                "Agitação motora acima do seu padrão",
                "%.1f desvios acima da sua média".format(agitationZ),
                (agitationZ * 9.0).coerceAtMost(26.0)
            )
        }

        val soundZ = z[FeatureKeys.SOUND_OVERLOAD] ?: 0.0
        if (soundZ > 0.6) {
            factor(
                "Sobrecarga sonora acima do seu padrão",
                "%.1f desvios acima da sua média".format(soundZ),
                (soundZ * 10.0).coerceAtMost(26.0)
            )
        }

        val impulsiveZ = z[FeatureKeys.IMPULSIVE] ?: 0.0
        if (impulsiveZ > 1.0) {
            factor(
                "Ruídos impulsivos",
                "${last.impulsivePeaks} picos súbitos na última janela",
                (impulsiveZ * 3.0).coerceAtMost(12.0)
            )
        }

        val highZ = z[FeatureKeys.HIGH_BAND] ?: 0.0
        if (highZ > 1.0) {
            factor(
                "Muitos agudos no ambiente",
                "%.0f%% da energia sonora em agudos".format(last.highBandRatio * 100),
                (highZ * 2.0).coerceAtMost(8.0)
            )
        }

        val gyroZ = z[FeatureKeys.GYRO_STD] ?: 0.0
        if (gyroZ > 1.0) {
            factor(
                "Movimento de rotação elevado",
                "possível stimming intenso ou inquietação",
                (gyroZ * 2.0).coerceAtMost(8.0)
            )
        }

        val sustainedSound = averageZ(recent.takeLast(4), profile, FeatureKeys.SOUND_OVERLOAD)
        if (sustainedSound > 1.2) {
            factor(
                "Ruído alto sustentado",
                "acima do seu padrão nos últimos 15–60 min",
                if (sustainedSound > 2.2) 14.0 else 8.0
            )
        }

        val today = todayCheckIns.filter { it.timestamp >= TimeUtils.startOfDay(now) }
        val sleep = today.mapNotNull { it.sleepHours.takeIf { h -> h > 0f } }.maxOrNull()
        if (sleep != null && sleep < 6.5f) {
            factor(
                "Sono curto",
                "%.1f h registradas hoje".format(sleep),
                if (sleep < 5f) 12.0 else 7.0
            )
        }

        val hunger = today.maxOfOrNull { it.hunger } ?: 0
        if (hunger >= 7) factor("Fome alta", "intensidade $hunger/10", 6.0)

        val pain = today.maxOfOrNull { it.pain } ?: 0
        if (pain >= 7) factor("Dor", "intensidade $pain/10", 5.0)

        val tension = today.maxOfOrNull { it.tension } ?: 0
        if (tension >= 7) factor("Tensão física", "intensidade $tension/10", 5.0)

        val sensory = today.maxOfOrNull { it.sensorySensitivity } ?: 0
        if (sensory >= 8) factor("Sensibilidade sensorial alta", "intensidade $sensory/10", 8.0)

        val anxious = today.any {
            it.emotionIntensity >= 8 && (Emotion.ANSIEDADE in it.emotions || Emotion.SOBRECARGA in it.emotions)
        }
        if (anxious) factor("Ansiedade/sobrecarga intensa", "registro emocional recente", 7.0)

        if (today.any { it.caffeine }) factor("Cafeína", "registrada hoje", 3.0)

        val transitions = today.sumOf { it.abruptTransitions }
        if (transitions >= 2) factor("Transições bruscas", "$transitions hoje", 6.0)

        if (today.isNotEmpty()) {
            val lastMeal = today.filter { it.consumed.isNotEmpty() }.maxOfOrNull { it.timestamp }
            val hour = TimeUtils.hourOfDay(now)
            if (hour in 10..21 && (lastMeal == null || now - lastMeal > 5 * 3_600_000L)) {
                factor("Muito tempo sem registrar refeição", "mais de 5 h", 3.0)
            }
        }

        val recentCrisis = recentCrises.filter { it.startMillis in (now - 2 * 3_600_000L)..now }
        if (recentCrisis.isNotEmpty()) {
            factor("Recuperação recente de crise", "nas últimas 2 h", 6.0)
        }

        val timePrior = timePriorFactor(recentCrises, now)
        if (timePrior > 0.5) {
            factor("Horário historicamente mais sensível", "padrão dos seus registros", timePrior)
        }

        val score = raw.coerceIn(0.0, 100.0).toInt()
        return LocalRisk(
            score = score,
            level = RiskLevel.fromScore(score),
            factors = factors.sortedByDescending { it.contribution }.take(5),
            baselineReady = true,
            baselineProgress = 1f,
            baselineDays = days,
            updatedAt = now,
            micActive = micActive,
            status = if (micActive) CollectionStatus.COLETANDO else CollectionStatus.PAUSADO
        )
    }

    private fun averageZ(
        windows: List<SensorWindow>,
        profile: BaselineProfile,
        key: String
    ): Double {
        if (windows.isEmpty()) return 0.0
        val stat = profile.stats[key] ?: return 0.0
        val source = if (key == FeatureKeys.SOUND_OVERLOAD || key == FeatureKeys.IMPULSIVE) {
            windows.filter { it.micCaptured }
        } else {
            windows
        }
        if (source.isEmpty()) return 0.0
        val values = source.mapNotNull {
            when (key) {
                FeatureKeys.SOUND_OVERLOAD -> it.soundOverloadIndex.toDouble()
                FeatureKeys.AGITATION -> it.agitationIndex.toDouble()
                else -> null
            }
        }
        if (values.isEmpty()) return 0.0
        val zs = values.map { (it - stat.mean) / (if (stat.std < 1e-6) 1.0 else stat.std) }
        return zs.average()
    }

    /**
     * Frequência relativa de crises neste período do dia, comparada a 25%
     * (linha de base uniforme). Só considera histórico com pelo menos 5 crises.
     */
    fun timePriorFactor(crises: List<CrisisEvent>, now: Long): Double {
        if (crises.size < 5) return 0.0
        val part = TimeUtils.dayPart(now)
        val inPart = crises.count { TimeUtils.dayPart(it.startMillis) == part }
        val freq = inPart.toDouble() / crises.size
        return ((freq - 0.25) * 24.0).coerceIn(0.0, 8.0)
    }

    fun dominantCrisisType(crises: List<CrisisEvent>, now: Long): com.equilibrio.core.model.CrisisType? {
        if (crises.isEmpty()) return null
        val part = TimeUtils.dayPart(now)
        val samePart = crises.filter { TimeUtils.dayPart(it.startMillis) == part }
        val pool = if (samePart.size >= 2) samePart else crises
        return pool.groupingBy { it.type }.eachCount().maxByOrNull { it.value }?.key
    }

    /** Tendência simples: inclinação dos últimos riscos por janela. */
    fun trend(windows: List<SensorWindow>): Double {
        val values = windows.takeLast(8).map { it.riskScore.toDouble() }
        if (values.size < 3) return 0.0
        val n = values.size
        val meanX = (n - 1) / 2.0
        val meanY = values.average()
        var num = 0.0
        var den = 0.0
        for (i in values.indices) {
            val dx = i - meanX
            num += dx * (values[i] - meanY)
            den += dx * dx
        }
        return if (den < 1e-9) 0.0 else (num / den).coerceIn(-20.0, 20.0)
    }

    fun factorsOverlap(a: LocalRisk, b: LocalRisk): Double {
        if (a.factors.isEmpty() || b.factors.isEmpty()) return 0.0
        val labelsA = a.factors.map { it.label }.toSet()
        val labelsB = b.factors.map { it.label }.toSet()
        return (labelsA intersect labelsB).size.toDouble() / labelsA.size.coerceAtLeast(1)
    }

    fun absolute(x: Double): Double = abs(x)
}
