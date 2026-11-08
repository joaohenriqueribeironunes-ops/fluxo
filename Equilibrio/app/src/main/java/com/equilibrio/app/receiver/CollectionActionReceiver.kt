package com.equilibrio.app.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.equilibrio.app.MainActivity
import com.equilibrio.app.service.CollectionService
import com.equilibrio.app.ui.Routes

/**
 * Atalhos/automações que encaminham ações para o Foreground Service.
 * A notificação usa PendingIntent direto no serviço; este receiver existe
 * para integrações explícitas (por exemplo, atalhos do launcher).
 */
class CollectionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val serviceIntent = Intent(context, CollectionService::class.java).setAction(action)
        runCatching {
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val route = when (intent.action) {
            ACTION_CHECKIN -> Routes.CHECKIN
            ACTION_DEBRIEF -> {
                val id = intent.getLongExtra(Routes.ARG_CRISIS_ID, -1L)
                if (id > 0) Routes.debrief(id) else Routes.CRISES
            }
            else -> return
        }
        open(context, route)
    }

    private fun open(context: Context, route: String) {
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(Routes.EXTRA_ROUTE, route)
        }
        runCatching { context.startActivity(open) }
    }

    companion object {
        const val ACTION_CHECKIN = "com.equilibrio.action.OPEN_CHECKIN"
        const val ACTION_DEBRIEF = "com.equilibrio.action.OPEN_DEBRIEF"
    }
}
