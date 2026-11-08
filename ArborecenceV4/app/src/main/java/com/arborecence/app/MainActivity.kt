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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.arborecence.coreui.theme.ArborecenceTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var keyStore: GeminiKeyStore
    @Inject lateinit var tester: GeminiConnectionTester

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArborecenceTheme {
                var onboarded by remember { mutableStateOf(keyStore.hasKey() || false) }
                var showConnect by remember { mutableStateOf(!keyStore.hasKey()) }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (showConnect) {
                        ConnectGeminiScreen(
                            initialKey = keyStore.getKey().orEmpty(),
                            tester = tester,
                            onSave = { key -> keyStore.saveKey(key) },
                            onRevoke = {
                                keyStore.revokeKey()
                                showConnect = true
                            },
                            onContinue = { showConnect = false; onboarded = true },
                        )
                    } else {
                        HomePlaceholder(onOpenGemini = { showConnect = true })
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectGeminiScreen(
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
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.connect_gemini_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.connect_gemini_body), style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.api_key_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Button(
                onClick = {
                    if (key.isBlank()) { status = "Cole uma chave ou use offline."; return@Button }
                    onSave(key); status = "Chave salva com segurança."
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !testing,
            ) { Text(stringResource(R.string.save_key)) }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        testing = true; status = null
                        val res = tester.test(key)
                        testing = false
                        status = when (res) {
                            GeminiTestResult.Success -> "Conexão OK."
                            is GeminiTestResult.Failure -> res.message
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !testing && key.isNotBlank(),
            ) {
                if (testing) CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                else Text(stringResource(R.string.test_connection))
            }
            TextButton(onClick = { key = ""; onRevoke(); status = "Chave apagada." }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.revoke_key))
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.continue_label))
            }
            TextButton(onClick = onContinue, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.skip_for_now))
            }
        }
    }
}

@Composable
private fun HomePlaceholder(onOpenGemini: () -> Unit) {
    Scaffold { pad ->
        Column(
            modifier = Modifier.fillMaxSize().padding(pad).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Arborecence — FASE 1", style = MaterialTheme.typography.headlineSmall)
            Text("Estrutura, módulos e bancos de dados prontos. Conteúdo e recursos completos chegam nas próximas fases.", style = MaterialTheme.typography.bodyMedium)
            Text("• behavior.db e academic.db (schemas Room)\n• Tela de conexão Gemini (chave criptografada)\n• Script de verificação de volume", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onOpenGemini) { Text("Conectar / trocar Gemini") }
        }
    }
}
