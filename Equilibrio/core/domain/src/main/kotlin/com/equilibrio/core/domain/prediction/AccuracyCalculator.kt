package com.equilibrio.core.domain.prediction

import com.equilibrio.core.model.AccuracyReport
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.PredictedOutcome
import com.equilibrio.core.model.PredictionRecord

/**
 * Métricas honestas de acerto. Não inflar: janelas previstas sem crise contam
 * como falso alarme quando o usuário marca assim explicitamente.
 */
object AccuracyCalculator {

    fun compute(
        predictions: List<PredictionRecord>,
        crises: List<CrisisEvent>,
        periodStart: Long,
        periodEnd: Long,
        probabilityThreshold: Float = 0.5f
    ): AccuracyReport {
        val periodPredictions = predictions.filter {
            it.createdAt in periodStart..periodEnd
        }
        val periodCrises = crises.filter {
            it.startMillis in periodStart..periodEnd && it.type.isActualCrisis
        }

        var predicted = 0
        val leads = mutableListOf<Long>()
        for (crisis in periodCrises) {
            val match = periodPredictions
                .filter {
                    it.probability >= probabilityThreshold &&
                        it.createdAt <= crisis.startMillis &&
                        it.windowEnd >= crisis.startMillis &&
                        it.outcome != PredictedOutcome.FALSO_ALARME
                }
                .minByOrNull { it.createdAt }
            if (match != null) {
                predicted++
                leads += crisis.startMillis - match.createdAt
            }
        }

        val falseAlarms = periodPredictions.count { it.outcome == PredictedOutcome.FALSO_ALARME }
        val brierSamples = periodPredictions.mapNotNull { record ->
            when (record.outcome) {
                PredictedOutcome.CRISE, PredictedOutcome.QUASE_CRISE ->
                    record.probability.toDouble() to true
                PredictedOutcome.NADA ->
                    record.probability.toDouble() to false
                else -> null
            }
        }
        val brier = PlattCalibration.brierScore(brierSamples)
        val precision = if (predicted + falseAlarms > 0) {
            predicted.toDouble() / (predicted + falseAlarms)
        } else {
            0.0
        }
        val recall = if (periodCrises.isNotEmpty()) {
            predicted.toDouble() / periodCrises.size
        } else {
            0.0
        }

        return AccuracyReport(
            periodStart = periodStart,
            periodEnd = periodEnd,
            totalCrises = periodCrises.size,
            predictedCrises = predicted,
            missedCrises = (periodCrises.size - predicted).coerceAtLeast(0),
            falseAlarms = falseAlarms,
            meanLeadMinutes = if (leads.isEmpty()) 0 else (leads.average() / 60_000.0).toInt(),
            brierScore = brier.toFloat(),
            precision = precision.toFloat(),
            recall = recall.toFloat(),
            evaluatedWindows = brierSamples.size + falseAlarms
        )
    }
}
