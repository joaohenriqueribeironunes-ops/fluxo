package com.lambda.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lambda.coreui.design.GlassCard

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
    GlassCard {
        Text(
            "Checklist para iniciar tarefa",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Apoio de função executiva, sem cobrança.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        )
        Spacer(Modifier.height(6.dp))
        items.forEachIndexed { i, (text, checked) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { v -> items[i] = text to v },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                    ),
                )
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
        }
        TextButton(onClick = { for (i in items.indices) items[i] = items[i].first to false }) {
            Text("Limpar checklist")
        }
    }
}

@Composable
fun HyperfocusHint() {
    GlassCard {
        Text(
            "Modo hiperfoco (apoio)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Escolha 1 pergunta, 3 conceitos, 2 exemplos e 1 contraexemplo. Avance só nesse recorte. " +
                "Defina um sinal de pausa (alarme/apoio externo) para cuidar do tempo e da regulação — sem pomodoro.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
        )
    }
}
