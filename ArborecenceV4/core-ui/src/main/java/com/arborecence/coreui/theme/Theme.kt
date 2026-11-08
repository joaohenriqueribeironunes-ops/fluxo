package com.arborecence.coreui.theme

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

@Composable
fun ArborecenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    lowStimulus: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, content = content)
}
