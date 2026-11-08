package com.equilibrio.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equilibrio.app.ui.MainViewModel
import com.equilibrio.app.ui.components.FactorRow
import com.equilibrio.app.ui.components.ForecastCard
import com.equilibrio.app.ui.components.ScreenScaffold
import com.equilibrio.core.model.CrisisType
import com.equilibrio.core.model.LocalRisk
import com.equilibrio.core.model.PredictedOutcome
import com.equilibrio.core.model.RiskLevel
import com.equilibrio.core.model.RiskForecast
import com.equilibrio.core.ui.components.MicIndicator
import com.equilibrio.core.ui.components.PrivacyDisclaimer
import com.equilibrio.core.ui.components.RiskBadge
import com.equilibrio.core.ui.components.RiskGauge
import com.equilibrio.core.ui.glass.GlassButton
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassChip
import com.equilibrio.core.ui.glass.GlassTone
import com.equilibrio.core.ui.theme.CalmTeal
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenCheckIn: () -> Unit,
    onOpenForecast: () -> Unit,
    onOpenCalm: () -> Unit,
    onOpenInsights: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val risk by viewModel.risk.collectAsStateWithLifecycle()
    val collection by viewModel.collection.collectAsStateWithLifecycle()
    val predictions by viewModel.predictions.collectAsStateWithLifecycle()
    val shortTerm by viewModel.shortTerm.collectAsStateWithLifecycle()
    val todayForecasts by viewModel.todayForecasts.collectAsStateWithLifecycle()

    var showCrisisDialog by remember { mutableStateOf(false) }

    val local = risk
    val ready = local?.baselineReady == true
    val score = local?.score ?: 0
    val level = local?.level ?: RiskLevel.BAIXO
    val now = System.currentTimeMillis()
    val pendingPrediction = predictions.firstOrNull {
        it.outcome == PredictedOutcome.PENDENTE && it.createdAt > now - 6 * 3_600_000L
    }
    val nextForecast: RiskForecast? = shortTerm.firstOrNull() ?: todayForecasts.firstOrNull()

    ScreenScaffold(
        title = "Equilíbrio",
        subtitle = "Um passo de cada vez, no seu ritmo.",
        actions = {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "Configurações",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenSettings)
                    .padding(9.dp)
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RiskBadge(level = level, score = if (ready) score else null)
            if (collection.running) {
                MicIndicator(active = collection.micActive)
            } else {
                Text(
                    text = "Coleta pausada",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
            }
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            RiskGauge(
                score = score,
                level = level,
                baselineReady = ready,
                learningLabel = if (local == null) "iniciando observação" else "aprendendo seus padrões"
            )
        }

        if (!ready) {
            val progress = (local?.baselineProgress ?: settings.baselineProgress).coerceIn(0f, 1f)
            GlassCard(tone = GlassTone.Calm) {
                Text(
                    text = "Baseline pessoal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress.coerceAtLeast(0.02f))
                            .height(9.dp)
                            .clip(CircleShape)
                            .background(CalmTeal)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${(progress * 100).toInt()}% · dia ${local?.baselineDays ?: 0} de ${settings.baselineDays}. " +
                        "Nenhum alerta é disparado antes de o baseline ficar pronto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        } else if (local != null && local.factors.isNotEmpty()) {
            GlassCard {
                Text(
                    text = "O que está pesando agora",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                local.factors.take(4).forEach { FactorRow(it) }
            }
        }

        GlassCard(tone = GlassTone.Alert) {
            Text(
                text = "Se a crise já começou",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Registre para aprender com ela ou abra a pausa guiada. Você não precisa passar por isso sozinho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = "Registrar crise",
                    onClick = { showCrisisDialog = true },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Pausa guiada",
                    onClick = onOpenCalm,
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (pendingPrediction != null) {
            GlassCard(tone = GlassTone.Warm) {
                Text(
                    text = "O último aviso fez sentido?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Havia ${pendingPrediction.probability.times(100).toInt()}% de chance de sobrecarga em " +
                        "${formatWindow(pendingPrediction.windowStart, pendingPrediction.windowEnd)}. " +
                        "Seu retorno calibra o modelo local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(
                        text = "Foi útil",
                        onClick = { viewModel.markLatestFeedback(PredictedOutcome.ALERTA_UTIL) },
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Falso alarme",
                        onClick = { viewModel.markLatestFeedback(PredictedOutcome.FALSO_ALARME) },
                        primary = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (nextForecast != null) {
            ForecastCard(forecast = nextForecast, onClick = onOpenForecast)
        } else {
            GlassCard(tone = GlassTone.Neutral) {
                Text(
                    text = "Previsões em preparação",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Assim que houver baseline e algumas janelas, a previsão de 30–120 minutos aparece aqui.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }

        GlassButton(
            text = "Check-in rápido",
            onClick = onOpenCheckIn,
            primary = true,
            modifier = Modifier.fillMaxWidth()
        )

        GlassCard {
            Text(
                text = "Coleta passiva",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (settings.collectionEnabled) {
                    "Ativa · bateria ${collection.batteryPercent}%" +
                        if (collection.lowBattery) " · pausada por bateria baixa" else ""
                } else {
                    "Desativada · os sensores não estão sendo lidos"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = if (settings.collectionEnabled) "Desligar coleta" else "Ligar coleta",
                    onClick = { viewModel.toggleCollection(!settings.collectionEnabled) },
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Insights",
                    onClick = onOpenInsights,
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        PrivacyDisclaimer(compact = true)
    }

    if (showCrisisDialog) {
        CrisisDialog(
            onDismiss = { showCrisisDialog = false },
            onConfirm = { type, intensity ->
                showCrisisDialog = false
                viewModel.registerCrisis(type = type, intensity = intensity)
            }
        )
    }
}

@Composable
private fun CrisisDialog(
    onDismiss: () -> Unit,
    onConfirm: (CrisisType, Int) -> Unit
) {
    var selected by remember { mutableStateOf(CrisisType.SOBRECARGA_SENSORIAL) }
    var intensity by remember { mutableFloatStateOf(6f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar crise agora") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Qual tipo se parece mais com o que está acontecendo?",
                    style = MaterialTheme.typography.bodyMedium
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CrisisType.entries.forEach { type ->
                        GlassChip(
                            text = type.label,
                            selected = type == selected,
                            onClick = { selected = type }
                        )
                    }
                }
                Text(
                    "Intensidade: ${intensity.toInt()}/10",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
                Text(
                    "Sem julgamento. Registrar também é cuidar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected, intensity.toInt()) }) {
                Text("Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Agora não") }
        }
    )
}

private fun formatWindow(start: Long, end: Long): String {
    val fmt = DateTimeFormatter.ofPattern("HH:mm")
    val from = java.time.Instant.ofEpochMilli(start).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    val to = java.time.Instant.ofEpochMilli(end).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    return "${from.format(fmt)}–${to.format(fmt)}"
}

private fun greeting(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }
}
