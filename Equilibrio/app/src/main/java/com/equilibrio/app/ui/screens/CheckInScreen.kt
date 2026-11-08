package com.equilibrio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equilibrio.app.ui.MainViewModel
import com.equilibrio.app.ui.components.ChipGroup
import com.equilibrio.app.ui.components.GlassSliderRow
import com.equilibrio.app.ui.components.ScreenScaffold
import com.equilibrio.app.ui.components.SingleChoiceChips
import com.equilibrio.core.model.CheckIn
import com.equilibrio.core.model.CrisisType
import com.equilibrio.core.model.Emotion
import com.equilibrio.core.ui.glass.GlassButton
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassTone
import java.util.Locale

private val consumedOptions = listOf(
    "Refeição", "Lanche", "Café", "Água", "Doce", "Nada"
)

private val activityOptions = listOf(
    "Trabalho", "Estudo", "Casa", "Social", "Deslocamento", "Espera", "Exercício", "Tela"
)

@Composable
fun CheckInScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var emotions by rememberSaveable { mutableStateOf(setOf<String>()) }
    var intensity by rememberSaveable { mutableFloatStateOf(5f) }
    var sleepHours by rememberSaveable { mutableFloatStateOf(0f) }
    var hunger by rememberSaveable { mutableFloatStateOf(0f) }
    var pain by rememberSaveable { mutableFloatStateOf(0f) }
    var tension by rememberSaveable { mutableFloatStateOf(0f) }
    var sensory by rememberSaveable { mutableFloatStateOf(0f) }
    var consumed by rememberSaveable { mutableStateOf(setOf<String>()) }
    var activities by rememberSaveable { mutableStateOf(setOf<String>()) }
    var caffeine by rememberSaveable { mutableStateOf(false) }
    var transitions by rememberSaveable { mutableIntStateOf(0) }
    var thoughts by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var crisisChoice by rememberSaveable { mutableStateOf("Não") }

    fun buildCheckIn(quick: Boolean): CheckIn {
        val emotionList = Emotion.entries.filter { it.label in emotions }
        return CheckIn(
            timestamp = System.currentTimeMillis(),
            emotions = emotionList,
            emotionIntensity = intensity.toInt().coerceIn(1, 10),
            hunger = hunger.toInt(),
            sleepHours = sleepHours,
            pain = pain.toInt(),
            tension = tension.toInt(),
            sensorySensitivity = sensory.toInt(),
            consumed = (consumed - "Nada").toList(),
            caffeine = caffeine || "Café" in consumed,
            activities = activities.toList(),
            abruptTransitions = transitions,
            thoughts = thoughts.trim(),
            note = note.trim(),
            quick = quick
        )
    }

    fun save(quick: Boolean, close: Boolean = true) {
        val checkIn = buildCheckIn(quick)
        val crisis = if (crisisChoice == "Não") null
        else CrisisType.entries.firstOrNull { it.label == crisisChoice }
        viewModel.saveCheckIn(checkIn, alsoCrisis = crisis)
        if (close) onSaved()
    }

    ScreenScaffold(
        title = "Check-in",
        subtitle = "Passo ${step + 1} de 3 · sem cobrança, sem julgamento",
        onBack = onBack
    ) {
        if (step == 0) {
            GlassCard(tone = GlassTone.Calm) {
                ChipGroup(
                    title = "Como você está agora?",
                    description = "Marque o que combina. Um toque já basta.",
                    options = Emotion.entries.map { it.label },
                    selected = emotions,
                    onToggle = { label ->
                        emotions = if (label in emotions) emotions - label else emotions + label
                    }
                )
                Spacer(Modifier.height(12.dp))
                GlassSliderRow(
                    title = "Intensidade emocional",
                    valueLabel = "${intensity.toInt()}/10",
                    value = intensity,
                    onValueChange = { intensity = it },
                    valueRange = 1f..10f,
                    steps = 8
                )
            }
            GlassButton(
                text = "Salvar check-in rápido",
                onClick = { save(quick = true) },
                enabled = emotions.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            )
            GlassButton(
                text = "Continuar",
                onClick = { step = 1 },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (step == 1) {
            GlassCard {
                GlassSliderRow(
                    title = "Horas de sono",
                    valueLabel = if (sleepHours <= 0f) "não sei" else String.format(Locale("pt", "BR"), "%.1f h", sleepHours),
                    value = sleepHours,
                    onValueChange = { sleepHours = it },
                    valueRange = 0f..12f,
                    steps = 11,
                    description = "Sono curto costuma anteceder crises."
                )
                GlassSliderRow(
                    title = "Fome",
                    valueLabel = "${hunger.toInt()}/10",
                    value = hunger,
                    onValueChange = { hunger = it },
                    valueRange = 0f..10f,
                    steps = 9
                )
                GlassSliderRow(
                    title = "Dor física",
                    valueLabel = "${pain.toInt()}/10",
                    value = pain,
                    onValueChange = { pain = it },
                    valueRange = 0f..10f,
                    steps = 9
                )
                GlassSliderRow(
                    title = "Tensão no corpo",
                    valueLabel = "${tension.toInt()}/10",
                    value = tension,
                    onValueChange = { tension = it },
                    valueRange = 0f..10f,
                    steps = 9
                )
                GlassSliderRow(
                    title = "Sensibilidade sensorial",
                    valueLabel = "${sensory.toInt()}/10",
                    value = sensory,
                    onValueChange = { sensory = it },
                    valueRange = 0f..10f,
                    steps = 9
                )
            }
            GlassCard {
                ChipGroup(
                    title = "O que você consumiu?",
                    description = "Só para procurar padrões. Nada é compartilhado sem seu consentimento.",
                    options = consumedOptions,
                    selected = consumed,
                    onToggle = { item ->
                        consumed = if (item in consumed) consumed - item else consumed + item
                    }
                )
                Spacer(Modifier.height(10.dp))
                GlassChipRowToggle(
                    text = "Cafeína",
                    selected = caffeine,
                    onClick = { caffeine = !caffeine }
                )
            }
            GlassButton(
                text = "Continuar",
                onClick = { step = 2 },
                modifier = Modifier.fillMaxWidth()
            )
            GlassButton(
                text = "Salvar só o essencial",
                onClick = { save(quick = true) },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (step == 2) {
            GlassCard {
                ChipGroup(
                    title = "O que estava acontecendo?",
                    options = activityOptions,
                    selected = activities,
                    onToggle = { item ->
                        activities = if (item in activities) activities - item else activities + item
                    }
                )
                Spacer(Modifier.height(12.dp))
                GlassSliderRow(
                    title = "Transições bruscas",
                    valueLabel = transitions.toString(),
                    value = transitions.toFloat(),
                    onValueChange = { transitions = it.toInt() },
                    valueRange = 0f..4f,
                    steps = 3,
                    description = "Mudanças de ambiente, tarefa ou pessoa nas últimas horas."
                )
            }
            GlassCard {
                OutlinedTextField(
                    value = thoughts,
                    onValueChange = { thoughts = it },
                    label = { Text("Pensamento que mais apareceu (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Outra observação (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
            SingleChoiceChips(
                title = "Isso foi uma crise?",
                options = listOf("Não") + CrisisType.entries.map { it.label },
                selected = crisisChoice,
                onSelect = { crisisChoice = it }
            )
            GlassButton(
                text = if (crisisChoice == "Não") "Salvar check-in" else "Salvar check-in e crise",
                onClick = { save(quick = false) },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = "Voltar",
                    onClick = { step = 1 },
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "Ver o dia",
                    onClick = { save(quick = false) },
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (settings.checkInReminders) {
            Text(
                text = "Lembretes suaves entre ${settings.reminderStartHour}h e ${settings.reminderEndHour}h. " +
                    "Você pode desligá-los nas configurações.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun GlassChipRowToggle(text: String, selected: Boolean, onClick: () -> Unit) {
    com.equilibrio.core.ui.glass.GlassChip(
        text = text,
        selected = selected,
        onClick = onClick
    )
}
