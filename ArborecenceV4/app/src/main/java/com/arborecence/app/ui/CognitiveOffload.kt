package com.arborecence.app.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arborecence.databehavior.dao.BehaviorDao
import com.arborecence.databehavior.entity.DestaqueCognitivo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class OffloadBuckets(val tarefas: List<String>, val ideias: List<String>, val emocoes: List<String>, val duvidas: List<String>)

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

fun organizeOffload(text: String): OffloadBuckets {
    val lines = text.lines().map { it.trim().trimStart('-', '*', '•', ' ') }.filter { it.length > 2 }
    val tarefas = mutableListOf<String>(); val ideias = mutableListOf<String>()
    val emocoes = mutableListOf<String>(); val duvidas = mutableListOf<String>()
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
    Text("Descarregamento cognitivo + marca-texto", style = MaterialTheme.typography.titleSmall)
    Text(
        "Fale ou digite livremente. Eu organizo em tarefas, ideias, emoções e dúvidas (sem exigir estrutura). " +
            "Use as cores para marcar o que importa (marca-texto do descarregamento).",
        style = MaterialTheme.typography.bodySmall,
    )
    OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Escreva/cole o que está na cabeça (ou use voz)") }, minLines = 3)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "pt-BR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Pode falar livremente. Eu organizo depois.")
            }
            runCatching { voiceLauncher.launch(intent) }
        }) { Text("Falar") }
        Button(onClick = { out = organizeOffload(input) }, enabled = input.isNotBlank()) { Text("Organizar") }
    }
    savedMsg?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
    out?.let { b ->
        HighlightBucket("Tarefas", b.tarefas, "agua", "dúvida/ação") { t, cor, cat -> vm.saveHighlight(t, cor, cat); savedMsg = "Marcado ($cat)." }
        HighlightBucket("Ideias", b.ideias, "lavanda", "conceito") { t, cor, cat -> vm.saveHighlight(t, cor, cat); savedMsg = "Marcado ($cat)." }
        HighlightBucket("Emoções", b.emocoes, "menta", "exemplo") { t, cor, cat -> vm.saveHighlight(t, cor, cat); savedMsg = "Marcado ($cat)." }
        HighlightBucket("Dúvidas", b.duvidas, "ambar", "atenção") { t, cor, cat -> vm.saveHighlight(t, cor, cat); savedMsg = "Marcado ($cat)." }
    }
    Legend()
}

private val COLORS = listOf(
    "lavanda" to "conceito",
    "menta" to "exemplo",
    "ambar" to "atenção/erro comum",
    "agua" to "dúvida",
)
private fun colorFor(key: String): Color = when (key) {
    "lavanda" -> Color(0xFFD9CCFF); "menta" -> Color(0xFFCCF2E1); "ambar" -> Color(0xFFFFE2B8); "agua" -> Color(0xFFCCE8FF)
    else -> Color(0xFFEFEFEF)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HighlightBucket(title: String, items: List<String>, defaultCor: String, defaultCat: String, onSave: (String, String, String) -> Unit) {
    if (items.isEmpty()) return
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        items.forEach { item ->
            var cor by remember(item) { mutableStateOf(defaultCor) }
            Column(Modifier.fillMaxWidth().background(colorFor(cor), RoundedCornerShape(8.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item, style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    COLORS.forEach { (k, cat) -> FilterChip(selected = cor == k, onClick = { cor = k }, label = { Text(cat) }) }
                }
                TextButton(onClick = { onSave(item, cor, COLORS.first { it.first == cor }.second) }) { Text("Salvar marca-texto") }
            }
        }
    } }
}

@Composable
private fun Legend() {
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Cores do marca-texto (descarregamento/estudo)", style = MaterialTheme.typography.labelLarge)
        Text("Lavanda = conceito · Menta = exemplo · Âmbar = atenção/erro comum · Água = dúvida", style = MaterialTheme.typography.bodySmall)
    } }
}
