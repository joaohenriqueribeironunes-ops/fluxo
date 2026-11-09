package com.arborecence.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StudyScreen(vm: StudyViewModel, modifier: Modifier = Modifier) {
    val s by vm.state.collectAsState()
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Central de estudos (offline)", style = MaterialTheme.typography.headlineSmall)
        Text("Busca em frases explicativas e etimologia. Frases: ${s.fraseCount ?: "…"} · Etimologias: ${s.etimCount ?: "…"}", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(value = s.query, onValueChange = vm::onQuery, modifier = Modifier.fillMaxWidth(), label = { Text("Buscar conceito, tema ou termo") }, singleLine = true)
        Button(onClick = vm::search, enabled = !s.loading && s.query.isNotBlank()) {
            if (s.loading) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp) else Text("Buscar")
        }
        s.frases.forEach { f ->
            ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Ângulo: ${f.angulo}", style = MaterialTheme.typography.labelLarge)
                Text(f.texto, style = MaterialTheme.typography.bodyMedium)
                Text("Fonte: ${f.fonte} · Confiança: ${"%.2f".format(f.nivelConfianca)}", style = MaterialTheme.typography.labelSmall)
            } }
        }
        if (s.etimologias.isNotEmpty()) {
            Text("Etimologia", style = MaterialTheme.typography.titleSmall)
            s.etimologias.forEach { e ->
                ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(e.termo, style = MaterialTheme.typography.titleSmall)
                    Text("Raiz: ${e.raizOriginal}", style = MaterialTheme.typography.bodySmall)
                    Text("Evolução fonética: ${e.evolucaoFonetica}", style = MaterialTheme.typography.bodySmall)
                    Text("Mudança semântica: ${e.mudancaSemantica}", style = MaterialTheme.typography.bodySmall)
                    Text("Fonte: ${e.fonte} · Confiança: ${"%.2f".format(e.nivelConfianca)}", style = MaterialTheme.typography.labelSmall)
                } }
            }
        }
        CognitiveOffloadSection()
        TaskStartChecklist()
        HyperfocusHint()
        Text("Em evolução: revisão espaçada, mapa de conexões, linhas do tempo (com base nos bancos locais). Sem pomodoro.", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))
    }
}
