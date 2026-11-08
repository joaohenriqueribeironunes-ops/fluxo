package com.equilibrio.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.equilibrio.app.MainActivity
import com.equilibrio.app.service.NotificationHelper
import com.equilibrio.app.ui.Routes
import com.equilibrio.core.domain.repository.PredictionRepository
import com.equilibrio.core.domain.repository.SettingsRepository
import com.equilibrio.core.model.PredictedOutcome
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Ações do alerta preventivo: abrir pausa sensorial, marcar alerta útil,
 * marcar falso alarme ou adiar. Todo retorno calibra o modelo local.
 */
@AndroidEntryPoint
class AlertActionReceiver : BroadcastReceiver() {

    @Inject lateinit var predictionRepository: PredictionRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notifications: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        notifications.cancelAlert()

        if (action == ACTION_OPEN_CALM) {
            open(context, Routes.CALM)
            return
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            try {
                val latest = predictionRepository.recent(8)
                    .firstOrNull { it.createdAt > now - 3 * 3_600_000L }
                when (action) {
                    ACTION_USEFUL -> {
                        latest?.let {
                            predictionRepository.update(
                                it.copy(
                                    outcome = PredictedOutcome.ALERTA_UTIL,
                                    feedbackAt = now
                                )
                            )
                        }
                        settingsRepository.update { it.copy(lastAlertAt = now) }
                    }
                    ACTION_FALSE_ALARM -> {
                        latest?.let {
                            predictionRepository.update(
                                it.copy(
                                    outcome = PredictedOutcome.FALSO_ALARME,
                                    feedbackAt = now
                                )
                            )
                        }
                        // Um falso alarme não deve ser repetido na mesma janela.
                        settingsRepository.update { it.copy(lastAlertAt = now + 45 * 60_000L) }
                    }
                    ACTION_POSTPONE -> {
                        latest?.let {
                            predictionRepository.update(
                                it.copy(
                                    outcome = PredictedOutcome.ALERTA_UTIL,
                                    feedbackAt = now
                                )
                            )
                        }
                        settingsRepository.update { it.copy(lastAlertAt = now + 30 * 60_000L) }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun open(context: Context, route: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(Routes.EXTRA_ROUTE, route)
        }
        runCatching { context.startActivity(intent) }
    }

    companion object {
        const val ACTION_OPEN_CALM = "com.equilibrio.action.ALERT_CALM"
        const val ACTION_USEFUL = "com.equilibrio.action.ALERT_USEFUL"
        const val ACTION_FALSE_ALARM = "com.equilibrio.action.ALERT_FALSE"
        const val ACTION_POSTPONE = "com.equilibrio.action.ALERT_POSTPONE"
    }
}
