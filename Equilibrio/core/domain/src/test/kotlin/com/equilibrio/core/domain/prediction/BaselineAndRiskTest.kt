package com.equilibrio.core.domain.prediction

import com.equilibrio.core.model.BaselineProfile
import com.equilibrio.core.model.BaselineStat
import com.equilibrio.core.model.CheckIn
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.CrisisType
import com.equilibrio.core.model.SensorWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineAndRiskTest {

    private val day = 86_400_000L

    private fun window(
        start: Long,
        agitation: Float,
        sound: Float,
        impulsive: Int = 0,
        db: Float = 55f,
        riskScore: Int = 0
    ) = SensorWindow(
        startMillis = start,
        endMillis = start + 15_000,
        agitationIndex = agitation,
        soundOverloadIndex = sound,
        soundDbMean = db,
        impulsivePeaks = impulsive,
        highBandRatio = 0.2f,
        gyroStd = 0.5f,
        riskScore = riskScore
    )

    @Test
    fun `baseline learns mean and becomes ready after 14 days`() {
        val now = System.currentTimeMillis()
        val engine = BaselineEngine()
        val windows = (0 until 250).map { window(now - 250 * 30_000L + it * 30_000L, 20f, 30f) }
        val profile = engine.update(
            current = null,
            windows = windows,
            now = now,
            fallbackStart = now - 20 * day
        )
        assertTrue(profile.ready)
        assertTrue(profile.daysObserved >= 14)
        assertEquals(250, profile.sampleCount)
        val stat = profile.stats[FeatureKeys.AGITATION]!!
        assertEquals(20.0, stat.mean, 0.001)
        assertTrue(stat.std < 0.001)
    }

    @Test
    fun `risk stays in learning mode without baseline`() {
        val now = System.currentTimeMillis()
        val engine = RiskEngine()
        val risk = engine.evaluate(
            recentWindows = listOf(window(now - 15_000, 10f, 20f)),
            todayCheckIns = emptyList(),
            recentCrises = emptyList(),
            profile = null,
            now = now,
            micActive = true
        )
        assertFalse(risk.baselineReady)
        assertEquals(0, risk.score)
        assertTrue(risk.baselineProgress < 1f)
    }

    @Test
    fun `high sensory load raises risk above baseline`() {
        val now = System.currentTimeMillis()
        val profile = baselineProfile(now)
        val engine = RiskEngine()
        val high = (0 until 4).map {
            window(now - (4 - it) * 30_000L, 55f, 90f, impulsive = 14, db = 88f)
        }
        val risk = engine.evaluate(
            recentWindows = high,
            todayCheckIns = listOf(
                CheckIn(
                    timestamp = now - 3_600_000L,
                    sleepHours = 4.5f,
                    hunger = 8,
                    emotions = listOf(com.equilibrio.core.model.Emotion.SOBRECARGA),
                    emotionIntensity = 9
                )
            ),
            recentCrises = emptyList(),
            profile = profile,
            now = now,
            micActive = true
        )
        assertTrue(risk.baselineReady)
        assertTrue("score=${risk.score}", risk.score >= 60)
        assertTrue(risk.factors.isNotEmpty())
        assertTrue(risk.factors.size <= 5)
    }

    @Test
    fun `baseline values keep risk low`() {
        val now = System.currentTimeMillis()
        val profile = baselineProfile(now)
        val engine = RiskEngine()
        val calm = (0 until 4).map { window(now - (4 - it) * 30_000L, 27f, 35f, impulsive = 2, db = 60f) }
        val risk = engine.evaluate(
            recentWindows = calm,
            todayCheckIns = emptyList(),
            recentCrises = emptyList(),
            profile = profile,
            now = now,
            micActive = false
        )
        assertTrue("score=${risk.score}", risk.score < 40)
    }

    @Test
    fun `forecasts stay within probability bounds`() {
        val now = System.currentTimeMillis()
        val profile = baselineProfile(now)
        val windows = (0 until 8).map {
            window(now - (8 - it) * 30_000L, 40f, 55f, riskScore = 70)
        }
        val risk = RiskEngine().evaluate(windows, emptyList(), emptyList(), profile, now, true)
        val engine = ForecastEngine()
        val short = engine.shortTerm(risk, windows, emptyList(), now)
        assertEquals(3, short.size)
        assertTrue(short.all { it.probability in 0f..1f })
        assertTrue(short.zipWithNext().all { (a, b) -> a.windowEnd <= b.windowEnd })

        val today = engine.today(risk, emptyList(), now)
        assertTrue(today.all { it.windowStart >= now && it.probability in 0f..1f })
    }

    @Test
    fun `accuracy counts hits and false alarms`() {
        val now = System.currentTimeMillis()
        val crisis = CrisisEvent(
            startMillis = now + 30 * 60_000L,
            type = CrisisType.MELTDOWN,
            intensity = 8
        )
        val hit = com.equilibrio.core.model.PredictionRecord(
            createdAt = now,
            horizon = com.equilibrio.core.model.PredictionHorizon.CURTO,
            windowStart = now,
            windowEnd = now + 60 * 60_000L,
            probability = 0.8f,
            crisisType = CrisisType.MELTDOWN,
            confidence = com.equilibrio.core.model.ConfidenceLevel.MEDIA
        )
        val falseAlarm = hit.copy(
            id = 2,
            probability = 0.75f,
            outcome = com.equilibrio.core.model.PredictedOutcome.FALSO_ALARME
        )
        val report = AccuracyCalculator.compute(
            predictions = listOf(hit, falseAlarm),
            crises = listOf(crisis),
            periodStart = now - day,
            periodEnd = now + day
        )
        assertEquals(1, report.totalCrises)
        assertEquals(1, report.predictedCrises)
        assertEquals(1, report.falseAlarms)
        assertEquals(30, report.meanLeadMinutes)
        assertEquals(1f, report.recall, 0.001f)
        assertEquals(0.5f, report.precision, 0.001f)
    }

    @Test
    fun `platt calibration keeps probabilities in range`() {
        val model = PlattCalibration.fit(
            listOf(
                0.2 to false, 0.3 to false, 0.4 to false, 0.6 to true,
                0.7 to true, 0.8 to true, 0.55 to true, 0.25 to false
            )
        )
        assertTrue(model.calibrate(0.7) in 0.0..1.0)
        assertTrue(model.calibrate(0.3) in 0.0..1.0)
        val brier = PlattCalibration.brierScore(listOf(0.8 to true, 0.2 to false))
        assertEquals(0.04, brier, 0.001)
    }

    private fun baselineProfile(now: Long): BaselineProfile = BaselineProfile(
        startedAt = now - 20 * day,
        daysObserved = 20,
        sampleCount = 400,
        ready = true,
        stats = mapOf(
            FeatureKeys.AGITATION to BaselineStat(27.0, 5.0, 400),
            FeatureKeys.SOUND_OVERLOAD to BaselineStat(35.0, 6.0, 400),
            FeatureKeys.DB_MEAN to BaselineStat(60.0, 4.0, 400),
            FeatureKeys.DB_VARIABILITY to BaselineStat(3.0, 1.0, 400),
            FeatureKeys.IMPULSIVE to BaselineStat(2.0, 1.0, 400),
            FeatureKeys.HIGH_BAND to BaselineStat(0.2, 0.05, 400),
            FeatureKeys.ACCEL_STD to BaselineStat(0.5, 0.2, 400),
            FeatureKeys.GYRO_STD to BaselineStat(0.5, 0.2, 400),
            FeatureKeys.LIGHT to BaselineStat(4.0, 1.0, 400),
            FeatureKeys.MOTION_RMS to BaselineStat(9.8, 0.5, 400)
        )
    )
}
