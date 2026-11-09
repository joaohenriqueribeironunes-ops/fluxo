package com.lambda.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lambda.app.BuildConfig
import com.lambda.coreui.design.GlassButton
import com.lambda.coreui.design.GlassCard
import com.lambda.coreui.design.GlassHeader
import com.lambda.coreui.design.GlassIconButton
import com.lambda.coreui.design.GlassPanel
import com.lambda.coreui.design.GlassSwitch
import com.lambda.coreui.design.SectionLabel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    a11y: AccessibilitySettings,
    prefs: AppPreferences,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        GlassHeader(
            title = "Ajustes",
            subtitle = "Conforto, previsibilidade e autonomia — tudo local.",
        )

        GlassPanel(contentPadding = PaddingValues(18.dp)) {
            SectionLabel("Acessibilidade neurodivergente")
            Spacer(Modifier.height(10.dp))
            SettingRow(
                title = "Baixo estímulo",
                detail = "Reduz brilho, saturação e efeitos do fundo.",
                checked = a11y.lowStimulus,
                onChange = { scope.launch { prefs.setLowStimulus(it) } },
            )
            SettingRow(
                title = "Alto contraste",
                detail = "Superfícies sólidas e bordas fortes, sem vidro.",
                checked = a11y.highContrast,
                onChange = { scope.launch { prefs.setHighContrast(it) } },
            )
            SettingRow(
                title = "Movimento reduzido",
                detail = "Sem animações de fundo, escala ou transição.",
                checked = a11y.reducedMotion,
                onChange = { scope.launch { prefs.setReducedMotion(it) } },
            )
            SettingRow(
                title = "Sem sons inesperados",
                detail = "A voz só fala quando você toca em “Ouvir”.",
                checked = a11y.noUnexpectedSounds,
                onChange = { scope.launch { prefs.setNoUnexpectedSounds(it) } },
            )
        }

        GlassPanel(contentPadding = PaddingValues(18.dp)) {
            SectionLabel("Tamanho do texto")
            Spacer(Modifier.height(6.dp))
            Text(
                "Ajuste fino de ${(a11y.fontScale * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(
                    icon = Icons.Rounded.Remove,
                    contentDescription = "Diminuir texto",
                    onClick = { scope.launch { prefs.setFontScale(a11y.fontScale - 0.05f) } },
                    size = 42.dp,
                )
                Slider(
                    value = a11y.fontScale,
                    onValueChange = { scope.launch { prefs.setFontScale(it) } },
                    valueRange = 0.85f..1.5f,
                    steps = 12,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                    ),
                )
                GlassIconButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = "Aumentar texto",
                    onClick = { scope.launch { prefs.setFontScale(a11y.fontScale + 0.05f) } },
                    size = 42.dp,
                )
            }
            Text(
                "O texto cresce em todo o app, sem quebrar a navegação.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
            )
        }

        GlassPanel(contentPadding = PaddingValues(18.dp)) {
            SectionLabel("Apoio e segurança")
            Spacer(Modifier.height(8.dp))
            Text(
                "O Lambda é apoio, não substitui profissional. Sem diagnóstico, sem dose de medicamento e sem plano alimentar prescritivo. ND não é “errado”; NT não é “vilão”. O foco é tradução e autonomia.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            )
            Spacer(Modifier.height(12.dp))
            GlassCard {
                Text("Risco imediato ou crise", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("CVV 188 — apoio emocional 24h, ligação gratuita.", style = MaterialTheme.typography.bodySmall)
                Text("SAMU 192 — emergência médica.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(
                        onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:188"))) },
                        accent = false,
                    ) { Text("Ligar 188") }
                    GlassButton(
                        onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:192"))) },
                        accent = false,
                    ) { Text("Ligar 192") }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Procure também pessoas de confiança. Contatos de confiança ficam no seu aparelho (em evolução).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
            )
        }

        LearnedSection()
        BackupSection()

        GlassPanel(contentPadding = PaddingValues(18.dp)) {
            SectionLabel("Privacidade")
            Spacer(Modifier.height(6.dp))
            Text(
                "Local por padrão, sem telemetria e sem internet. Nenhuma chave de API é usada. Seus compromissos e marca-textos ficam só neste aparelho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Fonte e confiança aparecem em cada resposta. O banco é curado por pessoas, com referências públicas — não gerado por IA.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Lambda ${BuildConfig.VERSION_NAME} · offline-first · PT-BR",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    detail: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            )
        }
        Spacer(Modifier.width(12.dp))
        GlassSwitch(checked = checked, onCheckedChange = onChange)
    }
}
