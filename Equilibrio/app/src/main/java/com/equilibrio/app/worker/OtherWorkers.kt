package com.equilibrio.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.equilibrio.app.service.NotificationHelper
import com.equilibrio.core.domain.repository.CheckInRepository
import com.equilibrio.core.domain.repository.CrisisRepository
import com.equilibrio.core.domain.repository.SensorRepository
import com.equilibrio.core.domain.repository.SettingsRepository
import com.equilibrio.core.domain.time.TimeUtils
import com.equilibrio.core.model.CrisisType
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CheckInReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val checkInRepository: CheckInRepository,
    private val notifications: NotificationHelper
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.current()
        if (!settings.checkInReminders) return Result.success()
        val hour = TimeUtils.hourOfDay(System.currentTimeMillis())
        if (hour < settings.reminderStartHour || hour > settings.reminderEndHour) return Result.success()

        val last = checkInRepository.recent(1).firstOrNull()
        val now = System.currentTimeMillis()
        if (last != null && now - last.timestamp < 3 * 3_600_000L) return Result.success()

        notifications.showCheckInReminder()
        return Result.success()
    }
}

@HiltWorker
class PredictionResolutionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val predictionRepository: com.equilibrio.core.domain.repository.PredictionRepository,
    private val crisisRepository: CrisisRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val pending = predictionRepository.pending()
        if (pending.isEmpty()) return Result.success()
        val crises = crisisRepository.recent(300)

        pending.filter { it.windowEnd < now - 2 * 3_600_000L }.forEach { record ->
            val crisis = crises.firstOrNull { it.startMillis in record.windowStart..record.windowEnd }
            val outcome = when {
                crisis == null -> com.equilibrio.core.model.PredictedOutcome.NADA
                crisis.type == CrisisType.QUASE_CRISE ->
                    com.equilibrio.core.model.PredictedOutcome.QUASE_CRISE
                else -> com.equilibrio.core.model.PredictedOutcome.CRISE
            }
            runCatching {
                predictionRepository.update(record.copy(outcome = outcome, feedbackAt = now))
            }
        }
        return Result.success()
    }
}

@HiltWorker
class CrisisDebriefWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val crisisRepository: CrisisRepository,
    private val notifications: NotificationHelper
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getLong(com.equilibrio.app.ui.Routes.ARG_CRISIS_ID, -1L)
        if (id <= 0L) return Result.success()
        val crisis = crisisRepository.all().firstOrNull { it.id == id } ?: return Result.success()
        if (crisis.debriefDone) return Result.success()
        notifications.showDebrief(crisis.id, crisis.type.label)
        return Result.success()
    }
}

@HiltWorker
class DailyDigestWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val sensorRepository: SensorRepository,
    private val checkInRepository: CheckInRepository,
    private val crisisRepository: CrisisRepository,
    private val notifications: NotificationHelper,
    private val scheduler: WorkScheduler
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.current()
        val now = System.currentTimeMillis()
        val start = TimeUtils.startOfDay(now)
        val end = TimeUtils.endOfDay(now)
        val windows = sensorRepository.windowsBetween(start, end)
        val checkIns = checkInRepository.between(start, end)
        val crises = crisisRepository.between(start, end)
        val summary = com.equilibrio.core.domain.report.DailySummaryBuilder.build(
            dayMillis = now,
            windows = windows,
            checkIns = checkIns,
            crises = crises
        )
        notifications.showDailyReady(
            averageRisk = summary.averageRisk,
            crises = summary.crisisCount,
            nearCrises = summary.nearCrisisCount
        )
        if (settings.geminiEnabled && settings.geminiConsent) {
            scheduler.enqueueGeminiNow()
        }
        return Result.success()
    }
}

@HiltWorker
class WeeklyMaintenanceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val sensorWindowDao: com.equilibrio.core.data.db.SensorWindowDao,
    private val predictionDao: com.equilibrio.core.data.db.PredictionDao
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        runCatching {
            sensorWindowDao.deleteOlderThan(now - 180L * 86_400_000L)
            predictionDao.dropStalePending(now - 2L * 86_400_000L)
        }
        return Result.success()
    }
}
