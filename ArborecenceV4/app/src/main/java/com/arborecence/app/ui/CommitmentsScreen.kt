package com.arborecence.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CommitmentsScreen(vm: CommitmentsViewModel, modifier: Modifier = Modifier) {
    val items by vm.items.collectAsState()
    LaunchedEffect(Unit) { vm.load() }
    var titulo by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var local by remember { mutableStateOf("") }
    var quando by remember { mutableStateOf("") } // texto simples (ex.: 2026-11-10 14:00) para previsibilidade
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Meus compromissos", style = MaterialTheme.typography.headlineSmall)
        Text("Marque compromissos de forma simples e previsível. Tudo fica salvo localmente neste aparelho.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(titulo, { titulo = it }, Modifier.fillMaxWidth(), label = { Text("Compromisso (obrigatório)") }, singleLine = true)
        OutlinedTextField(quando, { quando = it }, Modifier.fillMaxWidth(), label = { Text("Data/hora (opcional, ex.: 10/11/2026 14:00)") }, singleLine = true)
        OutlinedTextField(local, { local = it }, Modifier.fillMaxWidth(), label = { Text("Local (opcional)") }, singleLine = true)
        OutlinedTextField(desc, { desc = it }, Modifier.fillMaxWidth(), label = { Text("Observação (opcional)") })
        Button(onClick = {
            val ms = parseDateMillis(quando)
            vm.add(titulo, desc, ms, local)
            titulo = ""; desc = ""; local = ""; quando = ""
        }, enabled = titulo.isNotBlank()) { Text("Salvar compromisso") }

        val open = items.filter { !it.concluido }
        val done = items.filter { it.concluido }
        if (open.isNotEmpty()) Text("A fazer", style = MaterialTheme.typography.titleSmall)
        open.forEach { c -> CommitmentRow(c, onToggle = { vm.toggle(c.id, it) }, onDelete = { vm.delete(c.id) }) }
        if (done.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text("Concluídos", style = MaterialTheme.typography.titleSmall)
            done.forEach { c -> CommitmentRow(c, onToggle = { vm.toggle(c.id, it) }, onDelete = { vm.delete(c.id) }) }
        }
        if (items.isEmpty()) Text("Nenhum compromisso marcado ainda.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CommitmentRow(c: com.arborecence.databehavior.entity.Compromisso, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.Top) {
            Checkbox(checked = c.concluido, onCheckedChange = onToggle)
            Column(Modifier.weight(1f).padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(c.titulo, style = MaterialTheme.typography.titleSmall)
                c.dataHoraMillis?.let { Text(formatDate(it), style = MaterialTheme.typography.bodySmall) }
                if (c.local.isNotBlank()) Text("Local: ${c.local}", style = MaterialTheme.typography.bodySmall)
                if (c.descricao.isNotBlank()) Text(c.descricao, style = MaterialTheme.typography.bodySmall)
                Text("Fonte: ${c.fonte} · Confiança: ${"%.2f".format(c.nivelConfianca)}", style = MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onDelete) { Text("Remover") }
        }
    }
}

private fun parseDateMillis(s: String): Long? {
    val t = s.trim(); if (t.isBlank()) return null
    val fmts = listOf("dd/MM/yyyy HH:mm", "dd/MM/yyyy", "yyyy-MM-dd HH:mm", "yyyy-MM-dd")
    for (f in fmts) try { return SimpleDateFormat(f, Locale("pt", "BR")).parse(t)?.time } catch (_: Exception) {}
    return null
}
private fun formatDate(ms: Long): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date(ms))
