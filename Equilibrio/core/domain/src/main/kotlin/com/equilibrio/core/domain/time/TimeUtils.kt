package com.equilibrio.core.domain.time

import com.equilibrio.core.model.DayPart
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val hm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("pt", "BR"))
    private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale("pt", "BR"))
    private val full: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

    fun startOfDay(millis: Long, zoneId: ZoneId = zone): Long =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
            .atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun endOfDay(millis: Long, zoneId: ZoneId = zone): Long =
        Instant.ofEpochMilli(startOfDay(millis, zoneId)).atZone(zoneId)
            .toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

    fun epochDay(millis: Long, zoneId: ZoneId = zone): Long =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate().toEpochDay()

    fun epochDayToMillis(epochDay: Long, zoneId: ZoneId = zone): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun hourOfDay(millis: Long, zoneId: ZoneId = zone): Int =
        Instant.ofEpochMilli(millis).atZone(zoneId).hour

    fun dayPart(millis: Long, zoneId: ZoneId = zone): DayPart =
        DayPart.fromHour(hourOfDay(millis, zoneId))

    /** 1 = segunda-feira ... 7 = domingo. */
    fun dayOfWeek(millis: Long, zoneId: ZoneId = zone): Int =
        Instant.ofEpochMilli(millis).atZone(zoneId).dayOfWeek.value

    fun formatHm(millis: Long, zoneId: ZoneId = zone): String =
        Instant.ofEpochMilli(millis).atZone(zoneId).format(hm)

    fun formatDayMonth(millis: Long, zoneId: ZoneId = zone): String =
        Instant.ofEpochMilli(millis).atZone(zoneId).format(dayMonth)

    fun formatFull(millis: Long, zoneId: ZoneId = zone): String =
        Instant.ofEpochMilli(millis).atZone(zoneId).format(full)

    fun startOfNextHour(millis: Long, zoneId: ZoneId = zone): Long {
        val z = Instant.ofEpochMilli(millis).atZone(zoneId)
        return z.plusHours(1).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli()
    }

    fun plusHours(millis: Long, hours: Int): Long = millis + hours * 3_600_000L

    fun plusMinutes(millis: Long, minutes: Int): Long = millis + minutes * 60_000L

    fun weekdayLabel(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "Seg"
        2 -> "Ter"
        3 -> "Qua"
        4 -> "Qui"
        5 -> "Sex"
        6 -> "Sáb"
        7 -> "Dom"
        else -> "?"
    }

    fun nowLocal(): LocalDateTime = LocalDateTime.now(zone)
}
