package com.equilibrio.core.domain.signal

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * FFT radix-2 iterativa, sem dependências externas. Indicada para janelas
 * pequenas (256 a 2048 amostras) processadas no aparelho.
 */
object Fft {

    fun requirePowerOfTwo(n: Int) {
        require(n > 0 && (n and (n - 1)) == 0) { "FFT exige tamanho potência de 2, recebido $n" }
    }

    /** Magnitudes da metade útil do espectro (n/2 bins), sem normalização. */
    fun magnitudes(samples: DoubleArray): DoubleArray {
        val n = samples.size
        requirePowerOfTwo(n)
        val re = samples.copyOf()
        val im = DoubleArray(n)

        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j xor bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }

        var len = 2
        while (len <= n) {
            val angle = -2.0 * PI / len
            val wRe = cos(angle)
            val wIm = sin(angle)
            var i = 0
            while (i < n) {
                var curRe = 1.0
                var curIm = 0.0
                val half = len / 2
                for (k in 0 until half) {
                    val a = i + k
                    val b = a + half
                    val vRe = re[b] * curRe - im[b] * curIm
                    val vIm = re[b] * curIm + im[b] * curRe
                    val uRe = re[a]
                    val uIm = im[a]
                    re[a] = uRe + vRe
                    im[a] = uIm + vIm
                    re[b] = uRe - vRe
                    im[b] = uIm - vIm
                    val nextRe = curRe * wRe - curIm * wIm
                    curIm = curRe * wIm + curIm * wRe
                    curRe = nextRe
                }
                i += len
            }
            len = len shl 1
        }

        return DoubleArray(n / 2) { hypot(re[it], im[it]) }
    }

    /** Aplica janela de Hann e retorna magnitudes. */
    fun hannMagnitudes(samples: DoubleArray): DoubleArray {
        val n = samples.size
        requirePowerOfTwo(n)
        val windowed = DoubleArray(n) { i ->
            val w = 0.5 * (1.0 - cos(2.0 * PI * i / (n - 1)))
            samples[i] * w
        }
        return magnitudes(windowed)
    }

    /** Frequência do bin de maior magnitude. */
    fun dominantFrequencyHz(samples: DoubleArray, sampleRateHz: Double): Double {
        val mags = hannMagnitudes(samples)
        if (mags.isEmpty()) return 0.0
        var best = 0
        for (i in 1 until mags.size) if (mags[i] > mags[best]) best = i
        return best * sampleRateHz / samples.size
    }

    /** Energia total (soma dos quadrados das magnitudes) normalizada pelo tamanho. */
    fun spectralEnergy(samples: DoubleArray): Double {
        val mags = hannMagnitudes(samples)
        var sum = 0.0
        for (m in mags) sum += m * m
        return sum / samples.size.coerceAtLeast(1)
    }

    /** Energia de uma faixa de frequência, em fração de 0..1 do total. */
    fun bandRatio(
        magnitudes: DoubleArray,
        sampleRateHz: Double,
        lowHz: Double,
        highHz: Double
    ): Double {
        if (magnitudes.isEmpty()) return 0.0
        val binHz = sampleRateHz / (magnitudes.size * 2)
        var band = 0.0
        var total = 0.0
        for (i in magnitudes.indices) {
            val f = i * binHz
            val p = magnitudes[i] * magnitudes[i]
            total += p
            if (f >= lowHz && f < highHz) band += p
        }
        return if (total <= 1e-12) 0.0 else (band / total).coerceIn(0.0, 1.0)
    }
}
