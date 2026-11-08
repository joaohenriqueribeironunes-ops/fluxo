package com.equilibrio.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sensor_windows",
    indices = [Index("startMillis"), Index("riskScore")]
)
data class SensorWindowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long,
    val accelMean: Float,
    val accelStd: Float,
    val accelRms: Float,
    val accelPeaks: Int,
    val gyroMean: Float,
    val gyroStd: Float,
    val gyroRms: Float,
    val gyroPeaks: Int,
    val dominantFrequencyHz: Float,
    val spectralEnergy: Float,
    val lightMeanLux: Float,
    val proximityNear: Boolean,
    val agitationIndex: Float,
    val micCaptured: Boolean,
    val soundDbMean: Float,
    val soundDbMax: Float,
    val soundDbVariability: Float,
    val impulsivePeaks: Int,
    val lowBandRatio: Float,
    val midBandRatio: Float,
    val highBandRatio: Float,
    val soundOverloadIndex: Float,
    val batteryPercent: Int,
    val riskScore: Int
)

@Entity(
    tableName = "check_ins",
    indices = [Index("timestamp")]
)
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val emotions: String,
    val emotionIntensity: Int,
    val hunger: Int,
    val sleepHours: Float,
    val pain: Int,
    val tension: Int,
    val sensorySensitivity: Int,
    val consumed: String,
    val caffeine: Boolean,
    val medication: String,
    val supplements: String,
    val activities: String,
    val location: String,
    val people: String,
    val abruptTransitions: Int,
    val thoughts: String,
    val note: String,
    val quick: Boolean,
    val retroactive: Boolean
)

@Entity(
    tableName = "crisis_events",
    indices = [Index("startMillis"), Index("type")]
)
data class CrisisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMillis: Long,
    val endMillis: Long?,
    val type: String,
    val intensity: Int,
    val triggers: String,
    val helped: String,
    val worsened: String,
    val recoveryMinutes: Int,
    val notes: String,
    val retroactive: Boolean,
    val debriefDone: Boolean,
    val sensorWindowId: Long?
)

@Entity(
    tableName = "gemini_analyses",
    indices = [Index("createdAt")]
)
data class AnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val periodStart: Long,
    val periodEnd: Long,
    val riskCurrent: Int,
    val level: String,
    val triggers: String,
    val patterns: String,
    val analysisText: String,
    val suggestions: String,
    val confidence: Float,
    val warnings: String,
    val forecasts: String,
    val recurringPatterns: String,
    val questions: String,
    val model: String,
    val crisisCountUsed: Int,
    val daysOfData: Int
)

@Entity(
    tableName = "predictions",
    indices = [Index("createdAt"), Index("windowStart"), Index("outcome")]
)
data class PredictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val horizon: String,
    val windowStart: Long,
    val windowEnd: Long,
    val probability: Float,
    val crisisType: String?,
    val confidence: String,
    val factors: String,
    val plan: String,
    val outcome: String,
    val feedbackAt: Long?,
    val riskScore: Int,
    val source: String
)

@Entity(tableName = "baseline")
data class BaselineEntity(
    @PrimaryKey val id: Int = 1,
    val startedAt: Long,
    val daysObserved: Int,
    val sampleCount: Int,
    val stats: String,
    val ready: Boolean
)

@Entity(
    tableName = "calendar_events",
    indices = [Index("startMillis")]
)
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val calendarEventId: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val importedAt: Long
)
