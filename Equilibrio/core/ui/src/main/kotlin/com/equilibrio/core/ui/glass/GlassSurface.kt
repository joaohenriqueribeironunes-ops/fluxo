package com.equilibrio.core.ui.glass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.scale
import com.equilibrio.core.ui.theme.IosGreen
import com.equilibrio.core.ui.theme.LocalAccessibility
import com.equilibrio.core.ui.theme.LocalRiskScore
import com.equilibrio.core.ui.theme.riskPalette

enum class GlassTone {
    Neutral,
    Calm,
    Warm,
    Alert
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tone: GlassTone = GlassTone.Neutral,
    padding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    val accessibility = LocalAccessibility.current
    val palette = riskPalette(LocalRiskScore.current, dark)
    val tint = when (tone) {
        GlassTone.Neutral -> palette.accent
        GlassTone.Calm -> palette.calm
        GlassTone.Warm -> palette.moderate
        GlassTone.Alert -> palette.high
    }
    val solid = when {
        accessibility.highContrast && dark -> Color(0xFF162326)
        accessibility.highContrast -> Color(0xFFFFFFFF)
        accessibility.reduceTransparency && dark -> Color(0xFF142124)
        accessibility.reduceTransparency -> Color(0xFFF7FBFA)
        else -> Color.Transparent
    }
    val backgroundBrush = if (accessibility.reduceTransparency || accessibility.highContrast) {
        SolidColor(solid)
    } else if (dark) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.05f))
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.62f), Color.White.copy(alpha = 0.36f))
        )
    }
    val borderBrush = when {
        accessibility.highContrast -> SolidColor(MaterialTheme.colorScheme.outline)
        dark -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.06f))
        )
        else -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.30f))
        )
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !accessibility.reduceMotion) 0.985f else 1f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.7f),
        label = "glass-press"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = if (accessibility.reduceTransparency) 1.dp
                else if (dark) 18.dp else 12.dp,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(backgroundBrush)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .border(
                width = if (accessibility.highContrast) 1.5.dp else 1.dp,
                brush = borderBrush,
                shape = shape
            )
    ) {
        if (!accessibility.reduceTransparency && !accessibility.highContrast) {
            Box(
                Modifier
                    .matchParentSize()
                    .clip(shape)
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(26.dp)
                        } else {
                            Modifier
                        }
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(tint.copy(alpha = 0.30f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset.Zero,
                            radius = 520f
                        )
                    )
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = if (dark) 0.08f else 0.20f),
                                Color.Transparent,
                                Color.White.copy(alpha = 0.02f)
                            )
                        )
                    )
            )
        }
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val dark = isSystemInDarkTheme()
    val accessibility = LocalAccessibility.current
    val palette = riskPalette(LocalRiskScore.current, dark)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !accessibility.reduceMotion) 0.97f else 1f,
        animationSpec = spring(stiffness = 520f, dampingRatio = 0.65f),
        label = "button-press"
    )
    val haptics = LocalHapticFeedback.current
    val radius = RoundedCornerShape(22.dp)

    val bg = when {
        !enabled -> SolidColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        primary -> Brush.linearGradient(
            listOf(palette.accent, palette.accent.copy(alpha = 0.82f))
        )
        accessibility.reduceTransparency || accessibility.highContrast ->
            SolidColor(MaterialTheme.colorScheme.surfaceVariant)
        dark -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.06f))
        )
        else -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.72f), Color.White.copy(alpha = 0.42f))
        )
    }
    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        primary -> palette.onAccent
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(54.dp)
            .scale(scale)
            .clip(radius)
            .background(bg)
            .border(
                1.dp,
                if (primary) Color.White.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                radius
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val palette = riskPalette(LocalRiskScore.current, dark)
    val accessibility = LocalAccessibility.current
    val shape = RoundedCornerShape(50)
    val background = when {
        selected -> palette.accent.copy(alpha = if (accessibility.reduceTransparency) 1f else 0.24f)
        accessibility.reduceTransparency -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.White.copy(alpha = if (dark) 0.07f else 0.55f)
    }
    val border = when {
        selected -> palette.accent.copy(alpha = 0.75f)
        accessibility.highContrast -> MaterialTheme.colorScheme.outline
        else -> Color.White.copy(alpha = if (dark) 0.16f else 0.8f)
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
        )
    }
}

@Composable
fun GlassToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val accessibility = LocalAccessibility.current
    val offset by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = if (accessibility.reduceMotion) spring(stiffness = 2000f)
        else spring(dampingRatio = 0.72f, stiffness = 700f),
        label = "toggle"
    )
    val trackColor = if (checked) IosGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .width(52.dp)
            .height(32.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            Modifier
                .offset(x = offset.dp)
                .size(26.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
