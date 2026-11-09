package com.lambda.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.coreui.design.GlassBadge
import com.lambda.coreui.design.GlassButton
import com.lambda.coreui.design.GlassCard
import com.lambda.coreui.design.GlassHeader
import com.lambda.coreui.design.GlassPanel
import com.lambda.coreui.design.GlassTextField
import com.lambda.coreui.design.SectionLabel

@Composable
fun StudyScreen(vm: StudyViewModel, modifier: Modifier = Modifier) {
    val s by vm.state.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GlassHeader(
            title = "Estudos",
            subtitle = "Explicações, etimologia e descarregamento — sem internet.",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassBadge(text = "frases: ${s.fraseCount ?: "…"}", accent = true)
            GlassBadge(text = "etimologias: ${s.etimCount ?: "…"}")
        }

        GlassPanel(contentPadding = PaddingValues(18.dp)) {
            SectionLabel("Buscar")
            Spacer(Modifier.height(8.dp))
            GlassTextField(
                value = s.query,
                onValueChange = vm::onQuery,
                label = "Conceito, tema ou termo",
                singleLine = true,
            )
            Spacer(Modifier.height(10.dp))
            GlassButton(
                onClick = vm::search,
                enabled = !s.loading && s.query.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (s.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Buscando…")
                } else {
                    Text("Buscar no banco local", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (s.frases.isNotEmpty()) {
            SectionLabel("Frases explicativas")
            s.frases.forEach { f ->
                GlassCard {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlassBadge(text = f.angulo, accent = true)
                        GlassBadge(text = "confiança ${"%.0f".format(f.nivelConfianca * 100)}%")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(f.texto, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Fonte: ${f.fonte}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
            }
        } else if (s.query.isNotBlank() && !s.loading) {
            Text(
                "Nada encontrado. Tente uma palavra mais geral ou outro termo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        if (s.etimologias.isNotEmpty()) {
            SectionLabel("Etimologia")
            s.etimologias.forEach { e ->
                GlassCard {
                    Text(e.termo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Raiz: ${e.raizOriginal}", style = MaterialTheme.typography.bodySmall)
                    Text("Evolução fonética: ${e.evolucaoFonetica}", style = MaterialTheme.typography.bodySmall)
                    Text("Mudança semântica: ${e.mudancaSemantica}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Fonte: ${e.fonte}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
            }
        }

        CognitiveOffloadSection()
        TaskStartChecklist()
        HyperfocusHint()

        Text(
            "Revisão espaçada e linhas do tempo estão em evolução. O app não usa pomodoro.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
