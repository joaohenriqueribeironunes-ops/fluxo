package com.equilibrio.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SensorWindowEntity::class,
        CheckInEntity::class,
        CrisisEntity::class,
        AnalysisEntity::class,
        PredictionEntity::class,
        BaselineEntity::class,
        CalendarEventEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class EquilibrioDatabase : RoomDatabase() {
    abstract fun sensorWindowDao(): SensorWindowDao
    abstract fun checkInDao(): CheckInDao
    abstract fun crisisDao(): CrisisDao
    abstract fun analysisDao(): AnalysisDao
    abstract fun predictionDao(): PredictionDao
    abstract fun baselineDao(): BaselineDao
    abstract fun calendarDao(): CalendarEventDao

    companion object {
        const val NAME = "equilibrio_encrypted.db"
    }
}
