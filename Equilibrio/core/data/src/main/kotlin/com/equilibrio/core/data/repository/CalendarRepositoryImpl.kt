package com.equilibrio.core.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import com.equilibrio.core.data.db.CalendarEventDao
import com.equilibrio.core.data.db.CalendarEventEntity
import com.equilibrio.core.data.mapper.toModel
import com.equilibrio.core.domain.repository.CalendarRepository
import com.equilibrio.core.model.CalendarEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Importação opcional e explícita do calendário. Só títulos e horários são
 * guardados; descrições, participantes e localização são descartados.
 */
@Singleton
class CalendarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: CalendarEventDao
) : CalendarRepository {

    override suspend fun importRecent(daysBack: Int): Int = withContext(Dispatchers.IO) {
        if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext 0
        }
        val now = System.currentTimeMillis()
        val from = now - daysBack * 86_400_000L
        val to = now + 7 * 86_400_000L
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY
        )
        val selection = "${CalendarContract.Events.DTSTART} >= ? AND " +
            "${CalendarContract.Events.DTSTART} <= ?"
        val rows = mutableListOf<CalendarEventEntity>()
        runCatching {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                arrayOf(from.toString(), to.toString()),
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
                while (cursor.moveToNext()) {
                    val start = cursor.getLong(startIdx)
                    val end = if (cursor.isNull(endIdx)) start + 3_600_000L
                    else cursor.getLong(endIdx)
                    rows += CalendarEventEntity(
                        calendarEventId = cursor.getLong(idIdx),
                        title = cursor.getString(titleIdx)?.take(120) ?: "Compromisso",
                        startMillis = start,
                        endMillis = end,
                        allDay = cursor.getInt(allDayIdx) == 1,
                        importedAt = now
                    )
                }
            }
        }
        if (rows.isNotEmpty()) {
            dao.clear()
            dao.insertAll(rows)
        }
        rows.size
    }

    override fun observeEvents(startMillis: Long, endMillis: Long): Flow<List<CalendarEvent>> =
        dao.observeBetween(startMillis, endMillis).map { list -> list.map { it.toModel() } }

    override suspend fun clear() = dao.clear()
}
