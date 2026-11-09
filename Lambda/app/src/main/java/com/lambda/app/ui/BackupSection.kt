package com.lambda.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lambda.app.util.BackupUtil
import com.lambda.databehavior.dao.BehaviorDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(private val dao: BehaviorDao) : ViewModel() {
    private val _msg = MutableStateFlow<String?>(null)
    val msg = _msg.asStateFlow()
    fun export(ctx: android.content.Context) {
        viewModelScope.launch { _msg.value = runCatching { "Exportado: ${BackupUtil.exportAll(ctx, dao).absolutePath}" }.getOrElse { "Erro: ${it.message}" } }
    }
    fun import(ctx: android.content.Context, file: File) {
        viewModelScope.launch { _msg.value = runCatching { BackupUtil.importAll(ctx, dao, file) }.getOrElse { "Erro: ${it.message}" } }
    }
}

@Composable
fun BackupSection(vm: BackupViewModel = hiltViewModel()) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val msg by vm.msg.collectAsState()
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val tmp = File(ctx.cacheDir, "import.json")
                    ctx.contentResolver.openInputStream(uri)?.use { i -> tmp.outputStream().use { o -> i.copyTo(o) } }
                    vm.import(ctx, tmp)
                }
            }
        }
    }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Backup local (JSON)", style = MaterialTheme.typography.titleSmall)
            Text("Exporta/importa compromissos, destaques e resumo do perfil. Arquivos ficam no armazenamento do app.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { vm.export(ctx) }) { Text("Exportar") }
                OutlinedButton(onClick = { importLauncher.launch("application/json") }) { Text("Importar") }
            }
            msg?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
