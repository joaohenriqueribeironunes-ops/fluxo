package com.equilibrio.core.domain.prediction

import com.equilibrio.core.model.ConfidenceLevel
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.CrisisType
import com.equilibrio.core.model.DayPart
import com.equilibrio.core.model.ForecastSource
import com.equilibrio.core.model.LocalRisk
import com.equilibrio.core.model.PredictionHorizon
import com.equilibrio.core.model.RiskForecast
import com.equilibrio.core.model.SensorWindow
import com.equilibrio.core.domain.time.TimeUtils
import kotlin.math.exp

/**
 * Gera janelas de risco em três horizontes. A probabilidade local é a fonte
 * principal; o Gemini apenas explica e complementa. A confiança cai com a
 * distância no tempo e com o tamanho do histórico.
 */
class ForecastEngine(
    private val riskEngine: RiskEngine = RiskEngine()
) {

    fun shortTerm(
        risk: LocalRisk,
        recentWindows: List<SensorWindow>,
        crises: List<CrisisEvent>,
        now: Long
    ): List<RiskForecast> {
        if (!risk.baselineReady) return emptyList()
        val base = risk.score / 100.0
        val slope = riskEngine.trend(recentWindows)
        val trendEffect = (slope * 0.008).coerceIn(-0.12, 0.12)
        val type = likelyType(risk, recentWindows, crises, now)
        val plan = preventivePlan(risk)
        val factors = risk.factors.map { it.label }.take(3).toMutableList()
        if (slope > 0.4) factors += "Tendência de piora na última hora"
        if (slope < -0.4) factors += "Tendência de melhora na última hora"

        return listOf(30, 60, 120).map { minutes ->
            val horizonFactor = when (minutes) {
                30 -> 1.0
                60 -> 0.96
                else -> 0.9
            }
            val decay = 1.0 - (minutes / 120.0) * 0.08
            val probability = ((base + trendEffect) * horizonFactor * decay)
                .coerceIn(0.02, 0.95)
            RiskForecast(
                windowStart = now,
                windowEnd = TimeUtils.plusMinutes(now, minutes),
                crisisType = type,
                probability = probability.toFloat(),
                confidence = confidenceFor(
                    events = crises.size,
                    horizon = PredictionHorizon.CURTO,
                    minutesAhead = minutes
                ),
                factors = factors.take(3),
                preventivePlan = plan,
                source = ForecastSource.LOCAL
            )
        }
    }

    fun today(
        risk: LocalRisk,
        crises: List<CrisisEvent>,
        now: Long
    ): List<RiskForecast> {
        if (!risk.baselineReady) return emptyList()
        val endOfDay = TimeUtils.endOfDay(now)
        var start = maxOf(now, TimeUtils.startOfNextHour(now))
        val base = risk.score / 100.0
        val result = mutableListOf<RiskForecast>()
        var guard = 0
        while (start < endOfDay && guard < 12) {
            val end = minOf(TimeUtils.plusHours(start, 2), endOfDay)
            val hoursAhead = ((start - now) / 3_600_000.0).coerceAtLeast(0.0)
            val decay = exp(-hoursAhead / 4.0)
            val prior = partPrior(crises, TimeUtils.dayPart(start))
            val probability = if (crises.size >= 5) {
                (0.45 * prior + 0.55 * base * decay).coerceIn(0.03, 0.9)
            } else {
                (base * decay + 0.05).coerceIn(0.03, 0.85)
            }
            val factors = buildList {
                val count = crises.count { TimeUtils.dayPart(it.startMillis) == TimeUtils.dayPart(start) }
                if (crises.isNotEmpty() && count > 0) {
                    add("$count de ${crises.size} crises registradas neste período do dia")
                }
                addAll(risk.factors.take(2).map { it.label })
                if (hoursAhead > 4) add("Confiança menor: janela distante no dia")
            }.distinct().take(3)
            result += RiskForecast(
                windowStart = start,
                windowEnd = end,
                crisisType = likelyType(risk, emptyList(), crises, start),
                probability = probability.toFloat(),
                confidence = confidenceFor(
                    events = crises.size,
                    horizon = PredictionHorizon.HOJE,
                    minutesAhead = ((start - now) / 60_000L).toInt()
                ),
                factors = factors,
                preventivePlan = preventivePlan(risk),
                source = ForecastSource.LOCAL
            )
            start = end
            guard++
        }
        return result
    }

    fun week(crises: List<CrisisEvent>, now: Long): List<RiskForecast> {
        val result = mutableListOf<RiskForecast>()
        val startOfToday = TimeUtils.startOfDay(now)
        val weeksOfHistory = 8.0
        for (dayOffset in 0..6) {
            val dayStart = startOfToday + dayOffset * 86_400_000L
            val dow = TimeUtils.dayOfWeek(dayStart)
            for (part in DayPart.entries) {
                val partStart = when (part) {
                    DayPart.MADRUGADA -> 0
                    DayPart.MANHA -> 6
                    DayPart.TARDE -> 12
                    DayPart.NOITE -> 18
                }
                val windowStart = dayStart + partStart * 3_600_000L
                if (windowStart < now) continue
                val windowEnd = windowStart + 6 * 3_600_000L - 1
                val inPart = crises.filter {
                    TimeUtils.dayOfWeek(it.startMillis) == dow &&
                        TimeUtils.dayPart(it.startMillis) == part
                }
                val overallPart = crises.count { TimeUtils.dayPart(it.startMillis) == part }
                val overall = if (crises.isEmpty()) 0.0
                else overallPart.toDouble() / crises.size

                val probability = if (inPart.isNotEmpty()) {
                    ((inPart.size + 1.0) / (weeksOfHistory + 2.0))
                        .coerceIn(0.05, 0.85)
                } else {
                    (overall * 0.55 + 0.04).coerceIn(0.03, 0.6)
                }

                if (probability < 0.12 && inPart.isEmpty()) continue

                val confidence = when {
                    inPart.size >= 3 -> ConfidenceLevel.MEDIA
                    inPart.size in 1..2 -> ConfidenceLevel.BAIXA
                    else -> ConfidenceLevel.BAIXA
                }
                val factors = buildList {
                    if (inPart.isNotEmpty()) {
                        add("${inPart.size} crise(s) em ${TimeUtils.weekdayLabel(dow)} à ${part.label.lowercase()} nas últimas 8 semanas")
                    } else {
                        add("Nenhuma crise registrada nesse dia/período até agora")
                    }
                    add("Estimativa de longo prazo: pode errar bastante")
                }
                result += RiskForecast(
                    windowStart = windowStart,
                    windowEnd = windowEnd,
                    crisisType = inPart.groupingBy { it.type }.eachCount()
                        .maxByOrNull { it.value }?.key ?: crises.groupingBy { it.type }
                        .eachCount().maxByOrNull { it.value }?.key,
                    probability = probability.toFloat(),
                    confidence = confidence,
                    factors = factors.take(3),
                    preventivePlan = listOf(
                        "Planeje uma pausa sensorial preventiva nesse período",
                        "Reduza compromissos exigentes se possível",
                        "Garanta refeição e hidratação antes da janela"
                    ),
                    source = ForecastSource.LOCAL
                )
            }
        }
        return result
    }

    fun likelyType(
        risk: LocalRisk,
        recentWindows: List<SensorWindow>,
        crises: List<CrisisEvent>,
        now: Long
    ): CrisisType? {
        val last = recentWindows.lastOrNull()
        val soundsHigh = last != null &&
            (risk.factors.any { it.label.contains("Ruído", ignoreCase = true) } || last.soundOverloadIndex > 55f)
        val agitated = last != null && last.agitationIndex > 55f
        val tired = risk.factors.any { it.label.contains("Sono", ignoreCase = true) }
        val transitions = risk.factors.any { it.label.contains("Transições", ignoreCase = true) }
        return when {
            soundsHigh -> CrisisType.SOBRECARGA_SENSORIAL
            agitated -> CrisisType.MELTDOWN
            tired -> CrisisType.SHUTDOWN
            transitions -> CrisisType.SOBRECARGA_TDAH
            else -> crisisTypeMostCommon(crises, now)
        }
    }

    private fun crisisTypeMostCommon(crises: List<CrisisEvent>, now: Long): CrisisType? {
        if (crises.isEmpty()) return null
        val part = TimeUtils.dayPart(now)
        val samePart = crises.filter { TimeUtils.dayPart(it.startMillis) == part }
        val pool = if (samePart.size >= 2) samePart else crises
        return pool.groupingBy { it.type }.eachCount().maxByOrNull { it.value }?.key
    }

    fun preventivePlan(risk: LocalRisk): List<String> {
        val labels = risk.factors.map { it.label.lowercase() }
        val plan = linkedSetOf<String>()
        if (labels.any { it.contains("ruído") || it.contains("agudos") || it.contains("sonora") }) {
            plan += "Pausa sensorial de 10 min em local mais silencioso"
            plan += "Fones de proteção ou ruído branco bem baixo"
        }
        if (labels.any { it.contains("agitação") || it.contains("rotação") }) {
            plan += "Stimming seguro por alguns minutos"
            plan += "Respiração lenta: inspira 4 s, expira 6 s"
        }
        if (labels.any { it.contains("sono") }) {
            plan += "Reduza demandas hoje e proteja um bloco de descanso"
        }
        if (labels.any { it.contains("fome") || it.contains("refeição") }) {
            plan += "Coma algo simples e beba água"
        }
        if (labels.any { it.contains("transição") || it.contains("tensão") }) {
            plan += "Diminua estímulos e antecipe a próxima transição"
        }
        if (plan.isEmpty()) {
            plan += "Pausa sensorial curta"
            plan += "Hidratação"
            plan += "Reduzir estímulos por 10 minutos"
        }
        return plan.take(3)
    }

    private fun partPrior(crises: List<CrisisEvent>, part: DayPart): Double {
        if (crises.isEmpty()) return 0.25
        val count = crises.count { TimeUtils.dayPart(it.startMillis) == part }
        return (count + 1.0) / (crises.size + 4.0)
    }

    private fun confidenceFor(
        events: Int,
        horizon: PredictionHorizon,
        minutesAhead: Int
    ): ConfidenceLevel {
        var level = when {
            events >= 30 -> 2
            events >= 10 -> 1
            else -> 0
        }
        if (horizon == PredictionHorizon.HOJE && minutesAhead > 240) level--
        if (horizon == PredictionHorizon.SEMANA) level--
        if (minutesAhead > 720) level--
        return ConfidenceLevel.entries[level.coerceIn(0, 2)]
    }
}
