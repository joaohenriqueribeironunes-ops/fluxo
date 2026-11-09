package com.lambda.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.lambda.coreui.design.GlassCard
import com.lambda.databehavior.dao.BehaviorDao
import com.lambda.dataacademic.dao.AcademicDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class LearnedStats(
    val respostas: Int = 0,
    val frases: Int = 0,
    val etim: Int = 0,
    val compromissos: Int = 0,
    val destaques: Int = 0,
)

@HiltViewModel
class LearnedViewModel @Inject constructor(
    private val behaviorDao: BehaviorDao,
    private val academicDao: AcademicDao,
) : ViewModel() {
    val stats: StateFlow<LearnedStats> = combine(
        behaviorDao.countRespostasFlow(),
        academicDao.countFrasesFlow(),
        academicDao.countEtimologiasFlow(),
        behaviorDao.countCompromissosFlow(),
        behaviorDao.countDestaquesFlow(),
    ) { respostas, frases, etim, compromissos, destaques ->
        LearnedStats(respostas, frases, etim, compromissos, destaques)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearnedStats())
}

@Composable
fun LearnedSection(vm: LearnedViewModel = hiltViewModel()) {
    val s by vm.stats.collectAsStateWithLifecycle()

    GlassCard {
        Text(
            "O que está disponível (local)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Tudo local, sem API e sem telemetria.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("• Respostas de relacionamento: ${s.respostas}", style = MaterialTheme.typography.bodySmall)
            Text("• Frases acadêmicas: ${s.frases} · Etimologias: ${s.etim}", style = MaterialTheme.typography.bodySmall)
            Text("• Seus compromissos: ${s.compromissos} · Marca-textos: ${s.destaques}", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Preferências de acessibilidade são lembradas localmente.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
        )
    }
}
