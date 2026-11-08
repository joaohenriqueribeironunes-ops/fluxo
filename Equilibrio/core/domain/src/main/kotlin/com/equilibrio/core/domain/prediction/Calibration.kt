package com.equilibrio.core.domain.prediction

import kotlin.math.exp
import kotlin.math.ln

/**
 * Calibração de Platt: transforma o score bruto em probabilidade calibrada,
 * para que "70%" signifique aproximadamente 70% na prática.
 */
data class PlattModel(
    val a: Double = 1.0,
    val b: Double = 0.0,
    val samples: Int = 0
) {
    fun calibrate(probability: Double): Double {
        val p = probability.coerceIn(0.001, 0.999)
        val logit = ln(p / (1.0 - p))
        val z = a * logit + b
        return 1.0 / (1.0 + exp(-z))
    }
}

object PlattCalibration {

    /**
     * Ajuste por descida de gradiente com regularização L2 leve.
     * samples: probabilidade bruta e resultado observado (1 = crise).
     */
    fun fit(
        samples: List<Pair<Double, Boolean>>,
        iterations: Int = 800,
        learningRate: Double = 0.08,
        l2: Double = 1e-4
    ): PlattModel {
        if (samples.size < 5) return PlattModel()
        var a = 1.0
        var b = 0.0
        val n = samples.size
        repeat(iterations) {
            var gradA = 0.0
            var gradB = 0.0
            for ((pRaw, label) in samples) {
                val p = pRaw.coerceIn(0.001, 0.999)
                val x = ln(p / (1.0 - p))
                val y = if (label) 1.0 else 0.0
                val pred = 1.0 / (1.0 + exp(-(a * x + b)))
                val error = pred - y
                gradA += error * x
                gradB += error
            }
            a -= learningRate * (gradA / n + l2 * a)
            b -= learningRate * (gradB / n)
            a = a.coerceIn(0.05, 5.0)
            b = b.coerceIn(-3.0, 3.0)
        }
        return PlattModel(a, b, n)
    }

    fun brierScore(samples: List<Pair<Double, Boolean>>): Double {
        if (samples.isEmpty()) return 0.0
        var sum = 0.0
        for ((p, label) in samples) {
            val y = if (label) 1.0 else 0.0
            sum += (p - y) * (p - y)
        }
        return sum / samples.size
    }
}
