package com.equilibrio.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equilibrio.app.ui.MainViewModel
import com.equilibrio.app.ui.components.ScreenScaffold
import com.equilibrio.core.domain.time.TimeUtils
import com.equilibrio.core.model.TimelineItem
import com.equilibrio.core.ui.components.EmptyState
import com.equilibrio.core.ui.components.RiskBar
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassTone
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TimelineScreen(
    viewModel: MainViewModel,
    onOpenCrises: () -> Unit,
    onOpenReport: () -> Unit
) {
    val day by viewModel.selectedDay.collectAsStateWithLifecycle()
    val items by viewModel.timeline.collectAsStateWithLifecycle()
    val today = TimeUtils.epochDay(System.currentTimeMillis())

    ScreenScaffold(
        title = "Linha do tempo",
        subtitle = if (day == today) "Hoje" else formatEpochDay(day),
        scrollable = false,
        actions = {
            Icon(
                Icons.Outlined.WarningAmber,
                contentDescription = "Histórico de crises",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenCrises)
                    .padding(9.dp)
            )
            Icon(
                Icons.Outlined.Description,
                contentDescription = "Relatório",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenReport)
                    .padding(9.dp)
            )
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                    .clickable { viewModel.shiftDay(-1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ChevronLeft, "Dia anterior", tint = MaterialTheme.colorScheme.onBackground)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (day == today) "Hoje" else formatEpochDay(day),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f))
                    .clickable(enabled = day < today) { viewModel.shiftDay(1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    "Próximo dia",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (day < today) 1f else 0.3f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Timeline,
                title = "Nada registrado neste dia",
                body = "Janelas de sensores, check-ins e crises aparecem aqui. O dia ainda pode estar começando."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items) { item ->
                    TimelineRow(item = item, onOpenCrises = onOpenCrises)
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(item: TimelineItem, onOpenCrises: () -> Unit) {
    val time = TimeUtils.formatHm(item.timestamp)
    when (item) {
        is TimelineItem.SensorItem -> {
            val window = item.window
            GlassCard(padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        time,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.width(46.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Janela de sensores",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            buildString {
                                if (window.micCaptured) append("som ${window.soundDbMean.toInt()} dB · ")
                                append("agitação ${window.agitationIndex.toInt()}")
                                append(" · luz ${window.lightMeanLux.toInt()} lx")
                                if (window.proximityNear) append(" · perto")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                        )
                    }
                    Text(
                        "${window.riskScore}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (window.riskScore > 0) {
                    Spacer(Modifier.height(8.dp))
                    RiskBar(label = "Risco na janela", score = window.riskScore)
                }
            }
        }
        is TimelineItem.CheckInItem -> {
            val checkIn = item.checkIn
            GlassCard(tone = GlassTone.Calm, padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        time,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.width(46.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Check-in",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = checkIn.emotions.joinToString(", ") { it.label }
                                .ifBlank { "sem emoção marcada" } +
                                " · intensidade ${checkIn.emotionIntensity}/10" +
                                if (checkIn.sleepHours > 0f) " · sono ${"%.1f".format(checkIn.sleepHours)} h" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                        )
                        if (checkIn.activities.isNotEmpty()) {
                            Text(
                                text = checkIn.activities.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
        }
        is TimelineItem.CrisisItem -> {
            val crisis = item.crisis
            GlassCard(
                tone = GlassTone.Alert,
                padding = PaddingValues(14.dp),
                onClick = onOpenCrises
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        time,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.width(46.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            crisis.type.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = buildString {
                                append("Intensidade ${crisis.intensity}/10")
                                if (crisis.recoveryMinutes > 0) {
                                    append(" · recuperação ${crisis.recoveryMinutes} min")
                                }
                                if (crisis.retroactive) append(" · registro retroativo")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                        )
                        if (crisis.triggers.isNotEmpty()) {
                            Text(
                                "Gatilhos: ${crisis.triggers.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
        }
        is TimelineItem.AnalysisItem -> {
            GlassCard(tone = GlassTone.Neutral, padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp).size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Análise assistida · risco ${item.analysis.riskCurrent}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            item.analysis.analysisText.take(260),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                        )
                    }
                }
            }
        }
        is TimelineItem.ForecastItem -> {
            GlassCard(padding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        time,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.width(46.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Janela de risco prevista",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${item.forecast.crisisType?.label ?: "tipo incerto"} · confiança ${item.forecast.confidence.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                        )
                    }
                    Text(
                        "${item.forecast.percent}%",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun formatEpochDay(epochDay: Long): String {
    val millis = TimeUtils.epochDayToMillis(epochDay)
    val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(fmt)
}
