package com.equilibrio.core.domain.signal

import kotlin.math.abs
import kotlin.math.sqrt

data class WindowStats(
    val mean: Double,
    val stdDev: Double,
    val rms: Double,
    val peakCount: Int,
    val max: Double
)

object SignalStats {

    /**
     * Estatísticas de uma janela. Picos são máximos locais acima de
     * mean + sigma*std, com separação mínima em amostras para não contar
     * ruído de alta frequência repetidamente.
     */
    fun of(
        values: DoubleArray,
        sigma: Double = 2.0,
        minSeparation: Int = 3
    ): WindowStats {
        if (values.isEmpty()) return WindowStats(0.0, 0.0, 0.0, 0, 0.0)
        var sum = 0.0
        var sumSq = 0.0
        var max = Double.NEGATIVE_INFINITY
        for (v in values) {
            sum += v
            sumSq += v * v
            if (v > max) max = v
        }
        val mean = sum / values.size
        val rms = sqrt(sumSq / values.size)
        var variance = 0.0
        for (v in values) {
            val d = v - mean
            variance += d * d
        }
        val std = sqrt(variance / values.size)
        val threshold = mean + sigma * std
        var peaks = 0
        var lastPeak = -minSeparation - 1
        for (i in 1 until values.size - 1) {
            if (values[i] > threshold &&
                values[i] >= values[i - 1] &&
                values[i] > values[i + 1] &&
                i - lastPeak > minSeparation
            ) {
                peaks++
                lastPeak = i
            }
        }
        return WindowStats(mean, std, rms, peaks, max)
    }

    /**
     * Índice de agitação motora (0..100). Combina desvio do acelerômetro,
     * rotação do giroscópio, taxa de picos e energia espectral.
     * Não é diagnóstico: é um sinal relativo ao baseline da própria pessoa.
     */
    fun agitationIndex(
        accelStd: Double,
        gyroStd: Double,
        peakCount: Int,
        durationSeconds: Double,
        spectralEnergy: Double
    ): Double {
        val duration = durationSeconds.coerceAtLeast(1.0)
        val movement = (accelStd / 1.6).coerceIn(0.0, 1.0) * 38.0
        val rotation = (gyroStd / 1.4).coerceIn(0.0, 1.0) * 30.0
        val peakRate = (peakCount / duration / 3.0).coerceIn(0.0, 1.0) * 22.0
        val spectral = (spectralEnergy / 40.0).coerceIn(0.0, 1.0) * 10.0
        return (movement + rotation + peakRate + spectral).coerceIn(0.0, 100.0)
    }

    /** Z-score robusto a variância zero. */
    fun zScore(value: Double, mean: Double, std: Double): Double {
        val safeStd = if (std < 1e-6) 1.0 else std
        return ((value - mean) / safeStd).coerceIn(-6.0, 6.0)
    }

    /** Média móvel simples sobre os últimos n valores. */
    fun meanOf(values: List<Double>, lastN: Int = values.size): Double {
        if (values.isEmpty()) return 0.0
        val from = (values.size - lastN).coerceAtLeast(0)
        var sum = 0.0
        var count = 0
        for (i in from until values.size) {
            sum += values[i]
            count++
        }
        return if (count == 0) 0.0 else sum / count
    }

    fun stdOf(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        var acc = 0.0
        for (v in values) acc += (v - mean) * (v - mean)
        return sqrt(acc / (values.size - 1))
    }

    /** Razão sinal/ruído simples usada para decidir se um pico é impulsivo. */
    fun isImpulsive(currentDb: Double, previousDb: Double, thresholdDb: Double = 12.0): Boolean =
        abs(currentDb - previousDb) >= thresholdDb && currentDb > previousDb
}
