package com.equilibrio.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.equilibrio.app.ui.Routes
import com.equilibrio.core.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    fun scheduleAll(settings: UserSettings) {
        scheduleGemini(settings)
        scheduleReminders(settings)
        scheduleResolution()
        scheduleWeekly()
    }

    fun scheduleGemini(settings: UserSettings) {
        if (!settings.geminiEnabled || !settings.geminiConsent) {
            workManager.cancelUniqueWork(NAME_GEMINI)
            return
        }
        val minutes = settings.geminiBatchMinutes.coerceIn(30, 360).toLong()
        val request = PeriodicWorkRequestBuilder<GeminiBatchWorker>(minutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            NAME_GEMINI,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleReminders(settings: UserSettings) {
        if (!settings.checkInReminders) {
            workManager.cancelUniqueWork(NAME_REMINDERS)
            return
        }
        val minutes = settings.reminderIntervalMinutes.coerceIn(60, 480).toLong()
        val request = PeriodicWorkRequestBuilder<CheckInReminderWorker>(minutes, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            NAME_REMINDERS,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleResolution() {
        val request = PeriodicWorkRequestBuilder<PredictionResolutionWorker>(15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            NAME_RESOLUTION,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleWeekly() {
        val request = PeriodicWorkRequestBuilder<WeeklyMaintenanceWorker>(7, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            NAME_WEEKLY,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleDailyDigest() {
        val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            NAME_DAILY,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleDebrief(crisisId: Long, delayMinutes: Long = 30) {
        val request = OneTimeWorkRequestBuilder<CrisisDebriefWorker>()
            .setInputData(workDataOf(Routes.ARG_CRISIS_ID to crisisId))
            .setInitialDelay(delayMinutes.coerceAtLeast(1), TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniqueWork(
            "debrief_$crisisId",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun enqueueGeminiNow() {
        val request = OneTimeWorkRequestBuilder<GeminiBatchWorker>()
            .setInputData(workDataOf(GeminiBatchWorker.KEY_FORCE to true))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork(NAME_GEMINI_NOW, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(NAME_GEMINI)
        workManager.cancelUniqueWork(NAME_GEMINI_NOW)
        workManager.cancelUniqueWork(NAME_REMINDERS)
        workManager.cancelUniqueWork(NAME_RESOLUTION)
        workManager.cancelUniqueWork(NAME_WEEKLY)
        workManager.cancelUniqueWork(NAME_DAILY)
    }

    private companion object {
        const val NAME_GEMINI = "equilibrio_gemini_batch"
        const val NAME_GEMINI_NOW = "equilibrio_gemini_now"
        const val NAME_REMINDERS = "equilibrio_checkin_reminders"
        const val NAME_RESOLUTION = "equilibrio_prediction_resolution"
        const val NAME_WEEKLY = "equilibrio_weekly_maintenance"
        const val NAME_DAILY = "equilibrio_daily_digest"
    }
}
