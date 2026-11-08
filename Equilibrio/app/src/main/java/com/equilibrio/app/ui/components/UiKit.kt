package com.equilibrio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.equilibrio.core.domain.time.TimeUtils
import com.equilibrio.core.model.RiskFactor
import com.equilibrio.core.model.RiskForecast
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassChip
import com.equilibrio.core.ui.glass.GlassToggle
import com.equilibrio.core.ui.glass.GlassTone
import com.equilibrio.core.ui.components.RiskBar
import com.equilibrio.core.ui.components.SectionTitle

/**
 * Estrutura comum das telas: fundo líquido já desenhado pelo NavHost, cabeçalho
 * translúcido e conteúdo com rolagem. O espaçamento e os cantos seguem o mesmo
 * ritmo do Liquid Glass (24–32 dp).
 */
@Composable
fun ScreenScaffold(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    scrollable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                        .padding(10.dp)
                )
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
            actions()
        }
        if (scrollable) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 4.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 18.dp)
                    .padding(top = 4.dp, bottom = 12.dp),
                content = content
            )
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 0.6f else 0.35f)
            )
        }
        Spacer(Modifier.width(12.dp))
        GlassToggle(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun GlassSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    description: String? = null,
    enabled: Boolean = true
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f)
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipGroup(
    title: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    description: String? = null
) {
    Column {
        SectionTitle(title = title, subtitle = description)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                GlassChip(
                    text = option,
                    selected = option in selected,
                    onClick = { onToggle(option) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SingleChoiceChips(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    Column {
        SectionTitle(title = title)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                GlassChip(
                    text = option,
                    selected = option == selected,
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Composable
fun ForecastCard(forecast: RiskForecast, onClick: () -> Unit = {}) {
    val tone = when {
        forecast.percent >= 70 -> GlassTone.Alert
        forecast.percent >= 40 -> GlassTone.Warm
        else -> GlassTone.Calm
    }
    GlassCard(onClick = onClick, tone = tone) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${TimeUtils.formatHm(forecast.windowStart)} – ${TimeUtils.formatHm(forecast.windowEnd)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
                Text(
                    text = forecast.crisisType?.label ?: "Tipo ainda incerto",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "${forecast.percent}%",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(10.dp))
        RiskBar(label = "Probabilidade estimada", score = forecast.percent)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Confiança ${forecast.confidence.label}" +
                if (forecast.factors.isNotEmpty()) " · ${forecast.factors.first()}" else "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ForecastDetailDialog(forecast: RiskForecast, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Entendi") }
        },
        title = {
            Text("${forecast.percent}% · ${forecast.crisisType?.label ?: "Tipo incerto"}")
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${TimeUtils.formatFull(forecast.windowStart)} até ${TimeUtils.formatHm(forecast.windowEnd)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                if (forecast.factors.isNotEmpty()) {
                    Text("Fatores", style = MaterialTheme.typography.titleMedium)
                    forecast.factors.forEach { factor ->
                        Text("• $factor", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (forecast.preventivePlan.isNotEmpty()) {
                    Text("Plano preventivo", style = MaterialTheme.typography.titleMedium)
                    forecast.preventivePlan.forEach { step ->
                        Text("• $step", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    "Estimativa estatística, não uma certeza. Confiança ${forecast.confidence.label}. " +
                        "Nada aqui substitui avaliação profissional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    )
}

@Composable
fun FactorRow(factor: RiskFactor) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = factor.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "+${factor.contribution.toInt()}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = factor.detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun LabeledText(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
