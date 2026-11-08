package com.equilibrio.core.data.repository

import com.equilibrio.core.data.db.CheckInDao
import com.equilibrio.core.data.db.SensorWindowDao
import com.equilibrio.core.data.mapper.toEntity
import com.equilibrio.core.data.mapper.toModel
import com.equilibrio.core.domain.repository.CheckInRepository
import com.equilibrio.core.domain.repository.SensorRepository
import com.equilibrio.core.model.CheckIn
import com.equilibrio.core.model.SensorWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRepositoryImpl @Inject constructor(
    private val dao: SensorWindowDao
) : SensorRepository {

    override fun observeWindows(startMillis: Long, endMillis: Long): Flow<List<SensorWindow>> =
        dao.observeBetween(startMillis, endMillis).map { list -> list.map { it.toModel() } }

    override fun observeRecent(limit: Int): Flow<List<SensorWindow>> =
        dao.observeRecent(limit).map { list -> list.map { it.toModel() }.sortedBy { it.startMillis } }

    override suspend fun windowsBetween(startMillis: Long, endMillis: Long): List<SensorWindow> =
        dao.between(startMillis, endMillis).map { it.toModel() }

    override suspend fun recent(limit: Int): List<SensorWindow> =
        dao.recent(limit).map { it.toModel() }.sortedBy { it.startMillis }

    override suspend fun insert(window: SensorWindow): Long = dao.insert(window.toEntity())

    override suspend fun clear() = dao.clear()
}

@Singleton
class CheckInRepositoryImpl @Inject constructor(
    private val dao: CheckInDao
) : CheckInRepository {

    override fun observeBetween(startMillis: Long, endMillis: Long): Flow<List<CheckIn>> =
        dao.observeBetween(startMillis, endMillis).map { list -> list.map { it.toModel() } }

    override fun observeRecent(limit: Int): Flow<List<CheckIn>> =
        dao.observeRecent(limit).map { list -> list.map { it.toModel() } }

    override suspend fun between(startMillis: Long, endMillis: Long): List<CheckIn> =
        dao.between(startMillis, endMillis).map { it.toModel() }

    override suspend fun recent(limit: Int): List<CheckIn> =
        dao.recent(limit).map { it.toModel() }

    override suspend fun insert(checkIn: CheckIn): Long = dao.insert(checkIn.toEntity())

    override suspend fun clear() = dao.clear()
}
