package com.arborecence.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.arborecence.aiengine.AiEngine
import com.arborecence.aiengine.AiResult

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BehaviorScreen(vm: BehaviorViewModel, modifier: Modifier = Modifier) {
    val s by vm.state.collectAsState()
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Tradutor social (offline)", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Duas leituras do mesmo fato: como pode ser percebido por alguém ND e por alguém NT. Nenhuma é a “certa” — são traduções.",
            style = MaterialTheme.typography.bodySmall,
        )
        if (s.dbCount != null) {
            val ok = (s.dbCount ?: 0) > 0
            Text(
                if (ok) "Banco local: ${s.dbCount} respostas disponíveis." else "Banco local vazio. Rode o seed local para popular (veja README/docs).",
                style = MaterialTheme.typography.labelMedium,
                color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        OutlinedTextField(
            value = s.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Descreva a situação (o que aconteceu, quem estava, o que foi dito)") },
            minLines = 2,
        )
        Text("Tipo de relação", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            s.relacoes.forEach { r ->
                FilterChip(selected = s.relacao == r, onClick = { vm.onRelacaoChange(r) }, label = { Text(r) })
            }
        }
        Text("Modo de resposta", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AiEngine.Modo.entries.forEach { m ->
                FilterChip(selected = s.modo == m, onClick = { vm.onModoChange(m) }, label = {
                    Text(when (m) { AiEngine.Modo.CURTO -> "Curto"; AiEngine.Modo.PADRAO -> "Padrão"; AiEngine.Modo.DETALHADO -> "Detalhado" })
                })
            }
        }
        Button(onClick = { vm.ask(both = true) }, enabled = !s.loading && s.query.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            if (s.loading) CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
            else Text("Ver leitura ND e NT")
        }
        s.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        s.resultNd?.let { ResultCard("Leitura ND (como pode ser percebido/sentido)", it) }
        s.resultNt?.let { ResultCard("Leitura NT (como pode ser interpretado)", it) }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ResultCard(title: String, r: AiResult) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(r.texto, style = MaterialTheme.typography.bodyMedium)
            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                Text(
                    "Fonte: ${r.fonte} · Confiança: ${"%.2f".format(r.nivelConfianca)} · ${r.meta}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
