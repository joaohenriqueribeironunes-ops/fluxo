package com.equilibrio.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.equilibrio.app.ui.MainViewModel
import com.equilibrio.app.ui.components.ScreenScaffold
import com.equilibrio.core.model.CrisisType
import com.equilibrio.core.ui.glass.GlassButton
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassChip
import com.equilibrio.core.ui.glass.GlassTone
import com.equilibrio.core.ui.theme.CalmTeal
import com.equilibrio.core.ui.theme.LocalAccessibility

@Composable
fun CalmScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val accessibility = LocalAccessibility.current
    var showCrisis by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(CrisisType.SOBRECARGA_SENSORIAL) }
    var intensity by remember { mutableFloatStateOf(7f) }

    ScreenScaffold(
        title = "Pausa guiada",
        subtitle = "Escolha uma coisa. Só uma já ajuda.",
        onBack = onBack
    ) {
        GlassCard(tone = GlassTone.Calm) {
            Box(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                if (accessibility.reduceMotion) {
                    Box(
                        Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(CalmTeal.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Respire devagar.\nSem pressa.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    val transition = rememberInfiniteTransition(label = "breath")
                    val phase by transition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(10_000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "breath-phase"
                    )
                    Box(
                        Modifier
                            .size(170.dp)
                            .scale(0.78f + 0.22f * phase)
                            .clip(CircleShape)
                            .background(CalmTeal.copy(alpha = 0.20f))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (phase < 0.45f) "Inspire" else "Expire",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "4 s inspirando · 6 s expirando",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        GlassCard {
            Text(
                "Pausa sensorial",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Step("1", "Reduza a luz. Se puder, feche os olhos ou use um ambiente mais escuro.")
            Step("2", "Diminua o som. Fones de proteção, protetores auriculares ou um lugar mais quieto.")
            Step("3", "Solte o corpo. Ombros para baixo, mandíbula relaxada, pés apoiados no chão.")
            Step("4", "Dê permissão para stimmar. Movimento repetitivo e seguro ajuda a regular.")
            Step("5", "Beba água. Coma algo simples se a fome estiver alta.")
        }

        GlassCard {
            Text(
                "Ancoragem 5-4-3-2-1",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Diga para si, no seu tempo: 5 coisas que você vê, 4 que ouve, 3 que sente no corpo, " +
                    "2 que cheira e 1 que gosta no sabor. Não precisa ser perfeito.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
        }

        GlassCard(tone = GlassTone.Warm) {
            Text(
                "Se houver risco à segurança",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Ligue para alguém de confiança agora. Se houver pensamentos de se machucar, procure ajuda imediata:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassButton(
                    text = "CVV 188",
                    onClick = { dial(context, "188") },
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = "SAMU 192",
                    onClick = { dial(context, "192") },
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        GlassButton(
            text = "Registrar crise agora",
            onClick = { showCrisis = true },
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Você não precisa passar por isso sozinho. O app é uma ferramenta de apoio, " +
                "não um profissional de saúde.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
        )
    }

    if (showCrisis) {
        AlertDialog(
            onDismissRequest = { showCrisis = false },
            title = { Text("Registrar crise agora") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CrisisType.entries.forEach { type ->
                            GlassChip(
                                text = type.label,
                                selected = type == selectedType,
                                onClick = { selectedType = type }
                            )
                        }
                    }
                    Text("Intensidade: ${intensity.toInt()}/10", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = intensity,
                        onValueChange = { intensity = it },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.registerCrisis(selectedType, intensity.toInt())
                    showCrisis = false
                    onBack()
                }) { Text("Registrar") }
            },
            dismissButton = {
                TextButton(onClick = { showCrisis = false }) { Text("Agora não") }
            }
        )
    }
}

@Composable
private fun Step(number: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(CalmTeal.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.size(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

private fun dial(context: Context, number: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
    runCatching { context.startActivity(intent) }
}
