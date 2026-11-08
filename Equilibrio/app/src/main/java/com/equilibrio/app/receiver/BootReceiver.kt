package com.equilibrio.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.equilibrio.app.service.NotificationHelper
import com.equilibrio.app.worker.WorkScheduler
import com.equilibrio.core.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Android 14+ não permite iniciar um Foreground Service de microfone a partir
 * do boot. Então o app apenas lembra o usuário de reabrir. Também reagenda os
 * workers periódicos, que o sistema pode ter limpado.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notifications: NotificationHelper
    @Inject lateinit var scheduler: WorkScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = settingsRepository.current()
                scheduler.scheduleAll(settings)
                if (settings.collectionEnabled) {
                    notifications.showBootReminder()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
