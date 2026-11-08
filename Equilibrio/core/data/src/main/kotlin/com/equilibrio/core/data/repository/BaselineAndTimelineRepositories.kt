package com.equilibrio.core.data.repository

import com.equilibrio.core.data.db.AnalysisDao
import com.equilibrio.core.data.db.BaselineDao
import com.equilibrio.core.data.db.CheckInDao
import com.equilibrio.core.data.db.CrisisDao
import com.equilibrio.core.data.db.SensorWindowDao
import com.equilibrio.core.data.mapper.toEntity
import com.equilibrio.core.data.mapper.toModel
import com.equilibrio.core.domain.report.DailySummaryBuilder
import com.equilibrio.core.domain.repository.BaselineRepository
import com.equilibrio.core.domain.repository.TimelineRepository
import com.equilibrio.core.domain.time.TimeUtils
import com.equilibrio.core.model.BaselineProfile
import com.equilibrio.core.model.TimelineItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BaselineRepositoryImpl @Inject constructor(
    private val dao: BaselineDao
) : BaselineRepository {

    override suspend fun load(): BaselineProfile? = dao.get()?.toModel()

    override suspend fun save(profile: BaselineProfile) = dao.save(profile.toEntity())

    override suspend fun clear() = dao.clear()
}

@Singleton
class TimelineRepositoryImpl @Inject constructor(
    private val sensorDao: SensorWindowDao,
    private val checkInDao: CheckInDao,
    private val crisisDao: CrisisDao,
    private val analysisDao: AnalysisDao
) : TimelineRepository {

    override fun observeDay(epochDay: Long): Flow<List<TimelineItem>> {
        val start = TimeUtils.epochDayToMillis(epochDay)
        val end = start + 86_400_000L - 1
        return combine(
            sensorDao.observeBetween(start, end),
            checkInDao.observeBetween(start, end),
            crisisDao.observeBetween(start, end),
            analysisDao.observeBetween(start, end)
        ) { windows, checkIns, crises, analyses ->
            DailySummaryBuilder.buildTimeline(
                dayStart = start,
                dayEnd = end,
                windows = windows.map { it.toModel() },
                checkIns = checkIns.map { it.toModel() },
                crises = crises.map { it.toModel() },
                analyses = analyses.map { it.toModel() }
            )
        }
    }
}
