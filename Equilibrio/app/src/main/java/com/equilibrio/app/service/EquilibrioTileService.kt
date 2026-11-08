package com.equilibrio.app.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.equilibrio.app.MainActivity
import com.equilibrio.app.ui.Routes
import com.equilibrio.core.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class EquilibrioTileService : TileService() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var bus: CollectionBus

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        // Re-lê o estado real (fonte: DataStore + serviço em memória) para não dessincronizar
        // após morte do processo/MIUI.
        scope.launch { refreshState() }
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        scope.launch {
            val enabled = runCatching { settingsRepository.current().collectionEnabled }.getOrDefault(false)
            val running = bus.state.value.running
            val active = enabled && running
            if (active) {
                stopCollection()
            } else {
                startCollection()
            }
            refreshState()
        }
    }

    private suspend fun refreshState() {
        val settings = runCatching { withContext(Dispatchers.IO) { settingsRepository.current() } }.getOrNull()
        val enabled = settings?.collectionEnabled == true
        val running = bus.state.value.running
        val active = enabled && running
        val tile = qsTile ?: return
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Equilíbrio"
        tile.subtitle = when {
            active && bus.state.value.micActive -> "Coletando · microfone"
            active -> "Coletando"
            enabled && !running -> "Iniciando…"
            else -> "Desligado"
        }
        tile.contentDescription = if (active) "Desligar coleta do Equilíbrio" else "Ligar coleta do Equilíbrio"
        runCatching { tile.updateTile() }
    }

    private fun startCollection() {
        val missing = missingPermissions()
        if (missing.isNotEmpty()) {
            openAppForPermissions()
            return
        }
        runCatching {
            scope.launch(Dispatchers.IO) {
                settingsRepository.update { it.copy(collectionEnabled = true) }
            }
        }
        runCatching {
            ContextCompat.startForegroundService(
                this,
                CollectionService.startIntent(this)
            )
        }
    }

    private fun stopCollection() {
        runCatching {
            scope.launch(Dispatchers.IO) {
                settingsRepository.update { it.copy(collectionEnabled = false) }
            }
        }
        runCatching {
            startService(CollectionService.stopIntent(this))
        }
    }

    private fun missingPermissions(): List<String> {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) needed += android.Manifest.permission.POST_NOTIFICATIONS
        }
        // Microfone é opcional para a coleta (sensores funcionam sem), mas o serviço
        // é declarado com tipo microphone|health; pedimos no app se o usuário quiser.
        return needed
    }

    private fun openAppForPermissions() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(Routes.EXTRA_ROUTE, Routes.SETTINGS)
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val pi = PendingIntent.getActivity(
                this, 9001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            runCatching { startActivityAndCollapse(pi) }.onFailure { openAppLegacy(intent) }
        } else {
            openAppLegacy(intent)
        }
    }

    @Suppress("DEPRECATION", "StartActivityAndCollapseDeprecated")
    private fun openAppLegacy(intent: Intent) {
        runCatching { startActivityAndCollapse(intent) }.onFailure {
            runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun requestRefresh(context: android.content.Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
            runCatching {
                requestListeningState(
                    context,
                    ComponentName(context, EquilibrioTileService::class.java)
                )
            }
        }

        fun requestAddTile(context: android.content.Context) {
            if (Build.VERSION.SDK_INT < 33) return
            val sbm = context.getSystemService(android.app.StatusBarManager::class.java) ?: return
            runCatching {
                sbm.requestAddTileService(
                    ComponentName(context, EquilibrioTileService::class.java),
                    context.getString(com.equilibrio.app.R.string.app_name),
                    android.graphics.drawable.Icon.createWithResource(
                        context, com.equilibrio.app.R.drawable.ic_qs_equilibrio
                    ),
                    { it.run() },
                    { /* resultado ignorado; usuário pode adicionar manualmente */ }
                )
            }
        }
    }
}
