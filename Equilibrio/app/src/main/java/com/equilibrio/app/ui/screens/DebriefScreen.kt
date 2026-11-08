package com.equilibrio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.equilibrio.app.ui.components.GlassSliderRow
import com.equilibrio.app.ui.components.ScreenScaffold
import com.equilibrio.core.domain.time.TimeUtils
import com.equilibrio.core.ui.components.EmptyState
import com.equilibrio.core.ui.glass.GlassButton
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassTone

private val debriefTriggers = listOf(
    "Ruído", "Muita gente", "Mudança de plano", "Tela", "Fome", "Sono curto", "Dor", "Cobrança", "Trânsito"
)

private val debriefHelped = listOf(
    "Silêncio", "Fones", "Stimming", "Respiração", "Escuro", "Água", "Abraço", "Sair do lugar", "Remédio"
)

@Composable
fun DebriefScreen(
    viewModel: MainViewModel,
    crisisId: Long,
    onBack: () -> Unit
) {
    val crises by viewModel.crises.collectAsStateWithLifecycle()
    val crisis = crises.firstOrNull { it.id == crisisId }

    var triggers by remember { mutableStateOf(setOf<String>()) }
    var helped by remember { mutableStateOf(setOf<String>()) }
    var recovery by remember { mutableFloatStateOf(30f) }
    var notes by remember { mutableStateOf("") }

    ScreenScaffold(
        title = "Debrief",
        subtitle = "Sem cobrança: é para aprender, não para julgar",
        onBack = onBack
    ) {
        if (crisis == null) {
            EmptyState(
                icon = Icons.Outlined.Spa,
                title = "Registro não encontrado",
                body = "A crise pode ter sido removida junto com os dados. Volte para a lista de crises."
            )
            GlassButton(text = "Voltar", onClick = onBack, modifier = Modifier.fillMaxWidth())
        } else {
            GlassCard(tone = GlassTone.Calm) {
                Text(
                    text = crisis.type.label,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${TimeUtils.formatFull(crisis.startMillis)} · intensidade ${crisis.intensity}/10",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                if (crisis.triggers.isNotEmpty()) {
                    Text(
                        text = "Gatilhos já registrados: ${crisis.triggers.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            GlassCard {
                ChipGroup(
                    title = "O que pareceu gatilho?",
                    description = "Marque só o que fizer sentido.",
                    options = debriefTriggers,
                    selected = triggers,
                    onToggle = { item ->
                        triggers = if (item in triggers) triggers - item else triggers + item
                    }
                )
                Spacer(Modifier.height(10.dp))
                ChipGroup(
                    title = "O que ajudou a passar?",
                    options = debriefHelped,
                    selected = helped,
                    onToggle = { item ->
                        helped = if (item in helped) helped - item else helped + item
                    }
                )
                Spacer(Modifier.height(10.dp))
                GlassSliderRow(
                    title = "Tempo aproximado de recuperação",
                    valueLabel = if (recovery <= 0f) "ainda não sei" else "${recovery.toInt()} min",
                    value = recovery,
                    onValueChange = { recovery = it },
                    valueRange = 0f..180f,
                    steps = 11
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Algo que você quer lembrar (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            GlassButton(
                text = "Salvar debrief",
                onClick = {
                    viewModel.finishCrisis(
                        crisisId = crisisId,
                        triggers = triggers.toList(),
                        helped = helped.toList(),
                        recoveryMinutes = recovery.toInt(),
                        notes = notes
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            )
            GlassCard {
                Text(
                    text = "Por que isso importa",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "O debrief alimenta o aprendizado: gatilhos, o que ajudou e tempo de recuperação " +
                        "ajudam o app a reconhecer padrões parecidos antes que se repitam. " +
                        "Nada é enviado ao Gemini sem o seu consentimento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
        }
    }
}
