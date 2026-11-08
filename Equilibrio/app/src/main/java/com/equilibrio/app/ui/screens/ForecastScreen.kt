package com.equilibrio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equilibrio.app.ui.MainViewModel
import com.equilibrio.app.ui.components.ForecastCard
import com.equilibrio.app.ui.components.ForecastDetailDialog
import com.equilibrio.app.ui.components.ScreenScaffold
import com.equilibrio.app.ui.components.SingleChoiceChips
import com.equilibrio.core.model.RiskForecast
import com.equilibrio.core.ui.components.EmptyState
import com.equilibrio.core.ui.glass.GlassButton
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassTone

@Composable
fun ForecastScreen(
    viewModel: MainViewModel,
    onOpenCalm: () -> Unit,
    onOpenInsights: () -> Unit
) {
    val shortTerm by viewModel.shortTerm.collectAsStateWithLifecycle()
    val today by viewModel.todayForecasts.collectAsStateWithLifecycle()
    val week by viewModel.weekForecasts.collectAsStateWithLifecycle()
    val risk by viewModel.risk.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<RiskForecast?>(null) }

    val tabs = listOf("30–120 min", "Hoje", "7 dias")
    val forecasts = when (tab) {
        0 -> shortTerm
        1 -> today
        else -> week
    }

    ScreenScaffold(
        title = "Previsão",
        subtitle = "Estimativas calmas, com o tamanho da incerteza à mostra"
    ) {
        GlassCard(tone = GlassTone.Calm) {
            Text(
                text = "Como ler isto",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "A probabilidade vem do seu baseline pessoal, de regras simples e do histórico de crises. " +
                    "30–120 min é a janela mais útil; 7 dias serve para planejar, não para prever o futuro. " +
                    "Confiança baixa significa que o modelo ainda tem pouco histórico.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        if (risk?.baselineReady != true) {
            GlassCard(tone = GlassTone.Warm) {
                Text(
                    text = "Baseline em construção",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "A previsão só é liberada quando o app completa o aprendizado inicial. " +
                        "Enquanto isso, os check-ins e a linha do tempo continuam funcionando.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        SingleChoiceChips(
            title = "Horizonte",
            options = tabs,
            selected = tabs[tab],
            onSelect = { label -> tab = tabs.indexOf(label).coerceAtLeast(0) }
        )

        if (forecasts.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Insights,
                title = "Sem janelas para este horizonte",
                body = if (tab == 2) {
                    "A visão de 7 dias aparece quando há crises suficientes para estimar padrões por dia e período."
                } else {
                    "Ainda não há dados suficientes. Continue a coleta e os check-ins; o modelo local precisa de histórico."
                }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                forecasts.forEach { forecast ->
                    ForecastCard(forecast = forecast, onClick = { detail = forecast })
                }
            }
        }

        GlassButton(
            text = "Abrir pausa guiada",
            onClick = onOpenCalm,
            modifier = Modifier.fillMaxWidth()
        )
        GlassButton(
            text = "Ver padrões e precisão",
            onClick = onOpenInsights,
            primary = false,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "As janelas são hipóteses, não sentenças. Se uma previsão não fizer sentido, marque como falso alarme no aviso. " +
                "Isso ensina o modelo e evita repetir o mesmo erro.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
    }

    detail?.let { forecast ->
        ForecastDetailDialog(forecast = forecast, onDismiss = { detail = null })
    }
}
