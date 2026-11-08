package com.equilibrio.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.equilibrio.core.model.RiskLevel
import com.equilibrio.core.ui.theme.LocalAccessibility
import com.equilibrio.core.ui.theme.riskPalette

@Composable
fun RiskGauge(
    score: Int,
    level: RiskLevel,
    baselineReady: Boolean,
    modifier: Modifier = Modifier,
    learningLabel: String = "aprendendo seus padrões"
) {
    val accessibility = LocalAccessibility.current
    val dark = isSystemInDarkTheme()
    val palette = riskPalette(score, dark)
    val target = if (baselineReady) (score.coerceIn(0, 100) / 100f) else 0.04f
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = if (accessibility.reduceMotion) snap()
        else spring(dampingRatio = 0.82f, stiffness = 90f),
        label = "risk-gauge"
    )

    Box(modifier.size(238.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.085f
            val inset = strokeWidth * 1.2f
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)

            rotate(135f) {
                drawArc(
                    color = Color.White.copy(alpha = if (dark) 0.09f else 0.55f),
                    startAngle = 0f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(palette.calm, palette.moderate, palette.high, palette.calm)
                    ),
                    startAngle = 0f,
                    sweepAngle = 270f * progress.coerceIn(0.001f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (baselineReady) score.toString() else "—",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (baselineReady) "risco agora" else learningLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
            if (baselineReady) {
                Text(
                    text = level.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.accent
                )
            }
        }
    }
}

@Composable
fun RiskBar(
    label: String,
    score: Int,
    modifier: Modifier = Modifier,
    showValue: Boolean = true
) {
    val dark = isSystemInDarkTheme()
    val palette = riskPalette(score, dark)
    val accessibility = LocalAccessibility.current
    val progress by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f),
        animationSpec = if (accessibility.reduceMotion) snap() else spring(stiffness = 120f),
        label = "risk-bar"
    )
    Column(modifier) {
        Text(
            text = if (showValue) "$label · $score" else label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
            )
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(palette.calm, palette.accent)),
                size = Size(size.width * progress, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
            )
        }
    }
}
