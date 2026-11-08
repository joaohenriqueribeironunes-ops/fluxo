package com.equilibrio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equilibrio.app.ui.MainViewModel
import com.equilibrio.app.ui.components.LabeledText
import com.equilibrio.app.ui.components.ScreenScaffold
import com.equilibrio.core.domain.time.TimeUtils
import com.equilibrio.core.model.PredictedOutcome
import com.equilibrio.core.model.PredictionRecord
import com.equilibrio.core.ui.components.MetricTile
import com.equilibrio.core.ui.glass.GlassButton
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassTone

@Composable
fun AccuracyScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val accuracy by viewModel.accuracy.collectAsStateWithLifecycle()
    val predictions by viewModel.predictions.collectAsStateWithLifecycle()

    val report = accuracy
    val pending = predictions.filter { it.outcome == PredictedOutcome.PENDENTE }

    ScreenScaffold(
        title = "Precisão",
        subtitle = "Números honestos, inclusive quando o app erra",
        onBack = onBack
    ) {
        if (report == null || !report.hasData) {
            GlassCard(tone = GlassTone.Calm) {
                Text(
                    text = "Ainda não há dados para avaliar",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "As métricas consideram as previsões de 30–120 min e os desfechos que você informa. " +
                        "Elas ficam mais confiáveis com semanas de uso — e não escondem os falsos alarmes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    label = "Recall",
                    value = "${(report.recall * 100).toInt()}%",
                    helper = "crises detectadas",
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Precisão",
                    value = "${(report.precision * 100).toInt()}%",
                    helper = "alertas úteis",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    label = "Falsos alarmes",
                    value = report.falseAlarms.toString(),
                    helper = "no período",
                    modifier = Modifier.weight(1f),
                    tone = GlassTone.Warm
                )
                MetricTile(
                    label = "Antecedência",
                    value = "${report.meanLeadMinutes} min",
                    helper = "média",
                    modifier = Modifier.weight(1f)
                )
            }
            GlassCard {
                Text(
                    text = "Resumo do período",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                LabeledText("Crises registradas", report.totalCrises.toString())
                LabeledText("Previstas", report.predictedCrises.toString())
                LabeledText("Perdidas", report.missedCrises.toString())
                LabeledText("Brier (menor é melhor)", "%.2f".format(report.brierScore))
                LabeledText("Janelas avaliadas", report.evaluatedWindows.toString())
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Brier combina calibração e acerto: 0 é perfeito; 0,25 é o que se obtém chutando 50% sempre. " +
                        "Recall alto com muitos falsos alarmes também cansa — por isso os dois aparecem juntos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        if (pending.isNotEmpty()) {
            Text(
                text = "Avaliar previsões recentes",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            pending.take(6).forEach { record ->
                PendingPredictionCard(
                    record = record,
                    onUseful = { viewModel.markPrediction(record, PredictedOutcome.ALERTA_UTIL) },
                    onFalse = { viewModel.markPrediction(record, PredictedOutcome.FALSO_ALARME) },
                    onNothing = { viewModel.markPrediction(record, PredictedOutcome.NADA) }
                )
            }
        }

        val resolved = predictions.filter { it.outcome != PredictedOutcome.PENDENTE }.take(8)
        if (resolved.isNotEmpty()) {
            Text(
                text = "Histórico recente",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            resolved.forEach { record ->
                GlassCard(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                    Row {
                        Text(
                            text = TimeUtils.formatHm(record.windowStart),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier = Modifier.width(48.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${record.probability.times(100).toInt()}% · ${record.crisisType?.label ?: "tipo incerto"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                outcomeLabel(record.outcome),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingPredictionCard(
    record: PredictionRecord,
    onUseful: () -> Unit,
    onFalse: () -> Unit,
    onNothing: () -> Unit
) {
    GlassCard(tone = GlassTone.Warm, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Text(
            text = "${record.probability.times(100).toInt()}% · ${record.crisisType?.label ?: "tipo incerto"}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "${TimeUtils.formatFull(record.windowStart)} · confiança ${record.confidence.label}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )
        if (record.factors.isNotEmpty()) {
            Text(
                text = record.factors.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton(
                text = "Foi útil",
                onClick = onUseful,
                modifier = Modifier.weight(1f)
            )
            GlassButton(
                text = "Falso alarme",
                onClick = onFalse,
                primary = false,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(6.dp))
        GlassButton(
            text = "Não aconteceu nada",
            onClick = onNothing,
            primary = false,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun outcomeLabel(outcome: PredictedOutcome): String = when (outcome) {
    PredictedOutcome.PENDENTE -> "aguardando desfecho"
    PredictedOutcome.CRISE -> "crise ocorreu"
    PredictedOutcome.QUASE_CRISE -> "quase-crise"
    PredictedOutcome.NADA -> "nada aconteceu"
    PredictedOutcome.ALERTA_UTIL -> "alerta útil"
    PredictedOutcome.FALSO_ALARME -> "falso alarme"
}
