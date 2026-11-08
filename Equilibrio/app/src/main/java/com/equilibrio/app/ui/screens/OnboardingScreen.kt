package com.equilibrio.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.equilibrio.app.service.EquilibrioTileService
import com.equilibrio.app.ui.MainViewModel
import com.equilibrio.app.ui.components.GlassSliderRow
import com.equilibrio.app.ui.components.ScreenScaffold
import com.equilibrio.app.ui.components.SettingRow
import com.equilibrio.core.ui.components.PrivacyDisclaimer
import com.equilibrio.core.ui.glass.GlassButton
import com.equilibrio.core.ui.glass.GlassCard
import com.equilibrio.core.ui.glass.GlassTone
import com.equilibrio.core.ui.theme.CalmTeal

@Composable
fun OnboardingScreen(
    viewModel: MainViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var micEnabled by rememberSaveable { mutableStateOf(true) }
    var geminiConsent by rememberSaveable { mutableStateOf(false) }
    var permissionTick by remember { mutableIntStateOf(0) }
    var resumeTick by remember { mutableIntStateOf(0) }
    var batteryThreshold by rememberSaveable { mutableStateOf(15f) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val micGranted = remember(permissionTick, resumeTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }
    val notificationsGranted = remember(permissionTick, resumeTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
    val batteryExempt = remember(resumeTick) {
        val power = context.getSystemService(PowerManager::class.java)
        power?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionTick++
    }

    val steps = 4

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Equilíbrio",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(steps) { index ->
                    Box(
                        Modifier
                            .size(if (index == step) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(
                                if (index <= step) CalmTeal
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)
                            )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (step) {
                0 -> WelcomeStep()
                1 -> PermissionsStep(
                    micGranted = micGranted,
                    notificationsGranted = notificationsGranted,
                    onRequest = {
                        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            permissions += Manifest.permission.POST_NOTIFICATIONS
                        }
                        permissionLauncher.launch(permissions.toTypedArray())
                    }
                )
                2 -> BatteryStep(
                    batteryExempt = batteryExempt,
                    onOpenBattery = { openBatterySettings(context) },
                    onOpenAppDetails = { openAppDetails(context) },
                    onAddTile = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            EquilibrioTileService.requestAddTile(context)
                            Toast.makeText(context, "Confirme a adição do controle no painel do sistema.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Abra as Configurações rápidas e adicione o controle do Equilíbrio.", Toast.LENGTH_LONG).show()
                        }
                    },
                    threshold = batteryThreshold,
                    onThreshold = { batteryThreshold = it }
                )
                else -> BaselineStep(
                    micEnabled = micEnabled,
                    onMicEnabled = { micEnabled = it },
                    geminiConsent = geminiConsent,
                    onGeminiConsent = { geminiConsent = it },
                    geminiEnabled = settings.geminiEnabled,
                    onGeminiEnabled = {
                        viewModel.updateSettings { current -> current.copy(geminiEnabled = it) }
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (step > 0) {
                GlassButton(
                    text = "Voltar",
                    onClick = { step -= 1 },
                    primary = false,
                    modifier = Modifier.weight(1f)
                )
            }
            GlassButton(
                text = if (step == steps - 1) "Começar com cuidado" else "Continuar",
                onClick = {
                    if (step < steps - 1) {
                        step += 1
                    } else {
                        viewModel.updateSettings { current ->
                            current.copy(
                                micEnabled = micEnabled,
                                geminiConsent = geminiConsent,
                                batteryThreshold = batteryThreshold.toInt()
                            )
                        }
                        viewModel.completeOnboarding()
                        onDone()
                    }
                },
                modifier = Modifier.weight(if (step == 0) 1f else 1.2f)
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    GlassCard(tone = GlassTone.Calm) {
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Um espaço para entender seus padrões",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "O Equilíbrio observa sinais do dia a dia — movimento, luz, ruído e seus registros — " +
                "para estimar janelas de maior sobrecarga e sugerir pausas antes que a crise escale.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(14.dp))
        Bullet("Nada de áudio é salvo: o microfone processa em memória e descarta a gravação.")
        Bullet("Seus dados ficam no aparelho, em banco criptografado.")
        Bullet("A IA é opcional e recebe apenas resumos agregados, nunca áudio ou amostras.")
        Bullet("As estimativas são apoio ao autoconhecimento, não diagnóstico.")
    }
    PrivacyDisclaimer()
}

@Composable
private fun PermissionsStep(
    micGranted: Boolean,
    notificationsGranted: Boolean,
    onRequest: () -> Unit
) {
    GlassCard {
        Text(
            "Permissões essenciais",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "O microfone é usado só para medir volume, bandas de frequência e impulsividade. " +
                "O indicador fica visível na notificação enquanto a janela de áudio acontece.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(14.dp))
        PermissionLine("Microfone", micGranted)
        Spacer(Modifier.height(8.dp))
        PermissionLine("Notificações", notificationsGranted)
        Spacer(Modifier.height(14.dp))
        GlassButton(
            text = if (micGranted && notificationsGranted) "Permissões concedidas" else "Conceder permissões",
            onClick = onRequest,
            enabled = !(micGranted && notificationsGranted),
            modifier = Modifier.fillMaxWidth()
        )
    }
    GlassCard(tone = GlassTone.Neutral) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Mic, null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.size(10.dp))
            Text(
                "Você pode desligar o microfone depois, sem perder o resto do app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PermissionLine(label: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (granted) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.tertiary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.size(10.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        Text(
            if (granted) "Concedida" else "Pendente",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun BatteryStep(
    batteryExempt: Boolean,
    onOpenBattery: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onAddTile: () -> Unit,
    threshold: Float,
    onThreshold: (Float) -> Unit
) {
    GlassCard(tone = GlassTone.Warm) {
        Icon(
            Icons.Outlined.BatterySaver,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Bateria no MIUI / HyperOS",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Para a coleta passiva não parar quando a tela apaga, o sistema precisa deixar o Equilíbrio " +
                "trabalhar em segundo plano. Isso não impede você de encerrar a coleta a qualquer momento.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
        )
        Spacer(Modifier.height(12.dp))
        Bullet("Configurações → Apps → Gerenciar apps → Equilíbrio → Autostart: ativar.")
        Bullet("Economia de bateria → Sem restrições.")
        Bullet("No menu de apps recentes, toque no cadeado para fixar o app.")
        Spacer(Modifier.height(12.dp))
        GlassButton(
            text = if (batteryExempt) "Isenção de bateria concedida" else "Abrir opções de bateria",
            onClick = onOpenBattery,
            enabled = !batteryExempt,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        GlassButton(
            text = "Abrir dados do app (MIUI)",
            onClick = onOpenAppDetails,
            primary = false,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        GlassButton(
            text = "Adicionar controle no painel rápido",
            onClick = onAddTile,
            primary = false,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Permite ligar/desligar a coleta direto do painel de controle, sem abrir o app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
    GlassCard {
        GlassSliderRow(
            title = "Pausar coleta com bateria baixa",
            valueLabel = "${threshold.toInt()}%",
            value = threshold,
            onValueChange = onThreshold,
            valueRange = 5f..30f,
            steps = 4,
            description = "Abaixo disso, o app pausa sozinho e avisa quando você conectar o carregador."
        )
    }
}

@Composable
private fun BaselineStep(
    micEnabled: Boolean,
    onMicEnabled: (Boolean) -> Unit,
    geminiConsent: Boolean,
    onGeminiConsent: (Boolean) -> Unit,
    geminiEnabled: Boolean,
    onGeminiEnabled: (Boolean) -> Unit
) {
    GlassCard(tone = GlassTone.Calm) {
        Icon(
            Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "14 dias de aprendizado",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Nas duas primeiras semanas o app só observa e monta o seu baseline pessoal. " +
                "Nenhum alerta de risco é disparado antes disso — assim as estimativas têm com o que comparar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
        )
    }
    GlassCard {
        SettingRow(
            title = "Microfone na coleta passiva",
            description = "Mede ruído e agudos; o áudio é descartado após o processamento.",
            checked = micEnabled,
            onCheckedChange = onMicEnabled
        )
        SettingRow(
            title = "Análise com Gemini",
            description = "Explicações e padrões a partir de resumos agregados. Opcional.",
            checked = geminiEnabled,
            onCheckedChange = onGeminiEnabled
        )
        SettingRow(
            title = "Autorizo o envio de resumos agregados",
            description = "Posso revogar quando quiser. Nunca envio áudio, amostras ou texto bruto de sensores.",
            checked = geminiConsent,
            onCheckedChange = onGeminiConsent,
            enabled = geminiEnabled
        )
    }
    PrivacyDisclaimer(compact = true)
}

@Composable
private fun Bullet(text: String) {
    Row {
        Text(
            "•",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
        )
    }
}

private fun openBatterySettings(context: Context) {
    val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
    }
    runCatching { context.startActivity(request) }.onFailure {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}

private fun openAppDetails(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}
