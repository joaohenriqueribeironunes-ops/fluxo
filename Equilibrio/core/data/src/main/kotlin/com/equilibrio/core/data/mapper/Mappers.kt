package com.equilibrio.core.data.mapper

import com.equilibrio.core.data.db.AnalysisEntity
import com.equilibrio.core.data.db.BaselineEntity
import com.equilibrio.core.data.db.CalendarEventEntity
import com.equilibrio.core.data.db.CheckInEntity
import com.equilibrio.core.data.db.CrisisEntity
import com.equilibrio.core.data.db.PredictionEntity
import com.equilibrio.core.data.db.SensorWindowEntity
import com.equilibrio.core.model.BaselineProfile
import com.equilibrio.core.model.CalendarEvent
import com.equilibrio.core.model.CheckIn
import com.equilibrio.core.model.ConfidenceLevel
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.CrisisType
import com.equilibrio.core.model.Emotion
import com.equilibrio.core.model.ForecastSource
import com.equilibrio.core.model.GeminiAnalysis
import com.equilibrio.core.model.PredictionHorizon
import com.equilibrio.core.model.PredictionRecord
import com.equilibrio.core.model.PredictedOutcome
import com.equilibrio.core.model.RiskLevel
import com.equilibrio.core.model.SensorWindow

fun SensorWindowEntity.toModel() = SensorWindow(
    id = id,
    startMillis = startMillis,
    endMillis = endMillis,
    accelMean = accelMean,
    accelStd = accelStd,
    accelRms = accelRms,
    accelPeaks = accelPeaks,
    gyroMean = gyroMean,
    gyroStd = gyroStd,
    gyroRms = gyroRms,
    gyroPeaks = gyroPeaks,
    dominantFrequencyHz = dominantFrequencyHz,
    spectralEnergy = spectralEnergy,
    lightMeanLux = lightMeanLux,
    proximityNear = proximityNear,
    agitationIndex = agitationIndex,
    micCaptured = micCaptured,
    soundDbMean = soundDbMean,
    soundDbMax = soundDbMax,
    soundDbVariability = soundDbVariability,
    impulsivePeaks = impulsivePeaks,
    lowBandRatio = lowBandRatio,
    midBandRatio = midBandRatio,
    highBandRatio = highBandRatio,
    soundOverloadIndex = soundOverloadIndex,
    batteryPercent = batteryPercent,
    riskScore = riskScore
)

fun SensorWindow.toEntity() = SensorWindowEntity(
    id = id,
    startMillis = startMillis,
    endMillis = endMillis,
    accelMean = accelMean,
    accelStd = accelStd,
    accelRms = accelRms,
    accelPeaks = accelPeaks,
    gyroMean = gyroMean,
    gyroStd = gyroStd,
    gyroRms = gyroRms,
    gyroPeaks = gyroPeaks,
    dominantFrequencyHz = dominantFrequencyHz,
    spectralEnergy = spectralEnergy,
    lightMeanLux = lightMeanLux,
    proximityNear = proximityNear,
    agitationIndex = agitationIndex,
    micCaptured = micCaptured,
    soundDbMean = soundDbMean,
    soundDbMax = soundDbMax,
    soundDbVariability = soundDbVariability,
    impulsivePeaks = impulsivePeaks,
    lowBandRatio = lowBandRatio,
    midBandRatio = midBandRatio,
    highBandRatio = highBandRatio,
    soundOverloadIndex = soundOverloadIndex,
    batteryPercent = batteryPercent,
    riskScore = riskScore
)

fun CheckInEntity.toModel() = CheckIn(
    id = id,
    timestamp = timestamp,
    emotions = emotions.split("|").filter { it.isNotBlank() }
        .mapNotNull { runCatching { Emotion.valueOf(it) }.getOrNull() },
    emotionIntensity = emotionIntensity,
    hunger = hunger,
    sleepHours = sleepHours,
    pain = pain,
    tension = tension,
    sensorySensitivity = sensorySensitivity,
    consumed = Codec.decodeStrings(consumed),
    caffeine = caffeine,
    medication = Codec.decodeStrings(medication),
    supplements = Codec.decodeStrings(supplements),
    activities = Codec.decodeStrings(activities),
    location = location,
    people = Codec.decodeStrings(people),
    abruptTransitions = abruptTransitions,
    thoughts = thoughts,
    note = note,
    quick = quick,
    retroactive = retroactive
)

fun CheckIn.toEntity() = CheckInEntity(
    id = id,
    timestamp = timestamp,
    emotions = emotions.joinToString("|") { it.name },
    emotionIntensity = emotionIntensity,
    hunger = hunger,
    sleepHours = sleepHours,
    pain = pain,
    tension = tension,
    sensorySensitivity = sensorySensitivity,
    consumed = Codec.encodeStrings(consumed),
    caffeine = caffeine,
    medication = Codec.encodeStrings(medication),
    supplements = Codec.encodeStrings(supplements),
    activities = Codec.encodeStrings(activities),
    location = location,
    people = Codec.encodeStrings(people),
    abruptTransitions = abruptTransitions,
    thoughts = thoughts,
    note = note,
    quick = quick,
    retroactive = retroactive
)

fun CrisisEntity.toModel() = CrisisEvent(
    id = id,
    startMillis = startMillis,
    endMillis = endMillis,
    type = runCatching { CrisisType.valueOf(type) }
        .getOrDefault(CrisisType.SOBRECARGA_SENSORIAL),
    intensity = intensity,
    triggers = Codec.decodeStrings(triggers),
    helped = Codec.decodeStrings(helped),
    worsened = Codec.decodeStrings(worsened),
    recoveryMinutes = recoveryMinutes,
    notes = notes,
    retroactive = retroactive,
    debriefDone = debriefDone,
    sensorWindowId = sensorWindowId
)

fun CrisisEvent.toEntity() = CrisisEntity(
    id = id,
    startMillis = startMillis,
    endMillis = endMillis,
    type = type.name,
    intensity = intensity,
    triggers = Codec.encodeStrings(triggers),
    helped = Codec.encodeStrings(helped),
    worsened = Codec.encodeStrings(worsened),
    recoveryMinutes = recoveryMinutes,
    notes = notes,
    retroactive = retroactive,
    debriefDone = debriefDone,
    sensorWindowId = sensorWindowId
)

fun AnalysisEntity.toModel() = GeminiAnalysis(
    id = id,
    createdAt = createdAt,
    periodStart = periodStart,
    periodEnd = periodEnd,
    riskCurrent = riskCurrent,
    level = runCatching { RiskLevel.valueOf(level) }.getOrDefault(RiskLevel.BAIXO),
    triggers = Codec.decodeStrings(triggers),
    patterns = Codec.decodeStrings(patterns),
    analysisText = analysisText,
    suggestions = Codec.decodeStrings(suggestions),
    confidence = confidence,
    warnings = Codec.decodeStrings(warnings),
    forecasts = Codec.decodeForecasts(forecasts),
    recurringPatterns = Codec.decodeStrings(recurringPatterns),
    questions = Codec.decodeStrings(questions),
    model = model,
    crisisCountUsed = crisisCountUsed,
    daysOfData = daysOfData
)

fun GeminiAnalysis.toEntity() = AnalysisEntity(
    id = id,
    createdAt = createdAt,
    periodStart = periodStart,
    periodEnd = periodEnd,
    riskCurrent = riskCurrent,
    level = level.name,
    triggers = Codec.encodeStrings(triggers),
    patterns = Codec.encodeStrings(patterns),
    analysisText = analysisText,
    suggestions = Codec.encodeStrings(suggestions),
    confidence = confidence,
    warnings = Codec.encodeStrings(warnings),
    forecasts = Codec.encodeForecasts(forecasts),
    recurringPatterns = Codec.encodeStrings(recurringPatterns),
    questions = Codec.encodeStrings(questions),
    model = model,
    crisisCountUsed = crisisCountUsed,
    daysOfData = daysOfData
)

fun PredictionEntity.toModel() = PredictionRecord(
    id = id,
    createdAt = createdAt,
    horizon = runCatching { PredictionHorizon.valueOf(horizon) }
        .getOrDefault(PredictionHorizon.CURTO),
    windowStart = windowStart,
    windowEnd = windowEnd,
    probability = probability,
    crisisType = crisisType?.let { runCatching { CrisisType.valueOf(it) }.getOrNull() },
    confidence = runCatching { ConfidenceLevel.valueOf(confidence) }
        .getOrDefault(ConfidenceLevel.BAIXA),
    factors = Codec.decodeStrings(factors),
    plan = Codec.decodeStrings(plan),
    outcome = runCatching { PredictedOutcome.valueOf(outcome) }
        .getOrDefault(PredictedOutcome.PENDENTE),
    feedbackAt = feedbackAt,
    riskScore = riskScore,
    source = runCatching { ForecastSource.valueOf(source) }.getOrDefault(ForecastSource.LOCAL)
)

fun PredictionRecord.toEntity() = PredictionEntity(
    id = id,
    createdAt = createdAt,
    horizon = horizon.name,
    windowStart = windowStart,
    windowEnd = windowEnd,
    probability = probability,
    crisisType = crisisType?.name,
    confidence = confidence.name,
    factors = Codec.encodeStrings(factors),
    plan = Codec.encodeStrings(plan),
    outcome = outcome.name,
    feedbackAt = feedbackAt,
    riskScore = riskScore,
    source = source.name
)

fun BaselineEntity.toModel() = BaselineProfile(
    startedAt = startedAt,
    daysObserved = daysObserved,
    sampleCount = sampleCount,
    stats = Codec.decodeStats(stats),
    ready = ready
)

fun BaselineProfile.toEntity() = BaselineEntity(
    startedAt = startedAt,
    daysObserved = daysObserved,
    sampleCount = sampleCount,
    stats = Codec.encodeStats(stats),
    ready = ready
)

fun CalendarEventEntity.toModel() = CalendarEvent(
    id = id,
    calendarEventId = calendarEventId,
    title = title,
    startMillis = startMillis,
    endMillis = endMillis,
    allDay = allDay,
    importedAt = importedAt
)

fun CalendarEvent.toEntity() = CalendarEventEntity(
    id = id,
    calendarEventId = calendarEventId,
    title = title,
    startMillis = startMillis,
    endMillis = endMillis,
    allDay = allDay,
    importedAt = importedAt
)
