package com.equilibrio.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Composable
fun riskPalette(score: Int, darkTheme: Boolean): RiskPalette {
    val calm = if (darkTheme) Color(0xFF5FC9B3) else Color(0xFF2F7F76)
    val moderate = if (darkTheme) Color(0xFFE7B45F) else Color(0xFFA36C18)
    val high = if (darkTheme) Color(0xFFF09184) else Color(0xFFB4544A)
    return remember(score, darkTheme) {
        val s = score.coerceIn(0, 100) / 100f
        val accent = when {
            s < 0.4f -> lerp(calm, moderate, s / 0.4f)
            else -> lerp(moderate, high, (s - 0.4f) / 0.6f)
        }
        if (darkTheme) {
            RiskPalette(
                calm = calm,
                moderate = moderate,
                high = high,
                accent = accent,
                gradientTop = lerp(Color(0xFF0B2024), Color(0xFF2A1D13), s),
                gradientMiddle = lerp(Color(0xFF123033), accent.copy(alpha = 0.55f), s * 0.75f),
                gradientBottom = lerp(Color(0xFF081113), Color(0xFF2A1512), s * 0.85f),
                onAccent = Color(0xFF071516)
            )
        } else {
            RiskPalette(
                calm = calm,
                moderate = moderate,
                high = high,
                accent = accent,
                gradientTop = lerp(Color(0xFFE4F3F0), Color(0xFFF9EADF), s),
                gradientMiddle = lerp(Color(0xFFD6ECE8), accent.copy(alpha = 0.30f), s * 0.7f),
                gradientBottom = lerp(Color(0xFFEDF5F3), Color(0xFFF8DCD6), s * 0.85f),
                onAccent = Color.White
            )
        }
    }
}
