package com.equilibrio.core.domain.report

import com.equilibrio.core.model.CheckIn
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.DailySummary
import com.equilibrio.core.model.Emotion
import com.equilibrio.core.model.GeminiAnalysis
import com.equilibrio.core.model.SensorWindow
import com.equilibrio.core.model.TimelineItem
import com.equilibrio.core.domain.time.TimeUtils

object DailySummaryBuilder {

    fun build(
        dayMillis: Long,
        windows: List<SensorWindow>,
        checkIns: List<CheckIn>,
        crises: List<CrisisEvent>,
        analyses: List<GeminiAnalysis> = emptyList()
    ): DailySummary {
        val start = TimeUtils.startOfDay(dayMillis)
        val end = TimeUtils.endOfDay(dayMillis)
        val dayWindows = windows.filter { it.startMillis in start..end }
        val dayCheckIns = checkIns.filter { it.timestamp in start..end }
        val dayCrises = crises.filter { it.startMillis in start..end }
        val dayAnalyses = analyses.filter { it.createdAt in start..end }

        val riskScores = dayWindows.map { it.riskScore }.filter { it > 0 }
        val actualCrises = dayCrises.filter { it.type.isActualCrisis }
        val near = dayCrises.filter { it.type == com.equilibrio.core.model.CrisisType.QUASE_CRISE }
        val sleep = dayCheckIns.map { it.sleepHours }.filter { it > 0f }

        val emotions = dayCheckIns.flatMap { it.emotions }
            .groupingBy { it }
            .eachCount()
            .entries.sortedByDescending { it.value }
            .map { it.key }
            .take(3)

        val triggers = actualCrises.flatMap { it.triggers }
            .filter { it.isNotBlank() }
            .groupingBy { it.trim().lowercase() }
            .eachCount()
            .entries.sortedByDescending { it.value }
            .map { it.key }
            .take(5)

        return DailySummary(
            epochDay = TimeUtils.epochDay(dayMillis),
            startMillis = start,
            endMillis = end,
            averageRisk = if (riskScores.isEmpty()) 0 else riskScores.average().toInt(),
            maxRisk = riskScores.maxOrNull() ?: 0,
            crisisCount = actualCrises.size,
            nearCrisisCount = near.size,
            checkInCount = dayCheckIns.size,
            averageSleepHours = if (sleep.isEmpty()) 0f else sleep.average().toFloat(),
            averageSoundOverload = if (dayWindows.isEmpty()) 0f
            else dayWindows.map { it.soundOverloadIndex }.average().toFloat(),
            averageAgitation = if (dayWindows.isEmpty()) 0f
            else dayWindows.map { it.agitationIndex }.average().toFloat(),
            dominantEmotions = emotions,
            topTriggers = triggers,
            timeline = buildTimeline(start, end, dayWindows, dayCheckIns, dayCrises, dayAnalyses)
        )
    }

    fun buildTimeline(
        dayStart: Long,
        dayEnd: Long,
        windows: List<SensorWindow>,
        checkIns: List<CheckIn>,
        crises: List<CrisisEvent>,
        analyses: List<GeminiAnalysis>
    ): List<TimelineItem> {
        val bucketMillis = 15 * 60_000L
        val items = mutableListOf<TimelineItem>()

        windows.groupBy { ((it.startMillis - dayStart) / bucketMillis).toInt() }
            .toSortedMap()
            .forEach { (bucket, group) ->
                if (group.isEmpty()) return@forEach
                val bucketStart = dayStart + bucket * bucketMillis
                val bucketEnd = minOf(bucketStart + bucketMillis - 1, dayEnd)
                items += TimelineItem.SensorItem(
                    SensorWindow(
                        startMillis = bucketStart,
                        endMillis = bucketEnd,
                        accelMean = group.map { it.accelMean }.average().toFloat(),
                        accelStd = group.map { it.accelStd }.average().toFloat(),
                        accelRms = group.map { it.accelRms }.average().toFloat(),
                        accelPeaks = group.sumOf { it.accelPeaks },
                        gyroMean = group.map { it.gyroMean }.average().toFloat(),
                        gyroStd = group.map { it.gyroStd }.average().toFloat(),
                        gyroRms = group.map { it.gyroRms }.average().toFloat(),
                        gyroPeaks = group.sumOf { it.gyroPeaks },
                        spectralEnergy = group.map { it.spectralEnergy }.average().toFloat(),
                        lightMeanLux = group.map { it.lightMeanLux }.average().toFloat(),
                        proximityNear = group.any { it.proximityNear },
                        agitationIndex = group.map { it.agitationIndex }.average().toFloat(),
                        micCaptured = group.any { it.micCaptured },
                        soundDbMean = group.map { it.soundDbMean }.average().toFloat(),
                        soundDbMax = group.maxOf { it.soundDbMax },
                        soundDbVariability = group.map { it.soundDbVariability }.average().toFloat(),
                        impulsivePeaks = group.sumOf { it.impulsivePeaks },
                        lowBandRatio = group.map { it.lowBandRatio }.average().toFloat(),
                        midBandRatio = group.map { it.midBandRatio }.average().toFloat(),
                        highBandRatio = group.map { it.highBandRatio }.average().toFloat(),
                        soundOverloadIndex = group.map { it.soundOverloadIndex }.average().toFloat(),
                        batteryPercent = group.last().batteryPercent,
                        riskScore = group.maxOf { it.riskScore }
                    )
                )
            }

        checkIns.forEach { items += TimelineItem.CheckInItem(it) }
        crises.forEach { items += TimelineItem.CrisisItem(it) }
        analyses.forEach { items += TimelineItem.AnalysisItem(it) }

        return items.sortedBy { it.timestamp }
    }

    fun dominantEmotionLabel(emotions: List<Emotion>): String =
        if (emotions.isEmpty()) "sem registro" else emotions.joinToString(", ") { it.label }
}
