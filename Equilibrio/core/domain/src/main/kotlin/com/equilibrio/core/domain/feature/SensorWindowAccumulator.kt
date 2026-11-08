package com.equilibrio.core.domain.feature

import com.equilibrio.core.model.SensorWindow
import com.equilibrio.core.domain.signal.Fft
import com.equilibrio.core.domain.signal.SignalStats
import kotlin.math.sqrt

/**
 * Acumula amostras de uma janela de coleta e produz apenas agregados.
 * Nenhuma amostra individual é persistida.
 */
class SensorWindowAccumulator(
    private val deviceRateHz: Double = 50.0,
    private val targetRateHz: Double = 25.0,
    private val fftSize: Int = 256
) {
    private val accel = ArrayList<Double>(512)
    private val gyro = ArrayList<Double>(512)
    private var lightSum = 0.0
    private var lightCount = 0
    private var proximityNearCount = 0
    private var proximityCount = 0

    private var lastAccelNanos = 0L
    private var lastGyroNanos = 0L
    private val minIntervalNanos = (1_000_000_000.0 / targetRateHz * 0.85).toLong()

    val accelSampleCount: Int get() = accel.size
    val gyroSampleCount: Int get() = gyro.size

    fun onAccelerometer(x: Float, y: Float, z: Float, timestampNanos: Long) {
        if (timestampNanos - lastAccelNanos < minIntervalNanos) return
        lastAccelNanos = timestampNanos
        accel.add(sqrt((x * x + y * y + z * z).toDouble()))
    }

    fun onGyroscope(x: Float, y: Float, z: Float, timestampNanos: Long) {
        if (timestampNanos - lastGyroNanos < minIntervalNanos) return
        lastGyroNanos = timestampNanos
        gyro.add(sqrt((x * x + y * y + z * z).toDouble()))
    }

    fun onLight(lux: Float) {
        if (lux.isFinite() && lux >= 0f) {
            lightSum += lux
            lightCount++
        }
    }

    fun onProximity(centimeters: Float) {
        if (centimeters.isFinite()) {
            proximityCount++
            if (centimeters < 5f) proximityNearCount++
        }
    }

    fun reset() {
        accel.clear()
        gyro.clear()
        lightSum = 0.0
        lightCount = 0
        proximityNearCount = 0
        proximityCount = 0
        lastAccelNanos = 0L
        lastGyroNanos = 0L
    }

    fun hasData(): Boolean = accel.isNotEmpty() || gyro.isNotEmpty()

    fun finish(
        startMillis: Long,
        endMillis: Long,
        batteryPercent: Int,
        riskScore: Int = 0
    ): SensorWindow {
        val accelStats = SignalStats.of(accel.toDoubleArray())
        val gyroStats = SignalStats.of(gyro.toDoubleArray())
        val durationSeconds = ((endMillis - startMillis).coerceAtLeast(1L) / 1000.0)

        val fftSlice = if (accel.size >= fftSize) {
            accel.takeLast(fftSize).toDoubleArray()
        } else {
            DoubleArray(0)
        }
        val spectralEnergy = if (fftSlice.isNotEmpty()) {
            val mean = fftSlice.average()
            val detrended = DoubleArray(fftSlice.size) { fftSlice[it] - mean }
            Fft.spectralEnergy(detrended)
        } else {
            0.0
        }
        val dominant = if (fftSlice.size == fftSize) {
            val mean = fftSlice.average()
            val detrended = DoubleArray(fftSlice.size) { fftSlice[it] - mean }
            Fft.dominantFrequencyHz(detrended, deviceRateHz)
        } else {
            0.0
        }

        val agitation = SignalStats.agitationIndex(
            accelStd = accelStats.stdDev,
            gyroStd = gyroStats.stdDev,
            peakCount = accelStats.peakCount + gyroStats.peakCount,
            durationSeconds = durationSeconds,
            spectralEnergy = spectralEnergy
        )

        return SensorWindow(
            startMillis = startMillis,
            endMillis = endMillis,
            accelMean = accelStats.mean.toFloat(),
            accelStd = accelStats.stdDev.toFloat(),
            accelRms = accelStats.rms.toFloat(),
            accelPeaks = accelStats.peakCount,
            gyroMean = gyroStats.mean.toFloat(),
            gyroStd = gyroStats.stdDev.toFloat(),
            gyroRms = gyroStats.rms.toFloat(),
            gyroPeaks = gyroStats.peakCount,
            dominantFrequencyHz = dominant.toFloat(),
            spectralEnergy = spectralEnergy.toFloat(),
            lightMeanLux = if (lightCount > 0) (lightSum / lightCount).toFloat() else 0f,
            proximityNear = proximityCount > 0 && proximityNearCount * 2 >= proximityCount,
            agitationIndex = agitation.toFloat(),
            batteryPercent = batteryPercent,
            riskScore = riskScore
        )
    }
}
