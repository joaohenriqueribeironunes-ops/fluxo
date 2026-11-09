package com.arborecence.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arborecence.app.ui.AccessibilitySettings
import com.arborecence.app.ui.AppPreferences
import com.arborecence.app.ui.BehaviorScreen
import com.arborecence.app.ui.BehaviorViewModel
import com.arborecence.app.util.TtsHelper
import com.arborecence.coreui.theme.ArborecenceTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var keyStore: GeminiKeyStore
    @Inject lateinit var tester: GeminiConnectionTester
    @Inject lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val a11y by prefs.settings.collectAsState(initial = AccessibilitySettings())
            ArborecenceTheme(
                lowStimulus = a11y.lowStimulus,
                highContrast = a11y.highContrast,
                reducedMotion = a11y.reducedMotion,
                fontScale = a11y.fontScale,
            ) {
                var showConnect by rememberSaveable { mutableStateOf(false) }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (showConnect) {
                        ConnectGeminiScreen(
                            initialKey = keyStore.getKey().orEmpty(),
                            tester = tester,
                            onSave = { key -> keyStore.saveKey(key) },
                            onRevoke = { keyStore.revokeKey(); showConnect = true },
                            onContinue = { showConnect = false },
                        )
                    } else {
                        AppShell(onOpenGemini = { showConnect = true }, prefs = prefs, a11y = a11y)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppShell(onOpenGemini: () -> Unit, prefs: AppPreferences, a11y: AccessibilitySettings) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Tradutor", "Estudos", "Ajustes")
    val ctx = LocalContext.current
    val tts = remember { TtsHelper(ctx) }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }
    val speak: (String) -> Unit = { text -> tts.speak(text, enabled = !a11y.noUnexpectedSounds) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(when (i) { 0 -> Icons.Filled.Favorite; 1 -> Icons.Filled.Book; else -> Icons.Filled.Settings }, null) },
                        label = { Text(label) },
                    )
                }
            }
        }
    ) { pad ->
        when (tab) {
            0 -> { val vm: BehaviorViewModel = hiltViewModel(); BehaviorScreen(vm, Modifier.padding(pad), onSpeak = speak) }
            1 -> { val vm: com.arborecence.app.ui.StudyViewModel = hiltViewModel(); com.arborecence.app.ui.StudyScreen(vm, Modifier.padding(pad)) }
            else -> SettingsScreen(Modifier.padding(pad), onOpenGemini = onOpenGemini, prefs = prefs, a11y = a11y)
        }
    }
}

@Composable
private fun StudiesPlaceholder(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Central de estudos (offline)", style = MaterialTheme.typography.headlineSmall)
        Text("Busca e navegação em áreas, subáreas, conceitos, frases explicativas e etimologia (quando os bancos locais estiverem populados).", style = MaterialTheme.typography.bodyMedium)
        Text("Recursos em evolução: marca-texto (lavanda/menta/âmbar/água), revisão espaçada, mapa de conexões, linhas do tempo, modo hiperfoco/foco curto, checklists de início de tarefa.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier = Modifier, onOpenGemini: () -> Unit, prefs: AppPreferences, a11y: AccessibilitySettings) {
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall)
        Text("Modo offline ativo: o app não depende de API para funcionar. Gemini é opcional.", style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onOpenGemini) { Text("Conectar / trocar / revogar Gemini (opcional)") }

        Text("Acessibilidade neurodivergente", style = MaterialTheme.typography.titleSmall)
        A11yToggle("Baixo estímulo (reduz efeitos visuais)", a11y.lowStimulus) { scope.launch { prefs.setLowStimulus(it) } }
        A11yToggle("Alto contraste", a11y.highContrast) { scope.launch { prefs.setHighContrast(it) } }
        A11yToggle("Movimento reduzido (sem animações desnecessárias)", a11y.reducedMotion) { scope.launch { prefs.setReducedMotion(it) } }
        A11yToggle("Sem sons inesperados", a11y.noUnexpectedSounds) { scope.launch { prefs.setNoUnexpectedSounds(it) } }
        Text("Tamanho da fonte: ${"%.2f".format(a11y.fontScale)}x", style = MaterialTheme.typography.bodySmall)
        androidx.compose.material3.Slider(
            value = a11y.fontScale,
            onValueChange = { scope.launch { prefs.setFontScale(it) } },
            valueRange = 0.85f..1.5f,
        )
        Text("Navegação previsível e sem piscadas: mantida por padrão.", style = MaterialTheme.typography.bodySmall)

        Text("Apoio e segurança", style = MaterialTheme.typography.titleSmall)
        Text("Este app é apoio, não substitui profissional. Sem diagnóstico, sem dose de medicamento, sem plano alimentar prescritivo. ND não é “errado” e NT não é “vilão”: foco em tradução e autonomia.", style = MaterialTheme.typography.bodySmall)
        androidx.compose.material3.ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Se houver risco imediato ou crise", style = MaterialTheme.typography.titleSmall)
                Text("CVV 188 — apoio emocional 24h (ligação gratuita).", style = MaterialTheme.typography.bodyMedium)
                Text("SAMU 192 — emergência médica.", style = MaterialTheme.typography.bodyMedium)
                Text("Procure também pessoas de confiança. (Configuração de contatos de confiança: em evolução.)", style = MaterialTheme.typography.bodySmall)
            }
        }

        Text("O que o app aprendeu (local)", style = MaterialTheme.typography.titleSmall)
        Text("Aprendizado local, sem API: preferências de acessibilidade e modo ficam salvas neste aparelho. A evolução de lacunas/perfil por uso (painel detalhado) segue em implementação — ver docs/PENDENCIAS.", style = MaterialTheme.typography.bodySmall)
        Text("Privacidade e dados", style = MaterialTheme.typography.titleSmall)
        Text("Local por padrão, sem telemetria. A chave Gemini (se usada) fica criptografada no aparelho. Exportação/importação JSON e criptografia reforçada do banco: em evolução (ver docs/PENDENCIAS).", style = MaterialTheme.typography.bodySmall)
        Text("Sobre o conteúdo", style = MaterialTheme.typography.titleSmall)
        Text("Respostas mostram fonte e nível de confiança. ND e NT são leituras diferentes (tradução), sem certo/errado.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun A11yToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(label) },
        trailingContent = { androidx.compose.material3.Switch(checked = checked, onCheckedChange = onChange) },
    )
}

@Composable
fun ConnectGeminiScreen(
    initialKey: String,
    tester: GeminiConnectionTester,
    onSave: (String) -> Unit,
    onRevoke: () -> Unit,
    onContinue: () -> Unit,
) {
    var key by remember { mutableStateOf(initialKey) }
    var testing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(status) { status?.let { snackbar.showSnackbar(it) } }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.connect_gemini_title), style = MaterialTheme.typography.headlineSmall)
            Text("Opcional. O app funciona 100% offline sem chave. Se conectar, a chave fica criptografada só neste aparelho.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(value = key, onValueChange = { key = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.api_key_label)) }, singleLine = true, visualTransformation = PasswordVisualTransformation())
            Button(onClick = { if (key.isBlank()) { status = "Cole uma chave ou use offline."; return@Button }; onSave(key); status = "Chave salva com segurança." },
                modifier = Modifier.fillMaxWidth(), enabled = !testing) { Text(stringResource(R.string.save_key)) }
            OutlinedButton(onClick = {
                scope.launch {
                    testing = true; status = null
                    status = when (val res = tester.test(key)) {
                        GeminiTestResult.Success -> "Conexão OK."
                        is GeminiTestResult.Failure -> res.message
                    }
                    testing = false
                }
            }, modifier = Modifier.fillMaxWidth(), enabled = !testing && key.isNotBlank()) {
                if (testing) CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.test_connection))
            }
            TextButton(onClick = { key = ""; onRevoke(); status = "Chave apagada." }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.revoke_key)) }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.continue_label)) }
            TextButton(onClick = onContinue, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.skip_for_now)) }
        }
    }
}
