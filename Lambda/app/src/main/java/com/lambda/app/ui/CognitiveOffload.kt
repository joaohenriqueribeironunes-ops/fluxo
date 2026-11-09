package com.lambda.app.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambda.coreui.design.GlassButton
import com.lambda.coreui.design.GlassCard
import com.lambda.coreui.design.GlassPanel
import com.lambda.coreui.design.GlassTextField
import com.lambda.coreui.design.SectionLabel
import com.lambda.databehavior.dao.BehaviorDao
import com.lambda.databehavior.entity.DestaqueCognitivo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OffloadBuckets(
    val tarefas: List<String>,
    val ideias: List<String>,
    val emocoes: List<String>,
    val duvidas: List<String>,
)

@HiltViewModel
class OffloadViewModel @Inject constructor(private val dao: BehaviorDao) : ViewModel() {
    fun saveHighlight(texto: String, cor: String, categoria: String) {
        if (texto.isBlank()) return
        viewModelScope.launch {
            dao.insertDestaque(
                DestaqueCognitivo(
                    texto = texto.trim(), cor = cor, categoria = categoria,
                    origem = "descarregamento", dataGeracao = System.currentTimeMillis(),
                )
            )
        }
    }
}

/** Organização local e determinística: nenhuma IA, nenhum envio de dados. */
fun organizeOffload(text: String): OffloadBuckets {
    val lines = text.lines().map { it.trim().trimStart('-', '*', '•', ' ') }.filter { it.length > 2 }
    val tarefas = mutableListOf<String>()
    val ideias = mutableListOf<String>()
    val emocoes = mutableListOf<String>()
    val duvidas = mutableListOf<String>()
    val emoKeys = listOf("sinto", "estou", "ansios", "triste", "feliz", "raiva", "medo", "cansad", "sobrecarr", "frustrad", "aliviad", "animad")
    val taskKeys = listOf("fazer", "preciso", "tenho que", "comprar", "enviar", "ligar", "pagar", "marcar", "estudar", "terminar", "arrumar", "lembra")
    val doubtKeys = listOf("?", "será", "como", "não sei", "dúvida", "duvida", "talvez", "não entendi")
    for (l in lines) {
        val low = l.lowercase()
        when {
            doubtKeys.any { low.contains(it) } -> duvidas += l
            emoKeys.any { low.contains(it) } -> emocoes += l
            taskKeys.any { low.contains(it) } -> tarefas += l
            else -> ideias += l
        }
    }
    return OffloadBuckets(tarefas, ideias, emocoes, duvidas)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CognitiveOffloadSection(vm: OffloadViewModel = hiltViewModel()) {
    var input by remember { mutableStateOf("") }
    var out by remember { mutableStateOf<OffloadBuckets?>(null) }
    var savedMsg by remember { mutableStateOf<String?>(null) }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            val matches = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim().orEmpty()
            if (spoken.isNotBlank()) input = if (input.isBlank()) spoken else "$input\n$spoken"
        }
    }

    GlassPanel(contentPadding = PaddingValues(18.dp)) {
        SectionLabel("Descarregamento cognitivo")
        Spacer(Modifier.height(6.dp))
        Text(
            "Fale ou digite livremente. O app separa em tarefas, ideias, emoções e dúvidas — sem exigir estrutura.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        )
        Spacer(Modifier.height(12.dp))
        GlassTextField(
            value = input,
            onValueChange = { input = it },
            label = "O que está na cabeça agora?",
            minLines = 4,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton(
                onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Pode falar livremente. Eu organizo depois.")
                    }
                    runCatching { voiceLauncher.launch(intent) }
                },
                accent = false,
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Falar")
            }
            GlassButton(onClick = { out = organizeOffload(input) }, enabled = input.isNotBlank()) {
                Text("Organizar", fontWeight = FontWeight.SemiBold)
            }
        }
        savedMsg?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }

    out?.let { b ->
        Spacer(Modifier.height(14.dp))
        HighlightBucket("Tarefas", b.tarefas, "agua", vm) { savedMsg = it }
        HighlightBucket("Ideias", b.ideias, "lavanda", vm) { savedMsg = it }
        HighlightBucket("Emoções", b.emocoes, "menta", vm) { savedMsg = it }
        HighlightBucket("Dúvidas", b.duvidas, "ambar", vm) { savedMsg = it }
        Spacer(Modifier.height(14.dp))
        Legend()
    }
}

private val COLORS = listOf(
    "lavanda" to "Conceito",
    "menta" to "Exemplo",
    "ambar" to "Atenção",
    "agua" to "Dúvida",
)

private fun colorFor(key: String): Color = when (key) {
    "lavanda" -> Color(0xFFC7B6FF)
    "menta" -> Color(0xFF7BE8C6)
    "ambar" -> Color(0xFFFFC46B)
    "agua" -> Color(0xFF7FB2FF)
    else -> Color(0xFFE6EEF9)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HighlightBucket(
    title: String,
    items: List<String>,
    defaultCor: String,
    vm: OffloadViewModel,
    onSaved: (String) -> Unit,
) {
    if (items.isEmpty()) return
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(
                "${items.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
        Spacer(Modifier.height(8.dp))
        items.forEach { item ->
            var cor by remember(item) { mutableStateOf(defaultCor) }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorFor(cor).copy(alpha = 0.12f))
                    .border(1.dp, colorFor(cor).copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(item, style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    COLORS.forEach { (key, label) ->
                        val active = cor == key
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .background(
                                    if (active) colorFor(key).copy(alpha = 0.24f)
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                )
                                .border(
                                    width = if (active) 1.5.dp else 1.dp,
                                    color = colorFor(key).copy(alpha = if (active) 0.85f else 0.35f),
                                    shape = CircleShape,
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(7.dp).clip(CircleShape).background(colorFor(key)))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 0.95f else 0.65f),
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(
                        onClick = {
                            val cat = COLORS.first { it.first == cor }.second
                            vm.saveHighlight(item, cor, cat)
                            onSaved("Marcado como ${cat.lowercase()}.")
                        },
                        accent = false,
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Salvar marca-texto")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Legend() {
    GlassCard {
        Text("Cores do marca-texto", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            COLORS.forEach { (key, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(colorFor(key)))
                    Spacer(Modifier.width(5.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
