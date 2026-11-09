package com.lambda.coreui.design

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Linguagem visual do Lambda: vidro líquido (iOS 26) + profundidade espacial (visionOS).
 * Tudo respeita baixo estímulo, alto contraste e movimento reduzido.
 */
@Immutable
data class GlassStyle(
    val dark: Boolean,
    val highContrast: Boolean,
    val reducedMotion: Boolean,
    val lowStimulus: Boolean,
)

val LocalGlassStyle = compositionLocalOf { GlassStyle(false, false, true, true) }

val GlassShape = RoundedCornerShape(30.dp)
val GlassShapeSmall = RoundedCornerShape(20.dp)
val GlassShapePill = RoundedCornerShape(50)

@Composable
private fun glassTop(style: GlassStyle): Color = when {
    style.highContrast -> MaterialTheme.colorScheme.surface
    style.dark -> Color.White.copy(alpha = if (style.lowStimulus) 0.10f else 0.14f)
    else -> Color.White.copy(alpha = if (style.lowStimulus) 0.62f else 0.72f)
}

@Composable
private fun glassBottom(style: GlassStyle): Color = when {
    style.highContrast -> MaterialTheme.colorScheme.surface
    style.dark -> Color.White.copy(alpha = 0.045f)
    else -> Color.White.copy(alpha = 0.30f)
}

@Composable
private fun glassBorder(style: GlassStyle): Brush = when {
    style.highContrast -> Brush.linearGradient(
        listOf(MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurface)
    )
    style.dark -> Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.08f))
    )
    else -> Brush.linearGradient(
        listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.35f))
    )
}

/** Fundo aurora profundo, com vidro atrás de tudo. */
@Composable
fun AuroraBackdrop(modifier: Modifier = Modifier, style: GlassStyle) {
    val motion = !style.reducedMotion && !style.lowStimulus
    val drift = if (motion) {
        val t = rememberInfiniteTransition(label = "aurora")
        t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(52_000)),
            label = "drift",
        ).value
    } else 0f

    val base = if (style.dark) Color(0xFF05080F) else Color(0xFFEDF3F9)
    Box(modifier.fillMaxSize().background(base)) {
        if (!style.highContrast) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = if (style.lowStimulus) 0.55f else 0.85f }) {
                val w = size.width
                val h = size.height
                val shift = (drift - 0.5f) * w * 0.18f
                // azul profundo
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF2A6BFF).copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(w * 0.18f + shift, h * 0.16f),
                        radius = w * 0.85f,
                    ),
                    radius = w * 0.85f,
                    center = Offset(w * 0.18f + shift, h * 0.16f),
                )
                // turquesa
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF00C9B7).copy(alpha = 0.42f), Color.Transparent),
                        center = Offset(w * 0.92f - shift, h * 0.38f),
                        radius = w * 0.75f,
                    ),
                    radius = w * 0.75f,
                    center = Offset(w * 0.92f - shift, h * 0.38f),
                )
                // brasa/âmbar suave (só quando não é baixo estímulo)
                if (!style.lowStimulus) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFFF8A3D).copy(alpha = 0.30f), Color.Transparent),
                            center = Offset(w * 0.55f, h * 0.92f + shift),
                            radius = w * 0.8f,
                        ),
                        radius = w * 0.8f,
                        center = Offset(w * 0.55f, h * 0.92f + shift),
                    )
                }
                // vinheta para dar profundidade
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent, Color.Black.copy(alpha = 0.22f))
                    )
                )
            }
        }
    }
}

/** Superfície de vidro com borda especular e sombra suave. */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShape,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    elevated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = LocalGlassStyle.current
    val shadow = if (elevated && !style.highContrast) 20.dp else 0.dp
    Box(
        modifier = modifier
            .shadow(
                elevation = shadow,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.40f),
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(glassTop(style), glassBottom(style))))
            .border(BorderStroke(if (style.highContrast) 2.dp else 1.dp, glassBorder(style)), shape)
    ) {
        // brilho especular no topo (vidro líquido)
        if (!style.highContrast) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = if (style.dark) 0.55f else 0.95f), Color.Transparent)
                        )
                    )
            )
        }
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapeSmall,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) = GlassPanel(modifier = modifier, shape = shape, contentPadding = contentPadding, content = content)

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val style = LocalGlassStyle.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !style.reducedMotion) 0.97f else 1f,
        animationSpec = tween(120),
        label = "press",
    )
    val shape = RoundedCornerShape(22.dp)
    val bg = when {
        !enabled -> Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.03f)))
        accent -> Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.95f), MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
        )
        else -> Brush.verticalGradient(listOf(glassTop(style), glassBottom(style)))
    }
    val fg = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        accent -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (accent && enabled) 16.dp else 0.dp, shape, ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
            .clip(shape)
            .background(bg)
            .border(1.dp, if (accent) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.12f))) else glassBorder(style), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 15.dp)
            .semantics { role = Role.Button },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Botão circular de vidro (visionOS). */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val style = LocalGlassStyle.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && !style.reducedMotion) 0.92f else 1f, tween(120), label = "icon")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(size)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(glassTop(style), glassBottom(style))))
            .border(1.dp, glassBorder(style), CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.46f)) }
}

/** Chip/pílula de vidro, com seleção em azul profundo. */
@Composable
fun GlassPill(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    val style = LocalGlassStyle.current
    val shape = CircleShape
    val bg = when {
        selected -> Brush.verticalGradient(
            listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.96f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.86f))
        )
        else -> Brush.verticalGradient(listOf(glassTop(style), glassBottom(style)))
    }
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, if (selected) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.16f))) else glassBorder(style), shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true,
) {
    val style = LocalGlassStyle.current
    val shape = RoundedCornerShape(20.dp)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        singleLine = singleLine,
        minLines = minLines,
        shape = shape,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = if (style.dark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.55f),
            unfocusedContainerColor = if (style.dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.45f),
            disabledContainerColor = Color.Transparent,
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
            unfocusedBorderColor = if (style.dark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.75f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

/** Interruptor de vidro, sem piscadas. */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = LocalGlassStyle.current
    val track = if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
    else if (style.dark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.12f)
    val thumbX by animateFloatAsState(
        targetValue = if (checked) 24f else 2f,
        animationSpec = if (style.reducedMotion) tween(0) else tween(180),
        label = "switch",
    )
    Box(
        modifier
            .width(50.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(track)
            .border(1.dp, if (style.dark) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.8f), CircleShape)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .graphicsLayer { translationX = thumbX * density }
                .size(24.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
fun GlassBadge(text: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    val style = LocalGlassStyle.current
    val shape = CircleShape
    Box(
        modifier
            .clip(shape)
            .background(
                if (accent) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                else if (style.dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.55f)
            )
            .border(1.dp, if (accent) Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.40f), MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))) else glassBorder(style), shape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            maxLines = 1,
        )
    }
}

@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    val style = LocalGlassStyle.current
    Box(
        modifier.fillMaxWidth().height(1.dp).background(
            Brush.horizontalGradient(
                listOf(Color.Transparent, if (style.dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f), Color.Transparent)
            )
        )
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        fontWeight = FontWeight.SemiBold,
    )
}

/** Dock flutuante de vidro (visionOS), com seleção em cápsula. */
@Composable
fun GlassDock(
    items: List<DockItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = LocalGlassStyle.current
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .fillMaxWidth()
            .shadow(26.dp, RoundedCornerShape(34.dp), ambientColor = Color.Black.copy(alpha = 0.45f), spotColor = Color.Black.copy(alpha = 0.55f))
            .clip(RoundedCornerShape(34.dp))
            .background(Brush.verticalGradient(listOf(glassTop(style).copy(alpha = 0.94f), glassBottom(style).copy(alpha = 0.90f))))
            .border(1.dp, glassBorder(style), RoundedCornerShape(34.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            val active = index == selected
            val tint by animateColorAsState(
                targetValue = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                animationSpec = if (style.reducedMotion) tween(0) else tween(180),
                label = "dockTint",
            )
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .then(
                        if (active) Modifier.background(
                            Brush.verticalGradient(
                                listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f))
                            )
                        ) else Modifier
                    )
                    .clickable(role = Role.Tab) { onSelect(index) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Icon(item.icon, item.label, tint = tint, modifier = Modifier.size(22.dp))
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    maxLines = 1,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Immutable
data class DockItem(val label: String, val icon: ImageVector)

/** Cabeçalho grande, com respiro espacial. */
@Composable
fun GlassHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}
