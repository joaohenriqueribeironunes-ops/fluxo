package com.equilibrio.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equilibrio.app.ui.MainViewModel
import com.equilibrio.app.ui.components.LabeledText
import com.equilibrio.app.ui.components.ScreenScaffold
import com.equilibrio.core.ui.components.MetricTile
import com.equilibrio.core.ui.glass.GlassButton
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassTone

@Composable
fun ReportScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val summary by viewModel.dailySummary.collectAsStateWithLifecycle()
    val accuracy by viewModel.accuracy.collectAsStateWithLifecycle()
    val lastExport by viewModel.lastExport.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = "Relatório",
        subtitle = "PDF e backup gerados no aparelho",
        onBack = onBack
    ) {
        val day = summary
        if (day != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    label = "Risco médio",
                    value = day.averageRisk.toString(),
                    helper = "máximo ${day.maxRisk}",
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Crises",
                    value = day.crisisCount.toString(),
                    helper = "${day.nearCrisisCount} quase-crise(s)",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    label = "Check-ins",
                    value = day.checkInCount.toString(),
                    helper = "no dia",
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Sono",
                    value = "%.1f h".format(day.averageSleepHours),
                    helper = "média registrada",
                    modifier = Modifier.weight(1f)
                )
            }

            GlassCard {
                Text(
                    text = "Resumo do dia",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                LabeledText("Sobrecarga sonora média", "${day.averageSoundOverload.toInt()}/100")
                LabeledText("Agitação média", "${day.averageAgitation.toInt()}/100")
                if (day.dominantEmotions.isNotEmpty()) {
                    LabeledText(
                        "Emoções mais registradas",
                        day.dominantEmotions.joinToString(", ") { it.label }
                    )
                }
                if (day.topTriggers.isNotEmpty()) {
                    LabeledText("Gatilhos citados", day.topTriggers.joinToString(", "))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "O relatório diário inclui a linha do tempo, a análise Gemini (se houver) e as métricas de precisão.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            GlassCard(tone = GlassTone.Calm) {
                Text(
                    text = "Sem dados para o dia",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Assim que houver janelas de sensores ou check-ins, o resumo aparece aqui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }

        GlassCard {
            Text(
                text = "Exportar",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "PDFs e JSON ficam em uma pasta privada do app. Compartilhar envia só o arquivo escolhido.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
            )
            Spacer(Modifier.height(10.dp))
            GlassButton(
                text = "Gerar PDF do dia",
                onClick = { viewModel.exportDaily() },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            GlassButton(
                text = "Gerar PDF da semana",
                onClick = { viewModel.exportWeekly() },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            GlassButton(
                text = "Exportar backup JSON",
                onClick = { viewModel.exportJson() },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val file = lastExport
        if (file != null) {
            GlassCard(tone = GlassTone.Calm) {
                Text(
                    text = "Último arquivo gerado",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
                Spacer(Modifier.height(10.dp))
                GlassButton(
                    text = "Compartilhar arquivo",
                    onClick = { shareFile(context, file) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        val report = accuracy
        if (report != null && report.hasData) {
            GlassCard {
                Text(
                    text = "Precisão no período",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                LabeledText("Recall", "${(report.recall * 100).toInt()}%")
                LabeledText("Precisão", "${(report.precision * 100).toInt()}%")
                LabeledText("Falsos alarmes", report.falseAlarms.toString())
                LabeledText("Antecedência média", "${report.meanLeadMinutes} min")
                LabeledText("Brier", "%.2f".format(report.brierScore))
            }
        }

        Text(
            text = "Compartilhe o PDF apenas com quem você confia. Ele contém dados de saúde e não substitui " +
                "avaliação profissional. Em risco à segurança: CVV 188 ou SAMU 192.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
    }
}

private fun shareFile(context: Context, file: java.io.File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val mime = when {
        file.name.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
        file.name.endsWith(".json", ignoreCase = true) -> "application/json"
        else -> "*/*"
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching {
        context.startActivity(Intent.createChooser(intent, "Compartilhar ${file.name}"))
    }
}
