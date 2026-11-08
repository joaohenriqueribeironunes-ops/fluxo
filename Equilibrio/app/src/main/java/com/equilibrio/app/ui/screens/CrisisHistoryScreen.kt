package com.equilibrio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equilibrio.app.ui.MainViewModel
import com.equilibrio.app.ui.components.ChipGroup
import com.equilibrio.app.ui.components.ScreenScaffold
import com.equilibrio.app.ui.components.SingleChoiceChips
import com.equilibrio.core.domain.time.TimeUtils
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.CrisisType
import com.equilibrio.core.ui.components.EmptyState
import com.equilibrio.core.ui.glass.GlassButton
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassChip
import com.equilibrio.core.ui.glass.GlassTone

private val triggerOptions = listOf(
    "Ruído", "Muita gente", "Mudança de plano", "Tela", "Fome", "Sono curto", "Dor", "Cobrança", "Trânsito"
)

private val helpedOptions = listOf(
    "Silêncio", "Fones", "Stimming", "Respiração", "Escuro", "Água", "Abraço", "Sair do lugar", "Remédio"
)

@Composable
fun CrisisHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenDebrief: (Long) -> Unit
) {
    val crises by viewModel.crises.collectAsStateWithLifecycle()
    var showRetro by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Crises",
        subtitle = "${crises.size} registro(s) · aprender também faz parte",
        onBack = onBack
    ) {
        GlassButton(
            text = "Registrar crise passada",
            onClick = { showRetro = true },
            modifier = Modifier.fillMaxWidth()
        )

        if (crises.isEmpty()) {
            GlassCard(tone = GlassTone.Calm) {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = "Nenhuma crise registrada",
                    body = "Se acontecer, você pode registrar na hora ou depois, com o tempo que precisar. " +
                        "Nenhum registro é obrigatório."
                )
            }
        } else {
            crises.forEach { crisis ->
                CrisisHistoryCard(
                    crisis = crisis,
                    onDebrief = { onOpenDebrief(crisis.id) }
                )
            }
        }

        Text(
            text = "O registro retroativo também vale: o que importa é a informação, não a rapidez. " +
                "Evite anotar detalhes íntimos se não quiser; eles não são necessários para o padrão.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
    }

    if (showRetro) {
        RetroactiveDialog(
            onDismiss = { showRetro = false },
            onConfirm = { type, intensity, hoursAgo, triggers, helped, notes ->
                viewModel.registerCrisis(
                    type = type,
                    intensity = intensity,
                    triggers = triggers,
                    helped = helped,
                    startMillis = System.currentTimeMillis() - (hoursAgo * 3_600_000L).toLong(),
                    retroactive = true,
                    notes = notes
                )
                showRetro = false
            }
        )
    }
}

@Composable
private fun CrisisHistoryCard(crisis: CrisisEvent, onDebrief: () -> Unit) {
    GlassCard(
        tone = if (crisis.type == CrisisType.QUASE_CRISE) GlassTone.Calm else GlassTone.Alert,
        padding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = crisis.type.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = TimeUtils.formatFull(crisis.startMillis) +
                        if (crisis.retroactive) " · retroativo" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                )
            }
            Text(
                text = "${crisis.intensity}/10",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (crisis.triggers.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Gatilhos: ${crisis.triggers.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        if (crisis.helped.isNotEmpty()) {
            Text(
                text = "Ajudou: ${crisis.helped.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        if (crisis.recoveryMinutes > 0) {
            Text(
                text = "Recuperação: ${crisis.recoveryMinutes} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.height(10.dp))
        if (crisis.debriefDone) {
            Text(
                text = "Debrief concluído. Obrigado por transformar isso em aprendizado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        } else {
            GlassButton(
                text = "Fazer debrief",
                onClick = onDebrief,
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RetroactiveDialog(
    onDismiss: () -> Unit,
    onConfirm: (CrisisType, Int, Int, List<String>, List<String>, String) -> Unit
) {
    var type by remember { mutableStateOf(CrisisType.SOBRECARGA_SENSORIAL) }
    var intensity by remember { mutableFloatStateOf(6f) }
    var whenLabel by remember { mutableStateOf("1 h atrás") }
    var triggers by remember { mutableStateOf(setOf<String>()) }
    var helped by remember { mutableStateOf(setOf<String>()) }
    var notes by remember { mutableStateOf("") }

    val whenOptions = listOf("1 h atrás", "3 h atrás", "6 h atrás", "12 h atrás", "1 dia atrás")
    val hours = when (whenLabel) {
        "1 h atrás" -> 1
        "3 h atrás" -> 3
        "6 h atrás" -> 6
        "12 h atrás" -> 12
        else -> 24
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar crise passada") },
        text = {
            Column(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tipo", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CrisisType.entries.forEach { option ->
                        GlassChip(
                            text = option.label,
                            selected = option == type,
                            onClick = { type = option }
                        )
                    }
                }
                Text("Quando foi? ${if (whenLabel.isEmpty()) "" else whenLabel}", style = MaterialTheme.typography.labelLarge)
                SingleChoiceChips(
                    title = "Quando",
                    options = whenOptions,
                    selected = whenLabel,
                    onSelect = { whenLabel = it }
                )
                Text("Intensidade: ${intensity.toInt()}/10", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
                ChipGroup(
                    title = "Gatilhos",
                    options = triggerOptions,
                    selected = triggers,
                    onToggle = { item ->
                        triggers = if (item in triggers) triggers - item else triggers + item
                    }
                )
                ChipGroup(
                    title = "O que ajudou",
                    options = helpedOptions,
                    selected = helped,
                    onToggle = { item ->
                        helped = if (item in helped) helped - item else helped + item
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(type, intensity.toInt(), hours, triggers.toList(), helped.toList(), notes)
                }
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
