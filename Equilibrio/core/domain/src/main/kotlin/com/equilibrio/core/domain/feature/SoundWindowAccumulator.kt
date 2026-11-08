package com.equilibrio.core.domain.feature

import com.equilibrio.core.domain.signal.Fft
import com.equilibrio.core.domain.signal.SignalStats
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

data class SoundFeatures(
    val dbMean: Float,
    val dbMax: Float,
    val dbVariability: Float,
    val impulsivePeaks: Int,
    val lowBandRatio: Float,
    val midBandRatio: Float,
    val highBandRatio: Float,
    val overloadIndex: Float
) {
    companion object {
        val EMPTY = SoundFeatures(0f, 0f, 0f, 0, 0f, 0f, 0f, 0f)
    }
}

/**
 * Processa PCM 16 bits em tempo real e descarta o áudio. Guarda somente
 * nível, variabilidade, impulsividade e distribuição de energia por banda.
 *
 * A conversão dBFS -> dB SPL usa um offset fixo de calibração (94 dB). É uma
 * aproximação; o que importa para o modelo é a variação relativa ao baseline.
 */
class SoundWindowAccumulator(
    private val sampleRateHz: Int = 16_000,
    private val frameSize: Int = 1024,
    private val calibrationOffsetDb: Double = 94.0,
    private val fftEveryNFrames: Int = 2
) {
    private val frame = ShortArray(frameSize)
    private var filled = 0
    private var frameIndex = 0

    private var dbSumPower = 0.0
    private var dbCount = 0
    private var dbMax = 0.0
    private val dbValues = ArrayList<Double>(256)

    private var lastDb = Double.NaN
    private var lastImpulseFrame = -10
    private var impulsivePeaks = 0

    private var lowEnergy = 0.0
    private var midEnergy = 0.0
    private var highEnergy = 0.0

    private var totalSamples = 0

    fun feed(buffer: ShortArray, length: Int) {
        var i = 0
        while (i < length) {
            val toCopy = minOf(frameSize - filled, length - i)
            System.arraycopy(buffer, i, frame, filled, toCopy)
            filled += toCopy
            i += toCopy
            if (filled == frameSize) {
                processFrame()
                filled = 0
            }
        }
    }

    fun feed(sample: Short) {
        frame[filled++] = sample
        if (filled == frameSize) {
            processFrame()
            filled = 0
        }
    }

    private fun processFrame() {
        totalSamples += frameSize
        var sumSq = 0.0
        for (s in frame) {
            val v = s.toDouble()
            sumSq += v * v
        }
        val rms = sqrt(sumSq / frameSize)
        val dbfs = if (rms < 1e-9) -90.0 else 20.0 * log10(rms / 32768.0)
        val db = (dbfs + calibrationOffsetDb).coerceIn(0.0, 130.0)

        dbSumPower += 10.0.pow(db / 10.0)
        dbCount++
        dbValues.add(db)
        if (db > dbMax) dbMax = db

        if (!lastDb.isNaN() && frameIndex - lastImpulseFrame > 3 &&
            SignalStats.isImpulsive(db, lastDb, 12.0) && db > 55.0
        ) {
            impulsivePeaks++
            lastImpulseFrame = frameIndex
        }
        lastDb = db

        if (frameIndex % fftEveryNFrames == 0) {
            val samples = DoubleArray(frameSize) { frame[it].toDouble() }
            val mean = samples.average()
            for (k in samples.indices) samples[k] -= mean
            val mags = Fft.hannMagnitudes(samples)
            val binHz = sampleRateHz.toDouble() / frameSize
            for (bin in mags.indices) {
                val f = bin * binHz
                val p = mags[bin] * mags[bin]
                when {
                    f < 250.0 -> lowEnergy += p
                    f < 2000.0 -> midEnergy += p
                    else -> highEnergy += p
                }
            }
        }
        frameIndex++
    }

    fun hasData(): Boolean = dbCount > 0

    fun finish(): SoundFeatures {
        if (dbCount == 0) return SoundFeatures.EMPTY

        val meanPower = dbSumPower / dbCount
        val dbMean = 10.0 * log10(meanPower.coerceAtLeast(1e-9))
        val variance = dbValues.sumOf { (it - dbMean) * (it - dbMean) } / dbValues.size
        val variability = sqrt(variance)

        val total = lowEnergy + midEnergy + highEnergy
        val lowRatio = if (total < 1e-12) 0.0 else lowEnergy / total
        val midRatio = if (total < 1e-12) 0.0 else midEnergy / total
        val highRatio = if (total < 1e-12) 0.0 else highEnergy / total

        val levelComponent = ((dbMean - 45.0) / 40.0).coerceIn(0.0, 1.0) * 42.0
        val peakComponent = (impulsivePeaks / 8.0).coerceIn(0.0, 1.0) * 24.0
        val highComponent = highRatio.coerceIn(0.0, 1.0) * 18.0
        val variabilityComponent = (variability / 10.0).coerceIn(0.0, 1.0) * 10.0
        val midComponent = midRatio.coerceIn(0.0, 1.0) * 6.0
        val overload = (levelComponent + peakComponent + highComponent +
            variabilityComponent + midComponent).coerceIn(0.0, 100.0)

        return SoundFeatures(
            dbMean = dbMean.toFloat(),
            dbMax = dbMax.toFloat(),
            dbVariability = variability.toFloat(),
            impulsivePeaks = impulsivePeaks,
            lowBandRatio = lowRatio.toFloat(),
            midBandRatio = midRatio.toFloat(),
            highBandRatio = highRatio.toFloat(),
            overloadIndex = overload.toFloat()
        )
    }

    fun sampleCount(): Int = totalSamples
}
