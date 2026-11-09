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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lambda.coreui.design.GlassBadge
import com.lambda.coreui.design.GlassButton
import com.lambda.coreui.design.GlassCard
import com.lambda.coreui.design.GlassHeader
import com.lambda.coreui.design.GlassIconButton
import com.lambda.coreui.design.GlassPanel
import com.lambda.coreui.design.GlassTextField
import com.lambda.coreui.design.SectionLabel
import com.lambda.databehavior.entity.Compromisso
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CommitmentsScreen(vm: CommitmentsViewModel, modifier: Modifier = Modifier) {
    val items by vm.items.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var titulo by rememberSaveable { mutableStateOf("") }
    var desc by rememberSaveable { mutableStateOf("") }
    var local by rememberSaveable { mutableStateOf("") }
    var quando by rememberSaveable { mutableStateOf("") }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GlassHeader(
            title = "Compromissos",
            subtitle = "Simples e previsível. Salvo só neste aparelho.",
            trailing = {
                GlassIconButton(
                    icon = if (adding) Icons.Rounded.Close else Icons.Rounded.Add,
                    contentDescription = if (adding) "Fechar formulário" else "Novo compromisso",
                    onClick = { adding = !adding },
                    size = 46.dp,
                )
            },
        )

        if (adding) {
            GlassPanel(contentPadding = PaddingValues(18.dp)) {
                SectionLabel("Novo compromisso")
                Spacer(Modifier.height(8.dp))
                GlassTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = "Compromisso (obrigatório)",
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                GlassTextField(
                    value = quando,
                    onValueChange = { quando = it },
                    label = "Data/hora (opcional, ex.: 10/11/2026 14:00)",
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                GlassTextField(
                    value = local,
                    onValueChange = { local = it },
                    label = "Local (opcional)",
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                GlassTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = "Observação (opcional)",
                    minLines = 2,
                )
                Spacer(Modifier.height(12.dp))
                GlassButton(
                    onClick = {
                        vm.add(titulo, desc, parseDateMillis(quando), local)
                        titulo = ""; desc = ""; local = ""; quando = ""
                        adding = false
                    },
                    enabled = titulo.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Salvar compromisso", fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            GlassButton(onClick = { adding = true }, accent = false, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Novo compromisso")
            }
        }

        val open = items.filter { !it.concluido }
        val done = items.filter { it.concluido }

        if (open.isNotEmpty()) {
            SectionLabel("A fazer")
            open.forEach { c ->
                CommitmentCard(c, onToggle = { vm.toggle(c.id, it) }, onDelete = { vm.delete(c.id) })
            }
        }
        if (done.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            SectionLabel("Concluídos")
            done.forEach { c ->
                CommitmentCard(c, onToggle = { vm.toggle(c.id, it) }, onDelete = { vm.delete(c.id) })
            }
        }
        if (items.isEmpty()) {
            GlassCard {
                Text(
                    "Nada marcado ainda.",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Quando quiser, adicione um compromisso. Sem notificações inesperadas: você decide quando olhar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                )
            }
        }
    }
}

@Composable
private fun CommitmentCard(c: Compromisso, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    GlassCard {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = c.concluido,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f),
                ),
            )
            Column(Modifier.weight(1f).padding(start = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    c.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (c.concluido) TextDecoration.LineThrough else TextDecoration.None,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (c.concluido) 0.55f else 1f),
                )
                c.dataHoraMillis?.let {
                    GlassBadge(text = formatDate(it), accent = true)
                }
                if (c.local.isNotBlank()) {
                    Text(
                        "Local: ${c.local}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                }
                if (c.descricao.isNotBlank()) {
                    Text(
                        c.descricao,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                }
            }
            GlassIconButton(
                icon = Icons.Rounded.DeleteOutline,
                contentDescription = "Remover compromisso",
                onClick = onDelete,
                size = 38.dp,
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
            )
        }
    }
}

private val DATE_FORMATS = listOf("dd/MM/yyyy HH:mm", "dd/MM/yyyy", "yyyy-MM-dd HH:mm", "yyyy-MM-dd")
private val BR = Locale("pt", "BR")

private fun parseDateMillis(s: String): Long? {
    val t = s.trim()
    if (t.isBlank()) return null
    for (pattern in DATE_FORMATS) {
        val fmt = DateTimeFormatter.ofPattern(pattern, BR)
        val parsed = runCatching {
            if (pattern.contains("H")) LocalDateTime.parse(t, fmt) else LocalDate.parse(t, fmt).atStartOfDay()
        }.getOrNull() ?: continue
        return parsed.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    return null
}

private fun formatDate(ms: Long): String =
    Instant.ofEpochMilli(ms)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM · HH:mm", BR))
