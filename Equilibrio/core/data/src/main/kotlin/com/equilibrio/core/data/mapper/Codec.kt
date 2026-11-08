package com.equilibrio.core.data.mapper

import com.equilibrio.core.model.BaselineStat
import com.equilibrio.core.model.ConfidenceLevel
import com.equilibrio.core.model.CrisisType
import com.equilibrio.core.model.ForecastSource
import com.equilibrio.core.model.RiskForecast
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Serializable
data class ForecastDto(
    val start: Long,
    val end: Long,
    val type: String? = null,
    val probability: Float,
    val confidence: String,
    val factors: List<String> = emptyList(),
    val plan: List<String> = emptyList(),
    val source: String = "LOCAL"
)

@Serializable
data class BaselineStatDto(
    val mean: Double,
    val std: Double,
    val count: Int
)

object Codec {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
        isLenient = true
    }

    private val stringList = ListSerializer(String.serializer())
    private val forecastList = ListSerializer(ForecastDto.serializer())
    private val statMap = MapSerializer(String.serializer(), BaselineStatDto.serializer())

    fun encodeStrings(values: List<String>): String = json.encodeToString(stringList, values)

    fun decodeStrings(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString(stringList, raw) }
            .getOrElse { raw.split("|").map { it.trim() }.filter { it.isNotEmpty() } }
    }

    fun encodeForecasts(forecasts: List<RiskForecast>): String =
        json.encodeToString(forecastList, forecasts.map { it.toDto() })

    fun decodeForecasts(raw: String?): List<RiskForecast> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(forecastList, raw).map { it.toModel() }
        }.getOrElse { emptyList() }
    }

    fun encodeStats(stats: Map<String, BaselineStat>): String =
        json.encodeToString(
            statMap,
            stats.mapValues { (_, v) -> BaselineStatDto(v.mean, v.std, v.count) }
        )

    fun decodeStats(raw: String?): Map<String, BaselineStat> {
        if (raw.isNullOrBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString(statMap, raw)
                .mapValues { (_, v) -> BaselineStat(v.mean, v.std, v.count) }
        }.getOrElse { emptyMap() }
    }

    private fun RiskForecast.toDto() = ForecastDto(
        start = windowStart,
        end = windowEnd,
        type = crisisType?.name,
        probability = probability,
        confidence = confidence.name,
        factors = factors,
        plan = preventivePlan,
        source = source.name
    )

    private fun ForecastDto.toModel() = RiskForecast(
        windowStart = start,
        windowEnd = end,
        crisisType = type?.let { runCatching { CrisisType.valueOf(it) }.getOrNull() },
        probability = probability,
        confidence = runCatching { ConfidenceLevel.valueOf(confidence) }
            .getOrDefault(ConfidenceLevel.BAIXA),
        factors = factors,
        preventivePlan = plan,
        source = runCatching { ForecastSource.valueOf(source) }.getOrDefault(ForecastSource.LOCAL)
    )
}
