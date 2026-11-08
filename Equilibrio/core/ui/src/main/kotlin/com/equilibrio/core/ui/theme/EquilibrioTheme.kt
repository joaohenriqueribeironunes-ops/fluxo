package com.equilibrio.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val CalmTeal = Color(0xFF63C7B2)
val CalmTealDeep = Color(0xFF2F6F6A)
val SoftAmber = Color(0xFFE8B45A)
val SoftCoral = Color(0xFFF08A7E)
val IosGreen = Color(0xFF34C759)
val Ink = Color(0xFF101A1C)
val Mist = Color(0xFFEAF2F1)

private val DarkColors = darkColorScheme(
    primary = CalmTeal,
    onPrimary = Color(0xFF06211D),
    primaryContainer = CalmTealDeep,
    onPrimaryContainer = Color(0xFFE9FFFA),
    secondary = SoftAmber,
    onSecondary = Color(0xFF2B1F06),
    tertiary = IosGreen,
    background = Color(0xFF091214),
    onBackground = Color(0xFFF2F8F7),
    surface = Color(0xFF0E1A1C),
    onSurface = Color(0xFFF2F8F7),
    surfaceVariant = Color(0xFF172528),
    onSurfaceVariant = Color(0xFFC3D2D1),
    outline = Color(0xFF6F8384),
    error = Color(0xFFF08A7E),
    onError = Color(0xFF2B0B07)
)

private val LightColors = lightColorScheme(
    primary = CalmTealDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDEDE5),
    onPrimaryContainer = Color(0xFF06211D),
    secondary = Color(0xFF8A5A12),
    onSecondary = Color.White,
    tertiary = Color(0xFF187A33),
    background = Color(0xFFF0F6F5),
    onBackground = Color(0xFF10201F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10201F),
    surfaceVariant = Color(0xFFDDE9E7),
    onSurfaceVariant = Color(0xFF334847),
    outline = Color(0xFF5D7573),
    error = Color(0xFF9C3A2F),
    onError = Color.White
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun EquilibrioTheme(
    darkTheme: Boolean,
    accessibility: AccessibilitySettings,
    riskScore: Int = 0,
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkColors else LightColors
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalAccessibility provides accessibility,
        LocalRiskScore provides riskScore,
        LocalDensity provides Density(density.density, density.fontScale * accessibility.fontScale)
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
