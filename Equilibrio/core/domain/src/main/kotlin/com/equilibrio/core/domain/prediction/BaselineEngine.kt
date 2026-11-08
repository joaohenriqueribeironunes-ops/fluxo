package com.equilibrio.core.domain.prediction

import com.equilibrio.core.model.BaselineProfile
import com.equilibrio.core.model.BaselineStat
import com.equilibrio.core.model.SensorWindow
import com.equilibrio.core.domain.signal.SignalStats
import kotlin.math.sqrt

object FeatureKeys {
    const val AGITATION = "agitation"
    const val SOUND_OVERLOAD = "sound_overload"
    const val DB_MEAN = "db_mean"
    const val DB_VARIABILITY = "db_variability"
    const val IMPULSIVE = "impulsive"
    const val HIGH_BAND = "high_band"
    const val ACCEL_STD = "accel_std"
    const val GYRO_STD = "gyro_std"
    const val LIGHT = "light"
    const val MOTION_RMS = "motion_rms"
    const val RISK = "risk"
}

/**
 * Aprende média e desvio padrão de cada feature por pessoa (Welford).
 * "Normal" é o que é normal para esta pessoa, não para a população.
 */
class BaselineEngine(
    private val minSamples: Int = 200,
    private val minDays: Int = 14
) {

    fun valuesOf(window: SensorWindow): Map<String, Double> = buildMap {
        put(FeatureKeys.AGITATION, window.agitationIndex.toDouble())
        put(FeatureKeys.ACCEL_STD, window.accelStd.toDouble())
        put(FeatureKeys.GYRO_STD, window.gyroStd.toDouble())
        put(FeatureKeys.LIGHT, kotlin.math.ln(1.0 + window.lightMeanLux.toDouble()))
        put(FeatureKeys.MOTION_RMS, window.accelRms.toDouble())
        // As features de som só entram no baseline quando o microfone capturou
        // naquela janela; caso contrário os zeros distorceriam a média pessoal.
        if (window.micCaptured) {
            put(FeatureKeys.SOUND_OVERLOAD, window.soundOverloadIndex.toDouble())
            put(FeatureKeys.DB_MEAN, window.soundDbMean.toDouble())
            put(FeatureKeys.DB_VARIABILITY, window.soundDbVariability.toDouble())
            put(FeatureKeys.IMPULSIVE, window.impulsivePeaks.toDouble())
            put(FeatureKeys.HIGH_BAND, window.highBandRatio.toDouble())
        }
    }

    fun update(
        current: BaselineProfile?,
        windows: List<SensorWindow>,
        now: Long,
        fallbackStart: Long = now
    ): BaselineProfile {
        val startedAt = current?.startedAt?.takeIf { it > 0 } ?: fallbackStart
        val stats = (current?.stats ?: emptyMap()).toMutableMap()
        var totalCount = current?.sampleCount ?: 0

        for (window in windows) {
            val values = valuesOf(window)
            for ((key, value) in values) {
                if (!value.isFinite()) continue
                stats[key] = updateStat(stats[key], value)
            }
            totalCount++
        }

        val days = (((now - startedAt) / 86_400_000L).toInt() + 1).coerceAtLeast(1)
        return BaselineProfile(
            startedAt = startedAt,
            daysObserved = days,
            sampleCount = totalCount,
            stats = stats,
            ready = days >= minDays && totalCount >= minSamples
        )
    }

    fun zScores(window: SensorWindow, profile: BaselineProfile): Map<String, Double> {
        val values = valuesOf(window)
        return values.mapValues { (key, value) ->
            val stat = profile.stats[key] ?: return@mapValues 0.0
            SignalStats.zScore(value, stat.mean, stat.std)
        }
    }

    fun zScore(key: String, value: Double, profile: BaselineProfile): Double {
        val stat = profile.stats[key] ?: return 0.0
        return SignalStats.zScore(value, stat.mean, stat.std)
    }

    private fun updateStat(existing: BaselineStat?, value: Double): BaselineStat {
        if (existing == null || existing.count <= 0) return BaselineStat(value, 0.0, 1)
        val n = existing.count + 1
        val oldM2 = existing.std * existing.std * (existing.count - 1)
        val delta = value - existing.mean
        val mean = existing.mean + delta / n
        val m2 = oldM2 + delta * (value - mean)
        val std = sqrt((m2 / (n - 1)).coerceAtLeast(0.0))
        return BaselineStat(mean, std, n)
    }
}
