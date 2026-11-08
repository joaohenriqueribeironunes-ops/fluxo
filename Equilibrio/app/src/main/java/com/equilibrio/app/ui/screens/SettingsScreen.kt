package com.equilibrio.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import com.equilibrio.app.ui.MainViewModel
import com.equilibrio.app.ui.components.ConfirmDialog
import com.equilibrio.app.ui.components.GlassSliderRow
import com.equilibrio.app.ui.components.ScreenScaffold
import com.equilibrio.app.ui.components.SettingRow
import com.equilibrio.core.ui.glass.GlassButton
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassTone
import com.equilibrio.core.ui.theme.CalmTeal
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onOpenReport: () -> Unit,
    onOpenAccuracy: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var proxyUrl by remember { mutableStateOf(settings.geminiProxyUrl) }
    var apiKey by remember { mutableStateOf("") }
    var hasKey by remember { mutableStateOf(viewModel.hasGeminiKey()) }
    var confirmWipe by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "Configurações",
        subtitle = "Você no controle do que é coletado",
        onBack = onBack
    ) {
        GlassCard {
            Text("Coleta passiva", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            SettingRow(
                title = "Coleta ativa",
                description = "Sensores e microfone em ciclos de 30 s, com pausa entre janelas.",
                checked = settings.collectionEnabled,
                onCheckedChange = { viewModel.toggleCollection(it) }
            )
            SettingRow(
                title = "Usar microfone",
                description = "Volume, bandas e impulsividade; o áudio é processado e descartado.",
                checked = settings.micEnabled,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(micEnabled = value) }
                }
            )
            GlassSliderRow(
                title = "Duração do ciclo",
                valueLabel = "${settings.cycleSeconds} s",
                value = settings.cycleSeconds.toFloat(),
                onValueChange = { value ->
                    val rounded = (value / 15f).roundToInt() * 15
                    viewModel.updateSettings { it.copy(cycleSeconds = rounded) }
                },
                valueRange = 30f..120f,
                steps = 5,
                description = "30 s = 15 s de coleta + 15 s de pausa."
            )
            GlassSliderRow(
                title = "Janela de coleta",
                valueLabel = "${settings.windowSeconds} s",
                value = settings.windowSeconds.toFloat(),
                onValueChange = { value ->
                    val rounded = (value / 5f).roundToInt() * 5
                    viewModel.updateSettings { it.copy(windowSeconds = rounded) }
                },
                valueRange = 5f..30f,
                steps = 4
            )
            GlassSliderRow(
                title = "Pausar com bateria abaixo de",
                valueLabel = "${settings.batteryThreshold}%",
                value = settings.batteryThreshold.toFloat(),
                onValueChange = { value ->
                    viewModel.updateSettings { it.copy(batteryThreshold = value.toInt()) }
                },
                valueRange = 5f..30f,
                steps = 4
            )
        }

        GlassCard {
            Text("Alertas preventivos", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            GlassSliderRow(
                title = "Limiar de alerta",
                valueLabel = "${settings.alertThreshold}/100",
                value = settings.alertThreshold.toFloat(),
                onValueChange = { value ->
                    viewModel.updateSettings { it.copy(alertThreshold = value.toInt()) }
                },
                valueRange = 40f..90f,
                steps = 9,
                description = "Abaixo de 70 o app pode avisar com mais frequência; acima, só em risco mais alto."
            )
            GlassSliderRow(
                title = "Máximo de alertas por dia",
                valueLabel = settings.maxAlertsPerDay.toString(),
                value = settings.maxAlertsPerDay.toFloat(),
                onValueChange = { value ->
                    viewModel.updateSettings { it.copy(maxAlertsPerDay = value.toInt()) }
                },
                valueRange = 1f..6f,
                steps = 4
            )
            SettingRow(
                title = "Modo silencioso",
                description = "Mantém as previsões na tela, mas não envia notificações de risco.",
                checked = settings.quietPredictions,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(quietPredictions = value) }
                }
            )
            SettingRow(
                title = "Lembretes de check-in",
                description = "Um convite gentil, com intervalo mínimo de 1 h.",
                checked = settings.checkInReminders,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(checkInReminders = value) }
                }
            )
            GlassSliderRow(
                title = "Intervalo dos lembretes",
                valueLabel = "${settings.reminderIntervalMinutes / 60} h",
                value = settings.reminderIntervalMinutes.toFloat(),
                onValueChange = { value ->
                    val rounded = (value / 60f).roundToInt() * 60
                    viewModel.updateSettings { it.copy(reminderIntervalMinutes = rounded) }
                },
                valueRange = 60f..480f,
                steps = 6,
                enabled = settings.checkInReminders
            )
        }

        GlassCard {
            Text("Gemini (opcional)", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            SettingRow(
                title = "Análise com IA",
                description = "Explicações e padrões a partir de resumos agregados.",
                checked = settings.geminiEnabled,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(geminiEnabled = value) }
                }
            )
            SettingRow(
                title = "Consentimento de envio",
                description = "Você pode revogar a qualquer momento. Sem isso, nada é enviado.",
                checked = settings.geminiConsent,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(geminiConsent = value) }
                },
                enabled = settings.geminiEnabled
            )
            SettingRow(
                title = "Usar proxy recomendado",
                description = "O APK não contém chave. O proxy Cloudflare guarda a chave no servidor.",
                checked = settings.geminiUseProxy,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(geminiUseProxy = value) }
                },
                enabled = settings.geminiEnabled
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = proxyUrl,
                onValueChange = { proxyUrl = it },
                label = { Text("URL do proxy (HTTPS)") },
                placeholder = { Text("https://seu-worker.workers.dev") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            GlassButton(
                text = "Salvar URL do proxy",
                onClick = {
                    viewModel.updateSettings { it.copy(geminiProxyUrl = proxyUrl.trim()) }
                },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("Chave do Gemini (modo direto, opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    text = "Salvar chave no Keystore",
                    onClick = {
                        viewModel.saveGeminiApiKey(apiKey)
                        apiKey = ""
                        hasKey = viewModel.hasGeminiKey()
                    },
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
                GlassButton(
                    text = if (hasKey) "Remover" else "Sem chave",
                    onClick = {
                        viewModel.saveGeminiApiKey("")
                        hasKey = false
                    },
                    primary = false,
                    enabled = hasKey,
                    modifier = Modifier.weight(0.6f)
                )
            }
            Text(
                text = "A chave nunca entra no APK. No modo direto ela fica cifrada no Android Keystore, " +
                    "mas ainda sai do aparelho para a API do Google. O proxy é o caminho recomendado.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(8.dp))
            GlassButton(
                text = "Pedir análise agora",
                onClick = { viewModel.requestGeminiNow() },
                enabled = settings.geminiEnabled && settings.geminiConsent,
                modifier = Modifier.fillMaxWidth()
            )
        }

        GlassCard {
            Text("Calendário", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            SettingRow(
                title = "Permitir importação",
                description = "Somente título e horário; descrições e participantes são descartados.",
                checked = settings.calendarConsent,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(calendarConsent = value) }
                }
            )
            GlassButton(
                text = "Importar próximos compromissos",
                onClick = { viewModel.importCalendar() },
                primary = false,
                enabled = settings.calendarConsent,
                modifier = Modifier.fillMaxWidth()
            )
        }

        GlassCard {
            Text("Acessibilidade", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            SettingRow(
                title = "Reduzir movimento",
                description = "Desliga animações de fundo, pulsos e transições.",
                checked = settings.reduceMotion,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(reduceMotion = value) }
                }
            )
            SettingRow(
                title = "Reduzir transparência",
                description = "Superfícies mais sólidas, com contraste maior.",
                checked = settings.reduceTransparency,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(reduceTransparency = value) }
                }
            )
            SettingRow(
                title = "Alto contraste",
                description = "Texto e bordas com contraste reforçado (WCAG AA).",
                checked = settings.highContrast,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(highContrast = value) }
                }
            )
            GlassSliderRow(
                title = "Tamanho do texto",
                valueLabel = "${(settings.fontScale * 100).roundToInt()}%",
                value = settings.fontScale,
                onValueChange = { value ->
                    val rounded = (value * 20f).roundToInt() / 20f
                    viewModel.updateSettings { it.copy(fontScale = rounded.coerceIn(0.85f, 1.6f)) }
                },
                valueRange = 0.85f..1.6f,
                steps = 4
            )
            SettingRow(
                title = "Tema escuro",
                description = "O app também respeita o tema do sistema na primeira abertura.",
                checked = settings.darkTheme,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(darkTheme = value) }
                }
            )
        }

        GlassCard {
            Text("Privacidade", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            SettingRow(
                title = "Proteger captura de tela",
                description = "Ativa FLAG_SECURE e esconde o conteúdo nos apps recentes.",
                checked = settings.secureScreen,
                onCheckedChange = { value ->
                    viewModel.updateSettings { it.copy(secureScreen = value) }
                }
            )
            GlassButton(
                text = "Exportar meus dados (JSON)",
                onClick = { viewModel.exportJson() },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            GlassButton(
                text = "Ver relatório",
                onClick = onOpenReport,
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
            GlassButton(
                text = "Ver precisão do app",
                onClick = onOpenAccuracy,
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            GlassButton(
                text = "Apagar todos os dados",
                onClick = { confirmWipe = true },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "O banco é cifrado com SQLCipher. A passphrase fica no Android Keystore. " +
                    "Apagar remove tabelas, configurações e exportações; a chave do banco é mantida para o app continuar funcional.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        GlassCard(tone = GlassTone.Neutral) {
            val context = LocalContext.current
            Text("Diagnóstico", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Se o app fechar inesperadamente, um log local é gravado para ajudar a corrigir o problema. Ele fica só neste aparelho.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(8.dp))
            GlassButton(
                text = "Exportar log de erros",
                onClick = {
                    val intent = viewModel.crashLogIntent()
                    if (intent != null) {
                        context.startActivity(Intent.createChooser(intent, "Exportar log de erros"))
                    }
                },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            GlassButton(
                text = "Apagar log de erros",
                onClick = { viewModel.clearCrashLog() },
                primary = false,
                modifier = Modifier.fillMaxWidth()
            )
        }

        GlassCard(tone = GlassTone.Calm) {
            Text("Sobre", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Equilíbrio 1.0.2 · ferramenta de autoconhecimento (análise de padrões, sem diagnóstico).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Não substitui avaliação, diagnóstico, medicação ou acompanhamento profissional. " +
                    "Em crise com risco à segurança, procure alguém de confiança, ligue 188 (CVV) ou 192 (SAMU).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }

    if (confirmWipe) {
        ConfirmDialog(
            title = "Apagar todos os dados?",
            body = "Tabelas, configurações, análises e exportações serão removidas. " +
                "Essa ação não pode ser desfeita. A coleta será encerrada.",
            confirmLabel = "Apagar tudo",
            onConfirm = {
                viewModel.wipeAllData()
                confirmWipe = false
            },
            onDismiss = { confirmWipe = false }
        )
    }
}
