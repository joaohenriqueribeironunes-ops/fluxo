package com.arborecence.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TaskStartChecklist() {
    val items = remember {
        mutableStateListOf(
            "Definir a próxima ação concreta (1 passo pequeno)" to false,
            "Separar material necessário (só o essencial)" to false,
            "Reduzir distrações por 10 minutos (um foco)" to false,
            "Combinar um ponto de pausa/regulação" to false,
            "Se travar: pedir ajuda ou dividir em passo menor" to false,
        )
    }
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Checklist para iniciar tarefa (função executiva)", style = MaterialTheme.typography.titleSmall)
        items.forEachIndexed { i, (t, checked) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = checked, onCheckedChange = { v -> items[i] = t to v })
                Text(t, style = MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(onClick = { for (i in items.indices) items[i] = items[i].first to false }) { Text("Limpar checklist") }
    } }
}

@Composable
fun HyperfocusHint() {
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Modo hiperfoco (apoio)", style = MaterialTheme.typography.titleSmall)
        Text(
            "Escolha 1 pergunta, 3 conceitos, 2 exemplos e 1 contraexemplo. Avance só nesse recorte. " +
                "Defina um sinal de pausa (alarme/apoio externo) para cuidar do tempo e da regulação — sem pomodoro.",
            style = MaterialTheme.typography.bodySmall,
        )
    } }
}
