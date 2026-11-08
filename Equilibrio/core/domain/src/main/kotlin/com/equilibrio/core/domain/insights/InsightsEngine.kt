package com.equilibrio.core.domain.insights

import com.equilibrio.core.domain.time.TimeUtils
import com.equilibrio.core.model.BaselineProfile
import com.equilibrio.core.model.CheckIn
import com.equilibrio.core.model.ConfidenceLevel
import com.equilibrio.core.model.CorrelationInsight
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.DayPart
import com.equilibrio.core.model.HeatmapCell
import com.equilibrio.core.model.SensorWindow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class AggregationBucket(
    val label: String,
    val startMillis: Long,
    val endMillis: Long,
    val windowCount: Int = 0,
    val checkInCount: Int = 0,
    val crisisCount: Int = 0,
    val avgAgitation: Float = 0f,
    val avgSoundOverload: Float = 0f,
    val avgSleepHours: Float = 0f
)

data class InsightsBundle(
    val correlations: List<CorrelationInsight>,
    val heatmap: List<HeatmapCell>,
    val hourlyHeatmap: List<HeatmapCell> = emptyList(),
    val topTriggers: List<Pair<String, Int>>,
    val topHelped: List<Pair<String, Int>>,
    val preCrisisPatterns: List<String>,
    val recurringPatterns: List<String>,
    val anomalies: List<CorrelationInsight> = emptyList(),
    val trends: List<CorrelationInsight> = emptyList(),
    val sequences: List<CorrelationInsight> = emptyList(),
    val hourlyAggregations: List<AggregationBucket> = emptyList(),
    val dailyAggregations: List<AggregationBucket> = emptyList(),
    val weeklyAggregations: List<AggregationBucket> = emptyList(),
    val daysAnalyzed: Int,
    val crisesAnalyzed: Int,
    val sufficientData: Boolean = false,
    val dataLimitations: List<String> = emptyList(),
    val disclaimer: String = DISCLAIMER
)

private const val DISCLAIMER =
    "Análise de padrões para autoconhecimento. Não é diagnóstico, não afirma causalidade e não substitui avaliação profissional."

/**
 * Motor local de análise de padrões (sem IA). Produz achados auditáveis com
 * evidência, força/confiança e limitações. Nunca afirma causalidade.
 */
class InsightsEngine {

    fun build(
        windows: List<SensorWindow>,
        checkIns: List<CheckIn>,
        crises: List<CrisisEvent>,
        now: Long,
        days: Int = 28,
        baseline: BaselineProfile? = null
    ): InsightsBundle {
        val start = TimeUtils.startOfDay(now) - (days - 1) * 86_400_000L
        val filteredWindows = windows.filter { it.startMillis >= start }
        val filteredCheckIns = checkIns.filter { it.timestamp >= start }
        val filteredCrises = crises.filter { it.startMillis >= start }
        val actualCrises = filteredCrises.filter { it.type.isActualCrisis }

        val limitations = mutableListOf<String>()
        if (filteredWindows.isEmpty() && filteredCheckIns.isEmpty() && filteredCrises.isEmpty()) {
            limitations += "Sem registros no período analisado."
        }
        if (actualCrises.size < 3) {
            limitations += "Poucas crises registradas (${actualCrises.size}); associações são preliminares."
        }
        if (days < 7) limitations += "Período curto; padrões semanais podem não aparecer."
        if (baseline?.ready != true) limitations += "Baseline pessoal incompleto; detecção de anomalias é limitada."

        val correlations = correlations(filteredWindows, filteredCheckIns, filteredCrises, start, now)
        val anomalies = anomalyFindings(filteredWindows, filteredCrises, baseline)
        val trends = trendFindings(filteredWindows, filteredCheckIns, filteredCrises, start, now)
        val sequences = sequenceFindings(filteredWindows, filteredCheckIns, filteredCrises)
        val heatmap = heatmap(filteredCrises)
        val hourlyHeatmap = hourlyHeatmap(filteredCrises)
        val triggers = countTags(filteredCrises.flatMap { it.triggers })
        val helped = countTags(filteredCrises.flatMap { it.helped })
        val preCrisis = preCrisisPatterns(filteredWindows, filteredCheckIns, filteredCrises)
        val recurring = (preCrisis + temporalPatterns(filteredCrises)).distinct()

        val sufficient = (filteredWindows.size + filteredCheckIns.size) >= 10 || filteredCrises.isNotEmpty()

        return InsightsBundle(
            correlations = correlations,
            heatmap = heatmap,
            hourlyHeatmap = hourlyHeatmap,
            topTriggers = triggers.take(8),
            topHelped = helped.take(6),
            preCrisisPatterns = preCrisis,
            recurringPatterns = recurring,
            anomalies = anomalies,
            trends = trends,
            sequences = sequences,
            hourlyAggregations = aggregateHourly(filteredWindows, filteredCheckIns, filteredCrises, start, now),
            dailyAggregations = aggregateDaily(filteredWindows, filteredCheckIns, filteredCrises, start, now),
            weeklyAggregations = aggregateWeekly(filteredWindows, filteredCheckIns, filteredCrises, start, now),
            daysAnalyzed = days,
            crisesAnalyzed = filteredCrises.size,
            sufficientData = sufficient,
            dataLimitations = limitations.distinct(),
            disclaimer = DISCLAIMER
        )
    }

    fun heatmap(crises: List<CrisisEvent>): List<HeatmapCell> {
        val counts = mutableMapOf<Pair<Int, DayPart>, Int>()
        for (crisis in crises) {
            val key = TimeUtils.dayOfWeek(crisis.startMillis) to TimeUtils.dayPart(crisis.startMillis)
            counts[key] = (counts[key] ?: 0) + 1
        }
        val maxCount = counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        return buildList {
            for (day in 1..7) for (part in DayPart.entries) {
                val count = counts[day to part] ?: 0
                add(HeatmapCell(day, part, count, count.toFloat() / maxCount))
            }
        }
    }

    fun hourlyHeatmap(crises: List<CrisisEvent>): List<HeatmapCell> {
        val counts = mutableMapOf<Pair<Int, Int>, Int>() // dayOfWeek(1..7) x hour(0..23)
        for (crisis in crises) {
            val z = Instant.ofEpochMilli(crisis.startMillis).atZone(ZoneId.systemDefault())
            val day = ((z.dayOfWeek.value % 7) + 1) // java.time Mon=1..Sun=7 -> keep 1=dom? TimeUtils uses 1=domingo
            val mappedDay = TimeUtils.dayOfWeek(crisis.startMillis)
            val hour = z.hour
            val key = mappedDay to hour
            counts[key] = (counts[key] ?: 0) + 1
        }
        val maxCount = counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        return buildList {
            for (day in 1..7) for (hour in 0..23) {
                val count = counts[day to hour] ?: 0
                add(HeatmapCell(day, DayPart.fromHour(hour), count, count.toFloat() / maxCount, hour = hour))
            }
        }
    }

    fun preCrisisPatterns(
        windows: List<SensorWindow>,
        checkIns: List<CheckIn>,
        crises: List<CrisisEvent>
    ): List<String> {
        val actual = crises.filter { it.type.isActualCrisis }
        if (actual.size < 3) return emptyList()
        val soundThreshold = percentile(windows.map { it.soundOverloadIndex.toDouble() }, 0.75).toFloat()

        var soundCount = 0; var sleepCount = 0; var hungerCount = 0; var transitionCount = 0
        var agitationCount = 0; var moodCount = 0; var taskCount = 0; var consumptionCount = 0

        for (crisis in actual) {
            val from = crisis.startMillis - 6 * 3_600_000L
            val w = windows.filter { it.endMillis in from..crisis.startMillis }
            val c = checkIns.filter { it.timestamp in from..crisis.startMillis }
            if (w.isNotEmpty() && w.map { it.soundOverloadIndex }.average() >= soundThreshold && soundThreshold > 25.0) soundCount++
            if (w.isNotEmpty() && w.map { it.agitationIndex }.average() >= 60.0) agitationCount++
            if (c.any { it.sleepHours in 0.1f..5.9f }) sleepCount++
            if (c.any { it.hunger >= 7 || it.pain >= 7 || it.tension >= 7 }) hungerCount++
            if (c.sumOf { it.abruptTransitions } >= 2) transitionCount++
            if (c.any { it.emotions.any { e -> e.valence < 0 } && it.emotionIntensity >= 6 }) moodCount++
            if (c.any { it.activities.isNotEmpty() }) taskCount++
            if (c.any { it.caffeine || it.consumed.isNotEmpty() }) consumptionCount++
        }

        return buildList {
            fun addIf(count: Int, msg: String) { if (count >= 2) add("$msg em $count de ${actual.size} crises (associação observada, não causal).") }
            addIf(soundCount, "Ruído/sobrecarga sonora mais alta nas 6 h anteriores")
            addIf(agitationCount, "Agitação motora mais alta nas 6 h anteriores")
            addIf(sleepCount, "Sono curto (< 6 h) registrado antes")
            addIf(hungerCount, "Desconforto físico alto (fome/dor/tensão) antes")
            addIf(transitionCount, "Transições bruscas (>=2) antes")
            addIf(moodCount, "Humor negativo intenso registrado antes")
            addIf(consumptionCount, "Consumo/cafeína registrado no período anterior")
        }
    }

    fun correlations(
        windows: List<SensorWindow>,
        checkIns: List<CheckIn>,
        crises: List<CrisisEvent>,
        startMillis: Long,
        endMillis: Long
    ): List<CorrelationInsight> {
        val startDay = TimeUtils.startOfDay(startMillis)
        val endDay = TimeUtils.startOfDay(endMillis)
        val dayCount = ((endDay - startDay) / 86_400_000L + 1).toInt().coerceIn(1, 90)
        if (dayCount < 5) {
            return listOf(insufficientFinding("correlacoes", "Correlações", 0, emptyList(), "Menos de 5 dias no período."))
        }

        data class DayAgg(
            val sound: MutableList<Double> = mutableListOf(),
            val agitation: MutableList<Double> = mutableListOf(),
            var sleep: Float? = null,
            var transitions: Int = 0,
            var caffeine: Boolean = false,
            var discomfort: Boolean = false,
            var negativeMood: Boolean = false,
            var tasks: Int = 0,
            var consumption: Boolean = false
        )

        val days = Array(dayCount) { DayAgg() }
        fun idx(millis: Long): Int = (((TimeUtils.startOfDay(millis) - startDay) / 86_400_000L).toInt()).coerceIn(0, dayCount - 1)

        for (w in windows) if (w.startMillis in startMillis..endMillis) {
            val d = days[idx(w.startMillis)]
            d.sound += w.soundOverloadIndex.toDouble(); d.agitation += w.agitationIndex.toDouble()
        }
        for (c in checkIns) if (c.timestamp in startMillis..endMillis) {
            val d = days[idx(c.timestamp)]
            c.sleepHours.takeIf { it > 0f }?.let { d.sleep = maxOf(d.sleep ?: 0f, it) }
            d.transitions += c.abruptTransitions
            d.caffeine = d.caffeine || c.caffeine
            d.consumption = d.consumption || c.caffeine || c.consumed.isNotEmpty()
            d.discomfort = d.discomfort || c.hunger >= 7 || c.pain >= 7 || c.tension >= 7
            d.negativeMood = d.negativeMood || c.emotions.any { it.valence < 0 }
            d.tasks += c.activities.size
        }
        val crisisDays = BooleanArray(dayCount)
        val crisisDates = mutableListOf<Long>()
        for (c in crises) if (c.startMillis in startMillis..endMillis && c.type.isActualCrisis) {
            val i = idx(c.startMillis); crisisDays[i] = true; crisisDates += TimeUtils.startOfDay(c.startMillis)
        }

        val crisisCount = crisisDays.count { it }
        if (crisisCount < 2 || crisisCount == dayCount) {
            return listOf(insufficientFinding("correlacoes", "Correlações", crisisCount, crisisDates.distinct(),
                if (crisisCount < 2) "É preciso ter dias com e sem crise para comparar." else "Todos os dias têm crise no período."))
        }

        val insights = mutableListOf<CorrelationInsight>()
        val baseLimits = listOf("Comparação por dia; não controla todas as variáveis.", "Associação observada, não causal.")

        fun conf(strength: Int, n: Int): ConfidenceLevel = when {
            n >= 14 && strength >= 3 -> ConfidenceLevel.MEDIA
            n >= 21 && strength >= 4 -> ConfidenceLevel.ALTA
            strength >= 2 -> ConfidenceLevel.BAIXA
            else -> ConfidenceLevel.BAIXA
        }

        fun addContinuous(key: String, label: String, unit: String, values: List<Double?>) {
            val pairs = values.mapIndexedNotNull { i, v -> v?.let { i to it } }
            if (pairs.size < 6) return
            val withC = pairs.filter { crisisDays[it.first] }.map { it.second }
            val without = pairs.filter { !crisisDays[it.first] }.map { it.second }
            if (withC.size < 2 || without.size < 2) return
            val r = pointBiserial(withC, without)
            if (abs(r) < 0.25) return
            val direction = if (r > 0) "maior" else "menor"
            val m1 = withC.average(); val m0 = without.average()
            val strength = (abs(r) * 5).toInt().coerceIn(1, 5)
            insights += CorrelationInsight(
                key = key, label = label, correlation = r, strength = strength,
                description = "Em dias com crise, $label foi $direction (média %.1f%s vs %.1f%s; %d dias com crise, %d sem)."
                    .format(Locale.US, m1, unit, m0, unit, withC.size, without.size),
                evidenceCount = withC.size + without.size,
                evidenceDates = crisisDates.distinct().sorted().takeLast(8),
                confidence = conf(strength, pairs.size),
                limitations = baseLimits,
                kind = "correlacao"
            )
        }

        addContinuous("sono", "sono (h)", " h", days.map { it.sleep?.toDouble() })
        addContinuous("ruido", "sobrecarga sonora", "", days.map { it.sound.takeIf { l -> l.isNotEmpty() }?.average() })
        addContinuous("agitacao", "agitação motora", "", days.map { it.agitation.takeIf { l -> l.isNotEmpty() }?.average() })
        addContinuous("tarefas", "tarefas/atividades registradas", "", days.map { it.tasks.toDouble() })

        fun addBinary(key: String, label: String, predicate: (DayAgg) -> Boolean) {
            val wC = days.filterIndexed { i, _ -> crisisDays[i] }.count(predicate)
            val wo = days.filterIndexed { i, _ -> !crisisDays[i] }.count(predicate)
            val n1 = crisisCount; val n0 = dayCount - crisisCount
            if (n1 < 2 || n0 < 2) return
            val p1 = wC.toDouble() / n1; val p0 = wo.toDouble() / n0
            val diff = p1 - p0
            if (abs(diff) < 0.2) return
            val strength = (abs(diff) * 5).toInt().coerceIn(1, 5)
            insights += CorrelationInsight(
                key = key, label = label, correlation = diff, strength = strength,
                description = "%s apareceu em %.0f%% dos dias com crise (%d/%d) e %.0f%% dos sem crise (%d/%d)."
                    .format(label, p1 * 100, wC, n1, p0 * 100, wo, n0),
                evidenceCount = n1 + n0,
                evidenceDates = crisisDates.distinct().sorted().takeLast(8),
                confidence = conf(strength, dayCount),
                limitations = baseLimits,
                kind = "correlacao"
            )
        }

        addBinary("consumo", "Consumo/cafeína", DayAgg::consumption)
        addBinary("humor", "Humor negativo registrado", DayAgg::negativeMood)
        addBinary("desconforto", "Desconforto físico alto", DayAgg::discomfort)
        addBinary("transicoes", "Transições bruscas", { it.transitions >= 2 })

        return insights.sortedByDescending { abs(it.correlation) }.take(12)
    }

    private fun anomalyFindings(
        windows: List<SensorWindow>,
        crises: List<CrisisEvent>,
        baseline: BaselineProfile?
    ): List<CorrelationInsight> {
        if (baseline?.ready != true || windows.size < 10) {
            return if (baseline?.ready != true) listOf(insufficientFinding(
                "anomalias", "Anomalias", windows.size, emptyList(),
                "Baseline pessoal não está pronto para comparar desvios (z-score)."
            )) else emptyList()
        }
        val actual = crises.filter { it.type.isActualCrisis }
        val findings = mutableListOf<CorrelationInsight>()
        data class FDef(val key: String, val label: String, val value: (SensorWindow) -> Double)
        val defs = listOf(
            FDef("agitation", "agitação motora") { it.agitationIndex.toDouble() },
            FDef("accelStd", "variação de movimento") { it.accelStd.toDouble() },
            FDef("gyroStd", "instabilidade/giroscópio") { it.gyroStd.toDouble() },
            FDef("soundOverloadIndex", "sobrecarga sonora") { it.soundOverloadIndex.toDouble() },
            FDef("risk", "risco estimado") { it.riskScore.toDouble() }
        )
        for (def in defs) {
            val stat = baseline.stats[def.key] ?: continue
            if (stat.count < 20 || stat.std < 1e-6) continue
            val anomalous = windows.filter { abs((def.value(it) - stat.mean) / stat.std) >= 2.0 }
            if (anomalous.isEmpty()) continue
            val nearCrisis = anomalous.count { w -> actual.any { c -> abs(c.startMillis - w.endMillis) <= 3 * 3_600_000L } }
            val dates = anomalous.map { TimeUtils.startOfDay(it.startMillis) }.distinct().sorted().takeLast(8)
            val strength = min(5, max(1, anomalous.size / 5 + if (nearCrisis >= 2) 1 else 0))
            findings += CorrelationInsight(
                key = "anomalia_${def.key}", label = "Anomalia: ${def.label}",
                correlation = anomalous.size.toDouble() / windows.size.coerceAtLeast(1),
                strength = strength,
                description = "${anomalous.size} janelas com desvio >= 2 desvios-padrão do baseline pessoal" +
                    (if (nearCrisis > 0) "; $nearCrisis ocorreram até 3 h de crises registradas (associação temporal, não causal)." else "."),
                evidenceCount = anomalous.size, evidenceDates = dates,
                confidence = if (stat.count >= 100 && anomalous.size >= 5) ConfidenceLevel.MEDIA else ConfidenceLevel.BAIXA,
                limitations = listOf("Z-score compara com o baseline pessoal.", "Desvio alto não implica crise."),
                kind = "anomalia"
            )
        }
        return findings.sortedByDescending { it.evidenceCount }.take(6)
    }

    private fun trendFindings(
        windows: List<SensorWindow>,
        checkIns: List<CheckIn>,
        crises: List<CrisisEvent>,
        startMillis: Long,
        endMillis: Long
    ): List<CorrelationInsight> {
        val buckets = aggregateDaily(windows, checkIns, crises, startMillis, endMillis)
        if (buckets.size < 7) return listOf(insufficientFinding("tendencias", "Tendências", buckets.size, emptyList(), "Menos de 7 dias para estimar tendência."))
        fun slope(values: List<Double>): Double {
            val n = values.size; if (n < 2) return 0.0
            val xs = (0 until n).map { it.toDouble() }; val mx = xs.average(); val my = values.average()
            val num = xs.zip(values).sumOf { (x, y) -> (x - mx) * (y - my) }
            val den = xs.sumOf { (it - mx) * (it - mx) }
            return if (den < 1e-9) 0.0 else num / den
        }
        val findings = mutableListOf<CorrelationInsight>()
        fun addTrend(key: String, label: String, values: List<Double>) {
            if (values.all { it == 0.0 }) return
            val s = slope(values); val range = (values.maxOrNull() ?: 0.0) - (values.minOrNull() ?: 0.0)
            if (abs(s) < 0.01 && range < 1.0) return
            val dir = when { s > 0.05 -> "alta"; s < -0.05 -> "queda"; else -> "estável" }
            val strength = (abs(s) * 20).toInt().coerceIn(1, 5)
            findings += CorrelationInsight(
                key = "tendencia_$key", label = "Tendência: $label", correlation = s, strength = strength,
                description = "Tendência de $dir ao longo de ${values.size} dias (inclinação diária ~${"%.2f".format(Locale.US, s)}).",
                evidenceCount = values.size, evidenceDates = buckets.takeLast(7).map { it.startMillis },
                confidence = if (values.size >= 14) ConfidenceLevel.MEDIA else ConfidenceLevel.BAIXA,
                limitations = listOf("Tendência linear simples; pode haver variação diária."), kind = "tendencia"
            )
        }
        addTrend("agitacao", "agitação motora", buckets.map { it.avgAgitation.toDouble() })
        addTrend("som", "sobrecarga sonora", buckets.map { it.avgSoundOverload.toDouble() })
        addTrend("crises", "crises por dia", buckets.map { it.crisisCount.toDouble() })
        return findings
    }

    private fun sequenceFindings(
        windows: List<SensorWindow>,
        checkIns: List<CheckIn>,
        crises: List<CrisisEvent>
    ): List<CorrelationInsight> {
        val actual = crises.filter { it.type.isActualCrisis }
        if (actual.size < 3) return listOf(insufficientFinding("sequencias", "Sequências antes de eventos", actual.size, emptyList(), "Menos de 3 crises para analisar sequências."))
        val findings = mutableListOf<CorrelationInsight>()
        fun seq(key: String, label: String, predicate: (CrisisEvent) -> Boolean) {
            val matched = actual.filter(predicate)
            if (matched.size < 2) return
            findings += CorrelationInsight(
                key = "seq_$key", label = "Sequência: $label", correlation = matched.size.toDouble() / actual.size,
                strength = min(5, max(1, (matched.size * 5 / actual.size))),
                description = "$label precedeu ${matched.size} de ${actual.size} crises nas 6 h anteriores (padrão observado, não causal).",
                evidenceCount = matched.size, evidenceDates = matched.map { it.startMillis }.sorted().takeLast(8),
                confidence = if (matched.size >= 4 && actual.size >= 5) ConfidenceLevel.MEDIA else ConfidenceLevel.BAIXA,
                limitations = listOf("Baseado em registros disponíveis nas 6 h antes.", "Pode haver fatores não registrados."),
                kind = "sequencia"
            )
        }
        seq("sono_ruido", "Sono curto + som alto") { c ->
            val from = c.startMillis - 6 * 3_600_000L
            checkIns.any { it.timestamp in from..c.startMillis && it.sleepHours in 0.1f..5.9f } &&
                windows.filter { it.endMillis in from..c.startMillis }.any { it.soundOverloadIndex >= 60f }
        }
        seq("agitacao_transicao", "Agitação + transições") { c ->
            val from = c.startMillis - 6 * 3_600_000L
            checkIns.filter { it.timestamp in from..c.startMillis }.sumOf { it.abruptTransitions } >= 2 &&
                windows.filter { it.endMillis in from..c.startMillis }.any { it.agitationIndex >= 60f }
        }
        seq("humor_desconforto", "Humor negativo + desconforto") { c ->
            val from = c.startMillis - 6 * 3_600_000L
            checkIns.any { it.timestamp in from..c.startMillis && it.emotions.any { e -> e.valence < 0 } &&
                (it.hunger >= 6 || it.pain >= 6 || it.tension >= 6) }
        }
        return findings
    }

    private fun temporalPatterns(crises: List<CrisisEvent>): List<String> = buildList {
        val actual = crises.filter { it.type.isActualCrisis }
        if (actual.size >= 3) {
            val byPart = actual.groupingBy { TimeUtils.dayPart(it.startMillis) }.eachCount()
            byPart.maxByOrNull { it.value }?.let { add("${(it.value * 100 / actual.size)}% das crises ocorreram à ${it.key.label.lowercase()}.") }
            val byWeekday = actual.groupingBy { TimeUtils.dayOfWeek(it.startMillis) }.eachCount()
            byWeekday.maxByOrNull { it.value }?.let { add("${it.value} crises concentradas em ${TimeUtils.weekdayLabel(it.key)}.") }
        }
    }

    private fun aggregateHourly(w: List<SensorWindow>, c: List<CheckIn>, cr: List<CrisisEvent>, start: Long, end: Long): List<AggregationBucket> {
        if (w.isEmpty() && c.isEmpty() && cr.isEmpty()) return emptyList()
        val hourMs = 3_600_000L
        val from = (start / hourMs) * hourMs
        val to = min(end, from + 7 * 24 * hourMs)
        val buckets = mutableListOf<AggregationBucket>()
        var t = from
        while (t < to) {
            val e = t + hourMs
            val ww = w.filter { it.startMillis in t until e }
            val cc = c.filter { it.timestamp in t until e }
            val crr = cr.filter { it.startMillis in t until e }
            if (ww.isNotEmpty() || cc.isNotEmpty() || crr.isNotEmpty()) {
                buckets += AggregationBucket(fmtHour(t), t, e, ww.size, cc.size, crr.size,
                    ww.map { it.agitationIndex }.average().toFloat().takeIf { ww.isNotEmpty() } ?: 0f,
                    ww.map { it.soundOverloadIndex }.average().toFloat().takeIf { ww.isNotEmpty() } ?: 0f,
                    cc.mapNotNull { it.sleepHours.takeIf { s -> s > 0f } }.average().toFloat().takeIf { cc.any { it.sleepHours > 0f } } ?: 0f)
            }
            t = e
        }
        return buckets.takeLast(168)
    }

    private fun aggregateDaily(w: List<SensorWindow>, c: List<CheckIn>, cr: List<CrisisEvent>, start: Long, end: Long): List<AggregationBucket> {
        val startDay = TimeUtils.startOfDay(start)
        val endDay = TimeUtils.startOfDay(end)
        val dayCount = ((endDay - startDay) / 86_400_000L + 1).toInt().coerceIn(1, 90)
        return (0 until dayCount).map { i ->
            val s = startDay + i * 86_400_000L; val e = s + 86_400_000L
            val ww = w.filter { it.startMillis in s until e }
            val cc = c.filter { it.timestamp in s until e }
            val crr = cr.filter { it.startMillis in s until e }
            AggregationBucket(fmtDay(s), s, e, ww.size, cc.size, crr.size,
                ww.map { it.agitationIndex }.average().toFloat().takeIf { ww.isNotEmpty() } ?: 0f,
                ww.map { it.soundOverloadIndex }.average().toFloat().takeIf { ww.isNotEmpty() } ?: 0f,
                cc.mapNotNull { it.sleepHours.takeIf { sh -> sh > 0f } }.average().toFloat().takeIf { cc.any { it.sleepHours > 0f } } ?: 0f)
        }
    }

    private fun aggregateWeekly(w: List<SensorWindow>, c: List<CheckIn>, cr: List<CrisisEvent>, start: Long, end: Long): List<AggregationBucket> {
        val daily = aggregateDaily(w, c, cr, start, end)
        if (daily.isEmpty()) return emptyList()
        return daily.chunked(7).mapIndexed { i, chunk ->
            val s = chunk.first().startMillis; val e = chunk.last().endMillis
            AggregationBucket("Semana ${i + 1}", s, e,
                chunk.sumOf { it.windowCount }, chunk.sumOf { it.checkInCount }, chunk.sumOf { it.crisisCount },
                chunk.map { it.avgAgitation }.filter { it > 0f }.average().toFloat().let { if (it.isNaN()) 0f else it },
                chunk.map { it.avgSoundOverload }.filter { it > 0f }.average().toFloat().let { if (it.isNaN()) 0f else it },
                chunk.map { it.avgSleepHours }.filter { it > 0f }.average().toFloat().let { if (it.isNaN()) 0f else it })
        }
    }

    private fun insufficientFinding(key: String, label: String, evidence: Int, dates: List<Long>, reason: String) =
        CorrelationInsight(key, label, 0.0, 0, "Dados insuficientes para $label: $reason",
            evidence, dates, ConfidenceLevel.BAIXA, listOf(reason, DISCLAIMER), "insuficiente")

    private fun pointBiserial(groupA: List<Double>, groupB: List<Double>): Double {
        val nA = groupA.size; val nB = groupB.size; val n = nA + nB
        if (n < 4) return 0.0
        val meanA = groupA.average(); val meanB = groupB.average()
        val all = groupA + groupB; val mean = all.average()
        val sd = sqrt(all.sumOf { (it - mean) * (it - mean) } / n)
        if (sd < 1e-9) return 0.0
        return ((meanA - meanB) / sd) * sqrt((nA.toDouble() * nB) / (n * n.toDouble()))
    }

    private fun countTags(tags: List<String>): List<Pair<String, Int>> =
        tags.filter { it.isNotBlank() }.groupingBy { it.trim().lowercase() }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key to it.value }

    private fun percentile(values: List<Double>, p: Double): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        return sorted[((sorted.size - 1) * p).toInt().coerceIn(0, sorted.lastIndex)]
    }

    private fun fmtDay(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM"))

    private fun fmtHour(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM HH'h'"))
}
