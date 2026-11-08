package com.equilibrio.core.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.equilibrio.core.ui.theme.LocalAccessibility
import com.equilibrio.core.ui.theme.LocalRiskScore
import com.equilibrio.core.ui.theme.riskPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidBackground(
    modifier: Modifier = Modifier,
    riskScore: Int = LocalRiskScore.current,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    val accessibility = LocalAccessibility.current
    val palette = riskPalette(riskScore, darkTheme)
    val phase = if (accessibility.reduceMotion) 0.85f else animatedPhase()
    val boost = if (darkTheme) 1f else 0.65f

    Canvas(modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    palette.gradientTop,
                    palette.gradientMiddle,
                    palette.gradientBottom
                )
            )
        )
        val w = size.width
        val h = size.height

        fun blob(x: Float, y: Float, radius: Float, color: Color, alpha: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(x, y),
                    radius = radius
                ),
                radius = radius,
                center = Offset(x, y)
            )
        }

        blob(
            x = w * (0.22f + 0.07f * cos(phase)),
            y = h * (0.16f + 0.05f * sin(phase * 0.8f)),
            radius = w * 0.85f,
            color = palette.calm,
            alpha = 0.30f * boost
        )
        blob(
            x = w * (0.92f + 0.05f * cos(phase * 0.7f + 1.4f)),
            y = h * (0.36f + 0.06f * sin(phase)),
            radius = w * 0.75f,
            color = palette.accent,
            alpha = 0.24f * boost
        )
        blob(
            x = w * (0.18f + 0.06f * sin(phase * 0.9f + 2.0f)),
            y = h * (0.92f + 0.04f * cos(phase)),
            radius = w * 0.9f,
            color = if (riskScore >= 40) palette.high else palette.calm,
            alpha = 0.16f * boost
        )

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black.copy(alpha = 0.03f), Color.Black.copy(alpha = 0.16f))
            )
        )
    }
}

@Composable
private fun animatedPhase(): Float {
    val transition = rememberInfiniteTransition(label = "liquid-background")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 32_000, easing = LinearEasing)
        ),
        label = "phase"
    )
    return phase
}
