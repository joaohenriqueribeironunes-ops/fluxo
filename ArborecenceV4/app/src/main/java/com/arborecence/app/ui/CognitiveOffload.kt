package com.arborecence.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class OffloadBuckets(val tarefas: List<String>, val ideias: List<String>, val emocoes: List<String>, val duvidas: List<String>)

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

@Composable
fun CognitiveOffloadSection() {
    var input by remember { mutableStateOf("") }
    var out by remember { mutableStateOf<OffloadBuckets?>(null) }
    Text("Descarregamento cognitivo", style = MaterialTheme.typography.titleSmall)
    Text("Fale ou digite livremente. Eu separo em tarefas, ideias, emoções e dúvidas (organização local, sem exigir estrutura).", style = MaterialTheme.typography.bodySmall)
    OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Escreva/cole o que está na cabeça") }, minLines = 3)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { out = organizeOffload(input) }, enabled = input.isNotBlank()) { Text("Organizar") }
    }
    out?.let { b ->
        Bucket("Tarefas", b.tarefas); Bucket("Ideias", b.ideias); Bucket("Emoções", b.emocoes); Bucket("Dúvidas", b.duvidas)
    }
}

@Composable
private fun Bucket(title: String, items: List<String>) {
    if (items.isEmpty()) return
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        items.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
    } }
}
