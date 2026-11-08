package com.equilibrio.core.data.repository

import com.equilibrio.core.data.db.AnalysisDao
import com.equilibrio.core.data.db.CrisisDao
import com.equilibrio.core.data.db.PredictionDao
import com.equilibrio.core.data.mapper.toEntity
import com.equilibrio.core.data.mapper.toModel
import com.equilibrio.core.domain.prediction.AccuracyCalculator
import com.equilibrio.core.domain.repository.AnalysisRepository
import com.equilibrio.core.domain.repository.CrisisRepository
import com.equilibrio.core.domain.repository.PredictionRepository
import com.equilibrio.core.model.AccuracyReport
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.GeminiAnalysis
import com.equilibrio.core.model.PredictionRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrisisRepositoryImpl @Inject constructor(
    private val dao: CrisisDao
) : CrisisRepository {

    override fun observeAll(): Flow<List<CrisisEvent>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<CrisisEvent>> =
        dao.observeBetween(startMillis, endMillis).map { list -> list.map { it.toModel() } }

    override suspend fun all(): List<CrisisEvent> = dao.all().map { it.toModel() }

    override suspend fun between(startMillis: Long, endMillis: Long): List<CrisisEvent> =
        dao.between(startMillis, endMillis).map { it.toModel() }

    override suspend fun recent(limit: Int): List<CrisisEvent> =
        dao.recent(limit).map { it.toModel() }

    override suspend fun insert(crisis: CrisisEvent): Long = dao.insert(crisis.toEntity())

    override suspend fun update(crisis: CrisisEvent) = dao.update(crisis.toEntity())

    override suspend fun clear() = dao.clear()
}

@Singleton
class AnalysisRepositoryImpl @Inject constructor(
    private val dao: AnalysisDao
) : AnalysisRepository {

    override fun observeRecent(limit: Int): Flow<List<GeminiAnalysis>> =
        dao.observeRecent(limit).map { list -> list.map { it.toModel() } }

    override fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<GeminiAnalysis>> =
        dao.observeBetween(startMillis, endMillis).map { list -> list.map { it.toModel() } }

    override suspend fun latest(): GeminiAnalysis? = dao.latest()?.toModel()

    override suspend fun recent(limit: Int): List<GeminiAnalysis> =
        dao.recent(limit).map { it.toModel() }

    override suspend fun insert(analysis: GeminiAnalysis): Long = dao.insert(analysis.toEntity())

    override suspend fun clear() = dao.clear()
}

@Singleton
class PredictionRepositoryImpl @Inject constructor(
    private val dao: PredictionDao,
    private val crisisDao: CrisisDao
) : PredictionRepository {

    override fun observeRecent(limit: Int): Flow<List<PredictionRecord>> =
        dao.observeRecent(limit).map { list -> list.map { it.toModel() } }

    override suspend fun recent(limit: Int): List<PredictionRecord> =
        dao.recent(limit).map { it.toModel() }

    override suspend fun pending(): List<PredictionRecord> =
        dao.pending(System.currentTimeMillis()).map { it.toModel() }

    override suspend fun insert(record: PredictionRecord): Long = dao.insert(record.toEntity())

    override suspend fun update(record: PredictionRecord) = dao.update(record.toEntity())

    override suspend fun clear() = dao.clear()

    override suspend fun accuracy(periodStart: Long, periodEnd: Long): AccuracyReport {
        val predictions = dao.between(periodStart, periodEnd).map { it.toModel() }
        val crises = crisisDao.between(periodStart, periodEnd).map { it.toModel() }
        return AccuracyCalculator.compute(predictions, crises, periodStart, periodEnd)
    }
}
