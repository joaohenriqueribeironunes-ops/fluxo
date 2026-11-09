package com.lambda.coreui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lambda.coreui.design.GlassStyle
import com.lambda.coreui.design.LocalGlassStyle

/**
 * Paleta Lambda: azul profundo, turquesa e lavanda — vidro líquido sobre aurora.
 * ND = turquesa (percepção/sentido). NT = azul (interpretação/expectativa).
 */
object LambdaPalette {
    val Blue = Color(0xFF5B9DFF)
    val BlueDeep = Color(0xFF2A6BFF)
    val Turquoise = Color(0xFF35D6C4)
    val Lavender = Color(0xFFC7B6FF)
    val Amber = Color(0xFFFFC46B)
    val Nd = Color(0xFF4FE3D0)
    val Nt = Color(0xFF7FB2FF)
}

private val DarkColors = darkColorScheme(
    primary = LambdaPalette.Blue,
    onPrimary = Color(0xFF04122B),
    primaryContainer = Color(0xFF1B3A6B),
    onPrimaryContainer = Color(0xFFDCE9FF),
    secondary = LambdaPalette.Turquoise,
    onSecondary = Color(0xFF03201D),
    secondaryContainer = Color(0xFF0F4742),
    onSecondaryContainer = Color(0xFFC9FFF7),
    tertiary = LambdaPalette.Lavender,
    onTertiary = Color(0xFF1B1036),
    background = Color(0xFF050912),
    onBackground = Color(0xFFEAF2FF),
    surface = Color(0xFF0B1220),
    onSurface = Color(0xFFEAF2FF),
    surfaceVariant = Color(0xFF141E33),
    onSurfaceVariant = Color(0xFFC3D2E8),
    outline = Color(0xFF52708F),
    outlineVariant = Color(0xFF26374F),
    error = Color(0xFFFF8A9A),
    onError = Color(0xFF2A0009),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6BFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E5FF),
    onPrimaryContainer = Color(0xFF001B4D),
    secondary = Color(0xFF0C9E93),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC5F5EF),
    onSecondaryContainer = Color(0xFF00201D),
    tertiary = Color(0xFF6E5BD6),
    onTertiary = Color.White,
    background = Color(0xFFEEF4FB),
    onBackground = Color(0xFF0A1A2F),
    surface = Color(0xFFFBFDFF),
    onSurface = Color(0xFF0A1A2F),
    surfaceVariant = Color(0xFFE2ECF8),
    onSurfaceVariant = Color(0xFF33475F),
    outline = Color(0xFF6B84A0),
    outlineVariant = Color(0xFFC8D8EA),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

private val HighContrastDark = darkColorScheme(
    primary = Color(0xFF9CC4FF),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFF8CF5E8),
    onSecondary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFFFFFFF),
    outline = Color(0xFFFFFFFF),
    error = Color(0xFFFFB4AB),
)

private val HighContrastLight = lightColorScheme(
    primary = Color(0xFF003C9E),
    onPrimary = Color.White,
    secondary = Color(0xFF00544E),
    onSecondary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF000000),
    outline = Color(0xFF000000),
    error = Color(0xFFBA1A1A),
)

private val LambdaTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private fun Typography.scaled(scale: Float): Typography {
    if (scale == 1f) return this
    fun TextStyle.s() = copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)
    return copy(
        displayLarge = displayLarge.s(), displayMedium = displayMedium.s(), displaySmall = displaySmall.s(),
        headlineLarge = headlineLarge.s(), headlineMedium = headlineMedium.s(), headlineSmall = headlineSmall.s(),
        titleLarge = titleLarge.s(), titleMedium = titleMedium.s(), titleSmall = titleSmall.s(),
        bodyLarge = bodyLarge.s(), bodyMedium = bodyMedium.s(), bodySmall = bodySmall.s(),
        labelLarge = labelLarge.s(), labelMedium = labelMedium.s(), labelSmall = labelSmall.s(),
    )
}

private val LambdaShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(22),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34),
)

@Composable
fun LambdaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    lowStimulus: Boolean = true,
    highContrast: Boolean = false,
    reducedMotion: Boolean = true,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        highContrast && darkTheme -> HighContrastDark
        highContrast -> HighContrastLight
        darkTheme -> DarkColors
        else -> LightColors
    }
    val glass = GlassStyle(
        dark = darkTheme,
        highContrast = highContrast,
        reducedMotion = reducedMotion,
        lowStimulus = lowStimulus,
    )
    CompositionLocalProvider(LocalGlassStyle provides glass) {
        MaterialTheme(
            colorScheme = scheme,
            typography = LambdaTypography.scaled(fontScale.coerceIn(0.85f, 1.5f)),
            shapes = LambdaShapes,
            content = content,
        )
    }
}
