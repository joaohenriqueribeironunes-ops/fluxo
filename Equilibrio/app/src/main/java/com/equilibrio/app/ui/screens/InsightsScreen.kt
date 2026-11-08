package com.equilibrio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equilibrio.app.ui.MainViewModel
import com.equilibrio.app.ui.components.LabeledText
import com.equilibrio.app.ui.components.ScreenScaffold
import com.equilibrio.core.model.AccuracyReport
import com.equilibrio.core.model.GeminiAnalysis
import com.equilibrio.core.ui.components.EmptyState
import com.equilibrio.core.ui.components.HeatmapGrid
import com.equilibrio.core.ui.components.HourlyHeatmapGrid
import com.equilibrio.core.ui.glass.GlassButton
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassTone

@Composable
fun InsightsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenAccuracy: () -> Unit,
    onOpenReport: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val analyses by viewModel.analyses.collectAsStateWithLifecycle()
    val insights by viewModel.insights.collectAsStateWithLifecycle()
    val accuracy by viewModel.accuracy.collectAsStateWithLifecycle()

    val latest = analyses.firstOrNull()

    ScreenScaffold(
        title = "Insights",
        subtitle = "Padrões locais + análise opcional do Gemini",
        onBack = onBack
    ) {
        if (latest != null) {
            AnalysisCard(latest)
        } else {
            GlassCard(tone = GlassTone.Calm) {
                EmptyState(
                    icon = Icons.Outlined.Lightbulb,
                    title = "Nenhuma análise ainda",
                    body = "Os insights locais aparecem conforme você registra check-ins e crises. " +
                        "A análise do Gemini é opcional e usa apenas resumos agregados."
                )
                if (settings.geminiEnabled) {
                    GlassButton(
                        text = if (settings.geminiConsent) "Pedir análise agora" else "Autorizar e pedir análise",
                        onClick = {
                            if (!settings.geminiConsent) {
                                viewModel.updateSettings { it.copy(geminiConsent = true) }
                            }
                            viewModel.requestGeminiNow()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (settings.geminiEnabled && latest != null) {
            GlassButton(
                text = "Pedir nova análise",
                onClick = { viewModel.requestGeminiNow() },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val bundle = insights
        if (bundle != null) {
            GlassCard {
                Text(
                    text = "Correlações dos últimos ${bundle.daysAnalyzed} dias",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${bundle.crisesAnalyzed} crises analisadas. Correlação não é causalidade — " +
                        "sirva para levantar hipóteses, não para concluir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
                Spacer(Modifier.height(10.dp))
                if (bundle.correlations.isEmpty()) {
                    Text(
                        text = "Ainda não há dias suficientes com e sem crise para calcular correlações.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    bundle.correlations.forEach { insight ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Text(
                                    text = insight.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "★".repeat(insight.strength),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = insight.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }

            val hasHourly = bundle.hourlyHeatmap.any { it.count > 0 }
            if (hasHourly || bundle.heatmap.any { it.count > 0 }) {
                GlassCard {
                    Text(
                        text = "Quando os eventos costumam acontecer",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    if (hasHourly) {
                        HourlyHeatmapGrid(cells = bundle.hourlyHeatmap)
                    } else {
                        HeatmapGrid(cells = bundle.heatmap)
                    }
                }
            }

            if (bundle.topTriggers.isNotEmpty() || bundle.topHelped.isNotEmpty()) {
                GlassCard {
                    if (bundle.topTriggers.isNotEmpty()) {
                        Text(
                            text = "Gatilhos mais citados",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        bundle.topTriggers.forEach { (label, count) ->
                            LabeledText(label = label.replaceFirstChar { it.uppercase() }, value = "$count×")
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    if (bundle.topHelped.isNotEmpty()) {
                        Text(
                            text = "O que mais ajudou",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        bundle.topHelped.forEach { (label, count) ->
                            LabeledText(label = label.replaceFirstChar { it.uppercase() }, value = "$count×")
                        }
                    }
                }
            }

            if (bundle.recurringPatterns.isNotEmpty()) {
                GlassCard(tone = GlassTone.Calm) {
                    Text(
                        text = "Padrões recorrentes",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    bundle.recurringPatterns.forEach { pattern ->
                        Text(
                            text = "• $pattern",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        val report = accuracy
        GlassCard {
            Text(
                text = "Precisão do app",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            if (report == null || !report.hasData) {
                Text(
                    text = "Ainda não há previsões avaliadas. As métricas aparecem quando houver crises e feedback suficientes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            } else {
                AccuracySummary(report)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = "Ver precisão",
                    onClick = onOpenAccuracy,
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Relatório",
                    onClick = onOpenReport,
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Text(
            text = "O Gemini nunca recebe áudio, amostras de sensores ou texto livre de check-in. " +
                "Ele recebe janelas agregadas, contagens e as previsões locais. " +
                "As análises são sugestões para observar, não diagnóstico.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun AnalysisCard(analysis: GeminiAnalysis) {
    GlassCard(tone = GlassTone.Neutral) {
        Text(
            text = "Análise assistida por IA",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = analysis.analysisText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Confiança informada ${(analysis.confidence * 100).toInt()}% · " +
                "${analysis.crisisCountUsed} crises e ${analysis.daysOfData} dias usados · ${analysis.model}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )

        SectionList("Gatilhos prováveis", analysis.triggers)
        SectionList("Padrões detectados", analysis.patterns)
        SectionList("Sugestões de regulação", analysis.suggestions)
        SectionList("Perguntas para observar", analysis.questions)
        SectionList("Limitações", analysis.warnings, muted = true)
    }
}

@Composable
private fun SectionList(title: String, items: List<String>, muted: Boolean = false) {
    if (items.isEmpty()) return
    Spacer(Modifier.height(10.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
    )
    items.forEach { item ->
        Text(
            text = "• $item",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (muted) 0.55f else 0.78f)
        )
    }
}

@Composable
private fun AccuracySummary(report: AccuracyReport) {
    LabeledText("Crises registradas", report.totalCrises.toString())
    LabeledText("Previstas com antecedência", report.predictedCrises.toString())
    LabeledText("Falsos alarmes", report.falseAlarms.toString())
    LabeledText("Recall", "${(report.recall * 100).toInt()}%")
    LabeledText("Precisão", "${(report.precision * 100).toInt()}%")
    LabeledText("Antecedência média", "${report.meanLeadMinutes} min")
    LabeledText("Brier (menor é melhor)", "%.2f".format(report.brierScore))
}
