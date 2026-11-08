package com.equilibrio.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.equilibrio.app.MainActivity
import com.equilibrio.app.R
import com.equilibrio.app.receiver.AlertActionReceiver
import com.equilibrio.app.receiver.NotificationActionReceiver
import com.equilibrio.app.ui.Routes
import com.equilibrio.core.model.LocalRisk
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val manager: NotificationManager? =
        context.getSystemService(NotificationManager::class.java)

    init {
        ensureChannels()
    }

    fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val collection = NotificationChannel(
            CHANNEL_COLLECTION,
            context.getString(R.string.notification_channel_collection),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mostra o estado da coleta passiva, com indicador de microfone."
            setShowBadge(false)
            setSound(null, null)
        }
        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos calmos com antecedência, limitados por dia."
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 180, 240, 180)
            setShowBadge(true)
        }
        val reminders = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notification_channel_reminders),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Lembretes suaves de check-in, sem cobrança."
            setSound(null, null)
            setShowBadge(false)
        }
        val debrief = NotificationChannel(
            CHANNEL_DEBRIEF,
            context.getString(R.string.notification_channel_debrief),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Perguntas gentis depois de uma crise, para aprender com ela."
            setShowBadge(true)
        }
        manager?.createNotificationChannels(listOf(collection, alerts, reminders, debrief))
    }

    fun ongoingNotification(
        statusText: String,
        batteryPercent: Int,
        paused: Boolean,
        micActive: Boolean
    ): Notification {
        val title = if (paused) "Equilíbrio · coleta pausada" else "Equilíbrio · coletando"
        val text = buildString {
            append(statusText)
            append(" · bateria ")
            append(batteryPercent)
            append('%')
        }
        val toggleAction = if (paused) CollectionService.ACTION_RESUME else CollectionService.ACTION_PAUSE
        val toggleLabel = if (paused) "Retomar" else "Pausar"
        return NotificationCompat.Builder(context, CHANNEL_COLLECTION)
            .setSmallIcon(R.drawable.ic_stat_equilibrio)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(Routes.HOME, 10))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(0, toggleLabel, serviceAction(toggleAction, 11))
            .addAction(0, "Encerrar", serviceAction(CollectionService.ACTION_STOP, 12))
            .build()
    }

    fun updateOngoing(
        statusText: String,
        batteryPercent: Int,
        paused: Boolean,
        micActive: Boolean
    ) {
        runCatching {
            manager?.notify(
                ID_ONGOING,
                ongoingNotification(statusText, batteryPercent, paused, micActive)
            )
        }
    }

    fun showAlert(risk: LocalRisk, plan: List<String>) {
        val minutes = 60
        val suggestion = plan.firstOrNull() ?: "Que tal uma pausa sensorial curta?"
        val factorText = risk.factors.take(2).joinToString(" · ") { it.label }
        val body = buildString {
            append("Risco estimado em ")
            append(risk.score)
            append("% nos próximos ")
            append(minutes)
            append(" min. ")
            append(suggestion)
            if (factorText.isNotBlank()) {
                append("\n")
                append(factorText)
            }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_equilibrio)
            .setContentTitle("Um cuidado preventivo")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent(Routes.CALM, 20))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                0,
                "Pausa sensorial",
                broadcast(AlertActionReceiver::class.java, AlertActionReceiver.ACTION_OPEN_CALM, 21)
            )
            .addAction(
                0,
                "Foi útil",
                broadcast(AlertActionReceiver::class.java, AlertActionReceiver.ACTION_USEFUL, 22)
            )
            .addAction(
                0,
                "Falso alarme",
                broadcast(AlertActionReceiver::class.java, AlertActionReceiver.ACTION_FALSE_ALARM, 23)
            )
            .addAction(
                0,
                "Adiar",
                broadcast(AlertActionReceiver::class.java, AlertActionReceiver.ACTION_POSTPONE, 24)
            )
            .build()
        runCatching { manager?.notify(ID_ALERT, notification) }
    }

    fun cancelAlert() = runCatching { manager?.cancel(ID_ALERT) }

    fun showBatteryPaused(percent: Int) {
        val text = "Coleta pausada porque a bateria está em $percent%. " +
            "Conecte o carregador e toque para retomar."
        val notification = NotificationCompat.Builder(context, CHANNEL_COLLECTION)
            .setSmallIcon(R.drawable.ic_stat_equilibrio)
            .setContentTitle("Equilíbrio · bateria baixa")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(Routes.HOME, 30))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        runCatching { manager?.notify(ID_BATTERY, notification) }
    }

    fun showBootReminder() {
        val text = "O Android não permite retomar o microfone sozinho após reiniciar. " +
            "Toque para reabrir o Equilíbrio e continuar a coleta."
        val notification = NotificationCompat.Builder(context, CHANNEL_COLLECTION)
            .setSmallIcon(R.drawable.ic_stat_equilibrio)
            .setContentTitle("Retome a coleta do Equilíbrio")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(Routes.HOME, 40))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        runCatching { manager?.notify(ID_BOOT, notification) }
    }

    fun showCheckInReminder() {
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_equilibrio)
            .setContentTitle("Como você está agora?")
            .setContentText("Um check-in rápido de 3 toques ajuda a entender seus padrões.")
            .setContentIntent(
                broadcast(
                    NotificationActionReceiver::class.java,
                    NotificationActionReceiver.ACTION_CHECKIN,
                    50
                )
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        runCatching { manager?.notify(ID_REMINDER, notification) }
    }

    fun showDebrief(crisisId: Long, typeLabel: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_DEBRIEF)
            .setSmallIcon(R.drawable.ic_stat_equilibrio)
            .setContentTitle("Como foi depois da crise?")
            .setContentText("3 toques para registrar gatilho, o que ajudou e recuperação ($typeLabel).")
            .setContentIntent(openAppIntent(Routes.debrief(crisisId), 60))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                0,
                "Abrir debrief",
                broadcast(
                    NotificationActionReceiver::class.java,
                    NotificationActionReceiver.ACTION_DEBRIEF,
                    61,
                    crisisId
                )
            )
            .build()
        runCatching { manager?.notify(ID_DEBRIEF, notification) }
    }

    fun cancelDebrief() = runCatching { manager?.cancel(ID_DEBRIEF) }

    fun showDailyReady(averageRisk: Int, crises: Int, nearCrises: Int) {
        val text = if (crises == 0 && nearCrises == 0) {
            "Dia sem crises registradas. Risco médio $averageRisk/100. Veja a linha do tempo e o relatório."
        } else {
            "$crises crise(s) e $nearCrises quase-crise(s). Risco médio $averageRisk/100. Veja o relatório do dia."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_equilibrio)
            .setContentTitle("Resumo do seu dia")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent(Routes.REPORT, 70))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        runCatching { manager?.notify(ID_DAILY, notification) }
    }

    private fun openAppIntent(route: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(Routes.EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun serviceAction(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, CollectionService::class.java).setAction(action)
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun <T> broadcast(
        receiver: Class<T>,
        action: String,
        requestCode: Int,
        crisisId: Long? = null
    ): PendingIntent {
        val intent = Intent(context, receiver).setAction(action)
        if (crisisId != null) intent.putExtra(Routes.ARG_CRISIS_ID, crisisId)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_COLLECTION = "equilibrio_collection"
        const val CHANNEL_ALERTS = "equilibrio_alerts"
        const val CHANNEL_REMINDERS = "equilibrio_reminders"
        const val CHANNEL_DEBRIEF = "equilibrio_debrief"

        const val ID_ONGOING = 1001
        const val ID_ALERT = 2001
        const val ID_BATTERY = 2002
        const val ID_BOOT = 2003
        const val ID_REMINDER = 2010
        const val ID_DEBRIEF = 2020
        const val ID_DAILY = 2030
    }
}
