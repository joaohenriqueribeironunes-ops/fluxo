package com.equilibrio.core.domain.repository

import com.equilibrio.core.model.AccuracyReport
import com.equilibrio.core.model.BaselineProfile
import com.equilibrio.core.model.CheckIn
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.GeminiAnalysis
import com.equilibrio.core.model.PredictionRecord
import com.equilibrio.core.model.SensorWindow
import com.equilibrio.core.model.TimelineItem
import com.equilibrio.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import java.io.File

interface SensorRepository {
    fun observeWindows(startMillis: Long, endMillis: Long): Flow<List<SensorWindow>>
    fun observeRecent(limit: Int = 60): Flow<List<SensorWindow>>
    suspend fun windowsBetween(startMillis: Long, endMillis: Long): List<SensorWindow>
    suspend fun recent(limit: Int = 240): List<SensorWindow>
    suspend fun insert(window: SensorWindow): Long
    suspend fun clear()
}

interface CheckInRepository {
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<CheckIn>>
    fun observeRecent(limit: Int = 30): Flow<List<CheckIn>>
    suspend fun between(startMillis: Long, endMillis: Long): List<CheckIn>
    suspend fun recent(limit: Int = 100): List<CheckIn>
    suspend fun insert(checkIn: CheckIn): Long
    suspend fun clear()
}

interface CrisisRepository {
    fun observeAll(): Flow<List<CrisisEvent>>
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<CrisisEvent>>
    suspend fun all(): List<CrisisEvent>
    suspend fun between(startMillis: Long, endMillis: Long): List<CrisisEvent>
    suspend fun recent(limit: Int = 200): List<CrisisEvent>
    suspend fun insert(crisis: CrisisEvent): Long
    suspend fun update(crisis: CrisisEvent)
    suspend fun clear()
}

interface AnalysisRepository {
    fun observeRecent(limit: Int = 20): Flow<List<GeminiAnalysis>>
    fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<GeminiAnalysis>>
    suspend fun latest(): GeminiAnalysis?
    suspend fun recent(limit: Int = 20): List<GeminiAnalysis>
    suspend fun insert(analysis: GeminiAnalysis): Long
    suspend fun clear()
}

interface PredictionRepository {
    fun observeRecent(limit: Int = 50): Flow<List<PredictionRecord>>
    suspend fun recent(limit: Int = 100): List<PredictionRecord>
    suspend fun pending(): List<PredictionRecord>
    suspend fun insert(record: PredictionRecord): Long
    suspend fun update(record: PredictionRecord)
    suspend fun clear()
    suspend fun accuracy(periodStart: Long, periodEnd: Long): AccuracyReport
}

interface BaselineRepository {
    suspend fun load(): BaselineProfile?
    suspend fun save(profile: BaselineProfile)
    suspend fun clear()
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun update(transform: (UserSettings) -> UserSettings)
    suspend fun current(): UserSettings
    suspend fun reset()
}

interface GeminiClient {
    suspend fun analyze(payload: com.equilibrio.core.domain.gemini.GeminiPayload): Result<GeminiAnalysis>
}

interface CalendarRepository {
    suspend fun importRecent(daysBack: Int = 30): Int
    fun observeEvents(startMillis: Long, endMillis: Long): Flow<List<com.equilibrio.core.model.CalendarEvent>>
    suspend fun clear()
}

interface ReportExporter {
    suspend fun exportDailyPdf(
        summary: com.equilibrio.core.model.DailySummary,
        analysis: GeminiAnalysis?,
        accuracy: AccuracyReport?
    ): File
    suspend fun exportWeeklyPdf(
        summary: com.equilibrio.core.model.WeeklySummary,
        analysis: GeminiAnalysis?
    ): File
    suspend fun exportJson(): File
}

interface TimelineRepository {
    fun observeDay(epochDay: Long): Flow<List<TimelineItem>>
}
