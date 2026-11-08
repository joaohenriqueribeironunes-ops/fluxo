package com.equilibrio.core.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.equilibrio.core.model.DayPart
import com.equilibrio.core.model.HeatmapCell
import com.equilibrio.core.model.RiskLevel
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassTone
import com.equilibrio.core.ui.theme.LocalAccessibility
import com.equilibrio.core.ui.theme.LocalRiskScore
import com.equilibrio.core.ui.theme.riskPalette

@Composable
fun SectionTitle(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    accent: Color? = null
) {
    Column(modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = (accent ?: MaterialTheme.colorScheme.onBackground).copy(alpha = 0.62f)
            )
        }
    }
}

@Composable
fun RiskBadge(level: RiskLevel, score: Int? = null, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val palette = riskPalette(score ?: if (level == RiskLevel.ALTO) 80 else if (level == RiskLevel.MODERADO) 55 else 20, dark)
    val color = when (level) {
        RiskLevel.BAIXO -> palette.calm
        RiskLevel.MODERADO -> palette.moderate
        RiskLevel.ALTO -> palette.high
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.18f))
            .border(1.dp, color.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(7.dp))
        Text(
            text = level.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (score != null) {
            Text(
                text = " · $score",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
fun MicIndicator(active: Boolean, modifier: Modifier = Modifier) {
    val accessibility = LocalAccessibility.current
    val transition = rememberInfiniteTransition(label = "mic")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic-pulse"
    )
    val alpha = when {
        !active -> 0.35f
        accessibility.reduceMotion -> 1f
        else -> pulse
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (active) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Mic,
            contentDescription = null,
            tint = if (active) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp).alpha(alpha)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (active) "Microfone ativo · processando e descartando" else "Microfone pausado",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 0.85f else 0.5f)
        )
    }
}

@Composable
fun PrivacyDisclaimer(modifier: Modifier = Modifier, compact: Boolean = false) {
    GlassCard(modifier = modifier, tone = GlassTone.Calm, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Autoconhecimento, não diagnóstico",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (compact) {
                        "Ferramenta pessoal. Não substitui avaliação profissional. Em risco à segurança: CVV 188 ou SAMU 192."
                    } else {
                        "O Equilíbrio ajuda você a observar padrões e estimar riscos. Não faz diagnóstico, não indica medicação e não substitui acompanhamento profissional. Em crise com risco à segurança, procure alguém de confiança, ligue 188 (CVV) ou 192 (SAMU)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(34.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    helper: String? = null,
    modifier: Modifier = Modifier,
    tone: GlassTone = GlassTone.Neutral
) {
    GlassCard(modifier = modifier, tone = tone, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (helper != null) {
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
            )
        }
    }
}

@Composable
fun HeatmapGrid(cells: List<HeatmapCell>, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val palette = riskPalette(LocalRiskScore.current, dark)
    val max = cells.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val weekdays = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Spacer(Modifier.width(30.dp))
            DayPart.entries.forEach { part ->
                Text(
                    text = part.label.take(3),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        for (day in 1..7) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weekdays[day - 1],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    modifier = Modifier.width(30.dp)
                )
                DayPart.entries.forEach { part ->
                    val cell = cells.firstOrNull { it.dayOfWeek == day && it.dayPart == part }
                    val count = cell?.count ?: 0
                    val intensity = count.toFloat() / max
                    Box(
                        Modifier
                            .weight(1f)
                            .height(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                if (count == 0) {
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                                } else {
                                    palette.accent.copy(alpha = 0.18f + 0.6f * intensity)
                                }
                            )
                            .border(
                                0.8.dp,
                                if (count == 0) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f)
                                else palette.accent.copy(alpha = 0.35f),
                                RoundedCornerShape(9.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (count > 0) {
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HourlyHeatmapGrid(cells: List<HeatmapCell>, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val palette = riskPalette(LocalRiskScore.current, dark)
    val max = cells.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val weekdays = listOf("S", "T", "Q", "Q", "S", "S", "D")
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Spacer(Modifier.width(18.dp))
            listOf(0, 6, 12, 18).forEach { h ->
                Text(
                    text = "${h}h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    modifier = Modifier.weight(6f),
                    textAlign = TextAlign.Start
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        for (day in 1..7) {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = weekdays[day - 1],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.width(18.dp)
                )
                for (hour in 0..23) {
                    val cell = cells.firstOrNull { it.dayOfWeek == day && it.hour == hour }
                        ?: cells.firstOrNull { it.dayOfWeek == day && DayPart.fromHour(hour) == it.dayPart && it.hour < 0 }
                    val count = cell?.count ?: 0
                    val intensity = count.toFloat() / max
                    Box(
                        Modifier
                            .weight(1f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (count == 0) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f)
                                else palette.accent.copy(alpha = 0.18f + 0.65f * intensity)
                            )
                    )
                }
            }
        }
        Text(
            text = "Mapa: dia da semana × hora (mais escuro = mais ocorrências).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}
