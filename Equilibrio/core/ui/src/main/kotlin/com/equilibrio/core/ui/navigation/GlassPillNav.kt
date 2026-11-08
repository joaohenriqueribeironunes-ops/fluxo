package com.equilibrio.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.equilibrio.core.ui.theme.LocalAccessibility
import com.equilibrio.core.ui.theme.LocalRiskScore
import com.equilibrio.core.ui.theme.riskPalette

data class GlassNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun GlassPillNav(
    items: List<GlassNavItem>,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    val accessibility = LocalAccessibility.current
    val palette = riskPalette(LocalRiskScore.current, dark)
    val shape = CircleShape
    val background: Brush = when {
        accessibility.reduceTransparency || accessibility.highContrast -> {
            val solid = if (dark) Color(0xFF142124) else Color(0xFFF8FCFB)
            Brush.verticalGradient(listOf(solid, solid))
        }
        dark -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.06f))
        )
        else -> Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.55f))
        )
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(18.dp, shape)
            .clip(shape)
            .background(background)
            .border(
                1.dp,
                if (dark) Color.White.copy(alpha = 0.16f)
                else Color.White.copy(alpha = 0.95f),
                shape
            )
            .padding(horizontal = 6.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(
                        if (selected) palette.accent.copy(alpha = if (accessibility.reduceTransparency) 0.9f else 0.20f)
                        else Color.Transparent
                    )
                    .clickable { onNavigate(item.route) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (selected) palette.accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.size(21.dp)
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}
