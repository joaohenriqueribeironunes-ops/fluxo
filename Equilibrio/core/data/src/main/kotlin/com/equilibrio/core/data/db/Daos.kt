package com.equilibrio.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorWindowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SensorWindowEntity): Long

    @Query("SELECT * FROM sensor_windows WHERE startMillis >= :start AND startMillis <= :end ORDER BY startMillis ASC")
    fun observeBetween(start: Long, end: Long): Flow<List<SensorWindowEntity>>

    @Query("SELECT * FROM sensor_windows ORDER BY startMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<SensorWindowEntity>>

    @Query("SELECT * FROM sensor_windows WHERE startMillis >= :start AND startMillis <= :end ORDER BY startMillis ASC")
    suspend fun between(start: Long, end: Long): List<SensorWindowEntity>

    @Query("SELECT * FROM sensor_windows ORDER BY startMillis DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<SensorWindowEntity>

    @Query("SELECT COUNT(*) FROM sensor_windows")
    suspend fun count(): Int

    @Query("SELECT MIN(startMillis) FROM sensor_windows")
    suspend fun oldest(): Long?

    @Query("DELETE FROM sensor_windows")
    suspend fun clear()

    @Query("DELETE FROM sensor_windows WHERE startMillis < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}

@Dao
interface CheckInDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CheckInEntity): Long

    @Query("SELECT * FROM check_ins WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC")
    fun observeBetween(start: Long, end: Long): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE timestamp >= :start AND timestamp <= :end ORDER BY timestamp ASC")
    suspend fun between(start: Long, end: Long): List<CheckInEntity>

    @Query("SELECT * FROM check_ins ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<CheckInEntity>

    @Query("SELECT COUNT(*) FROM check_ins")
    suspend fun count(): Int

    @Query("DELETE FROM check_ins")
    suspend fun clear()
}

@Dao
interface CrisisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CrisisEntity): Long

    @Update
    suspend fun update(entity: CrisisEntity)

    @Query("SELECT * FROM crisis_events ORDER BY startMillis DESC")
    fun observeAll(): Flow<List<CrisisEntity>>

    @Query("SELECT * FROM crisis_events WHERE startMillis >= :start AND startMillis <= :end ORDER BY startMillis ASC")
    fun observeBetween(start: Long, end: Long): Flow<List<CrisisEntity>>

    @Query("SELECT * FROM crisis_events ORDER BY startMillis ASC")
    suspend fun all(): List<CrisisEntity>

    @Query("SELECT * FROM crisis_events WHERE startMillis >= :start AND startMillis <= :end ORDER BY startMillis ASC")
    suspend fun between(start: Long, end: Long): List<CrisisEntity>

    @Query("SELECT * FROM crisis_events ORDER BY startMillis DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<CrisisEntity>

    @Query("SELECT COUNT(*) FROM crisis_events WHERE type != 'QUASE_CRISE'")
    suspend fun actualCount(): Int

    @Query("DELETE FROM crisis_events")
    suspend fun clear()
}

@Dao
interface AnalysisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AnalysisEntity): Long

    @Query("SELECT * FROM gemini_analyses ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM gemini_analyses WHERE createdAt >= :start AND createdAt <= :end ORDER BY createdAt ASC")
    fun observeBetween(start: Long, end: Long): Flow<List<AnalysisEntity>>

    @Query("SELECT * FROM gemini_analyses ORDER BY createdAt DESC LIMIT 1")
    suspend fun latest(): AnalysisEntity?

    @Query("SELECT * FROM gemini_analyses ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<AnalysisEntity>

    @Query("DELETE FROM gemini_analyses")
    suspend fun clear()
}

@Dao
interface PredictionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PredictionEntity): Long

    @Update
    suspend fun update(entity: PredictionEntity)

    @Query("SELECT * FROM predictions ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM predictions ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<PredictionEntity>

    @Query("SELECT * FROM predictions WHERE createdAt >= :start AND createdAt <= :end ORDER BY createdAt ASC")
    suspend fun between(start: Long, end: Long): List<PredictionEntity>

    @Query("SELECT * FROM predictions WHERE outcome = 'PENDENTE' AND windowEnd <= :now ORDER BY windowEnd ASC")
    suspend fun pending(now: Long): List<PredictionEntity>

    @Query("DELETE FROM predictions WHERE outcome = 'PENDENTE' AND windowStart < :cutoff")
    suspend fun dropStalePending(cutoff: Long)

    @Query("DELETE FROM predictions")
    suspend fun clear()
}

@Dao
interface BaselineDao {
    @Query("SELECT * FROM baseline WHERE id = 1")
    suspend fun get(): BaselineEntity?

    @Upsert
    suspend fun save(entity: BaselineEntity)

    @Query("DELETE FROM baseline")
    suspend fun clear()
}

@Dao
interface CalendarEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CalendarEventEntity>)

    @Query("SELECT * FROM calendar_events WHERE startMillis >= :start AND startMillis <= :end ORDER BY startMillis ASC")
    fun observeBetween(start: Long, end: Long): Flow<List<CalendarEventEntity>>

    @Query("DELETE FROM calendar_events")
    suspend fun clear()
}
