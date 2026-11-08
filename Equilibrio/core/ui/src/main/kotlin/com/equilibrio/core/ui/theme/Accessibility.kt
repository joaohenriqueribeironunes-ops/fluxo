package com.equilibrio.core.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AccessibilitySettings(
    val reduceMotion: Boolean = false,
    val reduceTransparency: Boolean = false,
    val highContrast: Boolean = false,
    val fontScale: Float = 1f
)

val LocalAccessibility = staticCompositionLocalOf { AccessibilitySettings() }
val LocalRiskScore = staticCompositionLocalOf { 0 }

data class RiskPalette(
    val calm: Color,
    val moderate: Color,
    val high: Color,
    val accent: Color,
    val gradientTop: Color,
    val gradientMiddle: Color,
    val gradientBottom: Color,
    val onAccent: Color
)
