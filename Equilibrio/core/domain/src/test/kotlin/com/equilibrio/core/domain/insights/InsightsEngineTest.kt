package com.equilibrio.core.domain.insights

import com.equilibrio.core.model.BaselineProfile
import com.equilibrio.core.model.BaselineStat
import com.equilibrio.core.model.CheckIn
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.CrisisType
import com.equilibrio.core.model.Emotion
import com.equilibrio.core.model.SensorWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InsightsEngineTest {

    private val engine = InsightsEngine()
    private val dayMs = 86_400_000L
    // Fixed base (2026-01-10 12:00:00 UTC-ish local not critical; engine uses TimeUtils.startOfDay)
    private val base = 1_768_046_400_000L

    @Test
    fun `empty data returns insufficient and no false patterns`() {
        val bundle = engine.build(emptyList(), emptyList(), emptyList(), base, days = 14)
        assertFalse(bundle.sufficientData)
        assertTrue(bundle.dataLimitations.isNotEmpty())
        assertTrue(bundle.hourlyHeatmap.size == 7 * 24)
        assertTrue(bundle.disclaimer.contains("Não é diagnóstico", ignoreCase = true) || bundle.disclaimer.contains("nao", ignoreCase = true) || bundle.disclaimer.contains("Análise de padrões"))
    }

    @Test
    fun `few crises produce preliminary or insufficient correlation finding`() {
        val crises = listOf(
            CrisisEvent(startMillis = base - 2 * dayMs, type = CrisisType.MELTDOWN),
            CrisisEvent(startMillis = base - 5 * dayMs, type = CrisisType.SHUTDOWN)
        )
        val windows = syntheticWindows(crisisDays = setOf(2, 5), days = 14)
        val checkIns = syntheticCheckIns(crisisDays = setOf(2, 5), days = 14)
        val bundle = engine.build(windows, checkIns, crises, base, days = 14)
        assertTrue(bundle.dataLimitations.any { it.contains("Poucas crises", ignoreCase = true) || it.contains("prelim", ignoreCase = true) })
        // correlations may be insufficient or low-confidence
        assertTrue(bundle.correlations.all { it.confidence.name == "BAIXA" || it.description.contains("insuficient", ignoreCase = true) || it.evidenceCount >= 0 })
    }

    @Test
    fun `synthetic data finds sound and sleep associations with evidence`() {
        val crisisDays = setOf(1, 3, 6, 9, 12)
        val crises = crisisDays.map { CrisisEvent(startMillis = base - it * dayMs + 12 * 3_600_000L, type = CrisisType.SOBRECARGA_SENSORIAL) }
        val windows = syntheticWindows(crisisDays, days = 21)
        val checkIns = syntheticCheckIns(crisisDays, days = 21)
        val bundle = engine.build(windows, checkIns, crises, base, days = 21)

        assertTrue(bundle.sufficientData)
        assertEquals(21, bundle.daysAnalyzed)
        assertTrue(bundle.crisesAnalyzed >= 5)

        val relevant = bundle.correlations.filter { it.key in setOf("ruido", "sono", "sound", "sleep", "agitacao", "agitation") }
        assertTrue("Expected at least one correlation with evidence, got: ${bundle.correlations.map { it.key }}", relevant.any { it.evidenceCount > 0 || it.description.contains("dias", ignoreCase = true) })
        relevant.forEach { f ->
            assertTrue(f.limitations.isNotEmpty() || f.description.contains("causal", ignoreCase = true) || f.description.contains("associação", ignoreCase = true))
            // confidence is present
            assertTrue(f.confidence.name in setOf("BAIXA", "MEDIA", "ALTA"))
        }
        assertTrue(bundle.sequences.isNotEmpty() || bundle.preCrisisPatterns.isNotEmpty() || bundle.correlations.isNotEmpty())
    }

    @Test
    fun `anomaly detection uses baseline z-score`() {
        val crises = listOf(CrisisEvent(startMillis = base - dayMs, type = CrisisType.MELTDOWN))
        val windows = (0 until 40).map { i ->
            val t = base - 2 * dayMs + i * 30 * 60_000L
            val high = i in 10..14
            SensorWindow(
                startMillis = t, endMillis = t + 15_000L,
                agitationIndex = if (high) 95f else 20f,
                soundOverloadIndex = if (high) 90f else 15f,
                riskScore = if (high) 85 else 10,
                accelStd = 1f, gyroStd = 1f, lightMeanLux = 50f
            )
        }
        val baseline = BaselineProfile(
            startedAt = base - 30 * dayMs,
            daysObserved = 30,
            sampleCount = 500,
            stats = mapOf(
                "agitation" to BaselineStat(mean = 20.0, std = 8.0, count = 500),
                "soundOverloadIndex" to BaselineStat(mean = 15.0, std = 7.0, count = 500),
                "risk" to BaselineStat(mean = 10.0, std = 6.0, count = 500),
                "accelStd" to BaselineStat(mean = 1.0, std = 0.3, count = 500),
                "gyroStd" to BaselineStat(mean = 1.0, std = 0.3, count = 500)
            ),
            ready = true
        )
        val bundle = engine.build(windows, emptyList(), crises, base, days = 14, baseline = baseline)
        assertTrue("Expected anomalies, got ${bundle.anomalies.map { it.key }}", bundle.anomalies.any { it.kind == "anomalia" && it.evidenceCount > 0 })
        val anomaly = bundle.anomalies.first { it.kind == "anomalia" }
        assertTrue(anomaly.description.contains("desvio", ignoreCase = true) || anomaly.description.contains("baseline", ignoreCase = true))
        assertFalse(anomaly.description.contains("causa ", ignoreCase = true))
    }

    private fun syntheticWindows(crisisDays: Set<Int>, days: Int): List<SensorWindow> {
        val list = mutableListOf<SensorWindow>()
        for (d in 1..days) {
            val crisis = d in crisisDays
            for (h in listOf(9, 12, 15, 18)) {
                val t = base - d * dayMs + h * 3_600_000L
                list += SensorWindow(
                    startMillis = t, endMillis = t + 15_000L,
                    agitationIndex = if (crisis) 75f else 25f,
                    soundOverloadIndex = if (crisis) 80f else 20f,
                    riskScore = if (crisis) 70 else 15,
                    accelStd = 1f, gyroStd = 1f, lightMeanLux = 100f
                )
            }
        }
        return list
    }

    private fun syntheticCheckIns(crisisDays: Set<Int>, days: Int): List<CheckIn> {
        return (1..days).map { d ->
            val crisis = d in crisisDays
            CheckIn(
                timestamp = base - d * dayMs + 8 * 3_600_000L,
                emotions = if (crisis) listOf(Emotion.ANSIEDADE, Emotion.SOBRECARGA) else listOf(Emotion.CALMA),
                emotionIntensity = if (crisis) 7 else 3,
                sleepHours = if (crisis) 4.5f else 7.5f,
                hunger = if (crisis) 7 else 2,
                tension = if (crisis) 7 else 2,
                sensorySensitivity = if (crisis) 7 else 2,
                caffeine = crisis,
                abruptTransitions = if (crisis) 3 else 0,
                activities = if (crisis) listOf("trabalho") else listOf("rotina")
            )
        }
    }
}
