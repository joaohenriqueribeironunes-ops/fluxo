package com.arborecence.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arborecence.databehavior.dao.BehaviorDao
import com.arborecence.dataacademic.dao.AcademicDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LearnedStats(val respostas: Int?, val frases: Int?, val etim: Int?, val compromissos: Int?, val destaques: Int?)

@HiltViewModel
class LearnedViewModel @Inject constructor(private val b: BehaviorDao, private val a: AcademicDao) : ViewModel() {
    var stats by mutableStateOf(LearnedStats(null,null,null,null,null)); private set
    fun load() { viewModelScope.launch {
        stats = LearnedStats(
            respostas = runCatching { b.countRespostas() }.getOrNull(),
            frases = runCatching { a.countFrases() }.getOrNull(),
            etim = runCatching { a.countEtimologias() }.getOrNull(),
            compromissos = runCatching { b.listCompromissos().size }.getOrNull(),
            destaques = runCatching { b.listDestaques(1000).size }.getOrNull(),
        )
    } }
}

@Composable
fun LearnedSection(vm: LearnedViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { vm.load() }
    val s = vm.stats
    ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("O que o app aprendeu / tem disponível (local)", style = MaterialTheme.typography.titleSmall)
        Text("Tudo local, sem API e sem telemetria. Resumo do seu conteúdo neste aparelho:", style = MaterialTheme.typography.bodySmall)
        Text("• Respostas de relacionamento: ${s.respostas ?: "…"}", style = MaterialTheme.typography.bodySmall)
        Text("• Frases acadêmicas: ${s.frases ?: "…"} · Etimologias: ${s.etim ?: "…"}", style = MaterialTheme.typography.bodySmall)
        Text("• Seus compromissos: ${s.compromissos ?: "…"} · Seus marca-textos: ${s.destaques ?: "…"}", style = MaterialTheme.typography.bodySmall)
        Text("Preferências de acessibilidade/modo são lembradas localmente. Expansão do aprendizado por padrões de uso (100% local): em evolução.", style = MaterialTheme.typography.bodySmall)
    } }
}
