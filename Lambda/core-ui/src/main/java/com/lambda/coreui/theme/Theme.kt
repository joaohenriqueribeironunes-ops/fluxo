package com.lambda.coreui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F6B57),
    secondary = Color(0xFF7A6FA8),
    tertiary = Color(0xFFB07D4A),
    background = Color(0xFFF7F4EF),
    surface = Color(0xFFFFFBF7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA8CDB2),
    secondary = Color(0xFFC8B6FF),
    tertiary = Color(0xFFF0C89B),
    background = Color(0xFF141915),
    surface = Color(0xFF1C231E),
)

private val HighContrastLight = lightColorScheme(
    primary = Color(0xFF00391F), onPrimary = Color.White,
    background = Color(0xFFFFFFFF), onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE6E6E6), onSurfaceVariant = Color(0xFF000000),
)

private val HighContrastDark = darkColorScheme(
    primary = Color(0xFFBFF0C8), onPrimary = Color(0xFF000000),
    background = Color(0xFF000000), onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000), onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF222222), onSurfaceVariant = Color(0xFFFFFFFF),
)

@Composable
fun LambdaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    lowStimulus: Boolean = true,
    highContrast: Boolean = false,
    reducedMotion: Boolean = true,
    fontScale: Float = 1.0f,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        highContrast && darkTheme -> HighContrastDark
        highContrast -> HighContrastLight
        !lowStimulus && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    val base = MaterialTheme.typography
    val typography = if (fontScale == 1.0f) base else androidx.compose.material3.Typography(
        headlineSmall = base.headlineSmall.copy(fontSize = base.headlineSmall.fontSize * fontScale),
        titleLarge = base.titleLarge.copy(fontSize = base.titleLarge.fontSize * fontScale),
        titleMedium = base.titleMedium.copy(fontSize = base.titleMedium.fontSize * fontScale),
        titleSmall = base.titleSmall.copy(fontSize = base.titleSmall.fontSize * fontScale),
        bodyLarge = base.bodyLarge.copy(fontSize = base.bodyLarge.fontSize * fontScale),
        bodyMedium = base.bodyMedium.copy(fontSize = base.bodyMedium.fontSize * fontScale),
        bodySmall = base.bodySmall.copy(fontSize = base.bodySmall.fontSize * fontScale),
        labelLarge = base.labelLarge.copy(fontSize = base.labelLarge.fontSize * fontScale),
        labelMedium = base.labelMedium.copy(fontSize = base.labelMedium.fontSize * fontScale),
        labelSmall = base.labelSmall.copy(fontSize = base.labelSmall.fontSize * fontScale),
    )
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
