package com.equilibrio.core.data.gemini

import com.equilibrio.core.domain.gemini.GeminiPayload
import com.equilibrio.core.model.ConfidenceLevel
import com.equilibrio.core.model.CrisisType
import com.equilibrio.core.model.GeminiAnalysis
import com.equilibrio.core.model.RiskForecast
import com.equilibrio.core.model.RiskLevel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object GeminiJson {

    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun iso(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(iso)

    fun payloadToJson(payload: GeminiPayload): String = buildJsonObject {
        put("gerado_em", iso(payload.generatedAt))
        putJsonObject("periodo") {
            put("inicio", iso(payload.periodStart))
            put("fim", iso(payload.periodEnd))
        }
        put("dias_de_dados", payload.daysOfData)
        put("total_crises_historicas", payload.totalCrises)
        putJsonObject("baseline_pessoal") {
            put("pronto", payload.baselineReady)
            put("dias", payload.baselineDays)
            put("amostras", payload.baselineSamples)
        }
        putJsonArray("janelas_agregadas") {
            payload.windowAggregates.forEach { w ->
                addJsonObject {
                    put("inicio", iso(w.startMillis))
                    put("fim", iso(w.endMillis))
                    put("agitacao_motora", w.agitation)
                    put("sobrecarga_sonora", w.soundOverload)
                    put("db_medio", w.soundDbMean)
                    put("db_max", w.soundDbMax)
                    put("ratio_agudos", w.highBandRatio)
                    put("picos_impulsivos", w.impulsivePeaks)
                    put("luz_lux", w.lightLux)
                    put("proximidade_perto", w.proximityNear)
                    put("risco_local", w.riskScore)
                }
            }
        }
        putJsonArray("registros_manuais") {
            payload.checkIns.forEach { c ->
                addJsonObject {
                    put("horario", iso(c.timestamp))
                    putJsonArray("emocoes") { c.emotions.forEach { add(it) } }
                    put("intensidade_emocional", c.intensity)
                    put("horas_sono", c.sleepHours)
                    put("fome", c.hunger)
                    put("dor", c.pain)
                    put("tensao", c.tension)
                    put("sensibilidade_sensorial", c.sensorySensitivity)
                    putJsonArray("consumido") { c.consumed.forEach { add(it) } }
                    put("cafeina", c.caffeine)
                    putJsonArray("medicacao") { c.medication.forEach { add(it) } }
                    putJsonArray("atividades") { c.activities.forEach { add(it) } }
                    put("transicoes_bruscas", c.transitions)
                }
            }
        }
        putJsonArray("crises") {
            payload.crises.forEach { c ->
                addJsonObject {
                    put("inicio", iso(c.startMillis))
                    put("tipo", c.type)
                    put("intensidade", c.intensity)
                    put("duracao_minutos", c.durationMinutes)
                    putJsonArray("gatilhos") { c.triggers.forEach { add(it) } }
                    putJsonArray("o_que_ajudou") { c.helped.forEach { add(it) } }
                    put("recuperacao_minutos", c.recoveryMinutes)
                    put("retroativa", c.retroactive)
                }
            }
        }
        putJsonArray("resumos_diarios") {
            payload.daily.forEach { d ->
                addJsonObject {
                    put("dia_epoch", d.epochDay)
                    put("risco_medio", d.averageRisk)
                    put("risco_maximo", d.maxRisk)
                    put("crises", d.crises)
                    put("quase_crises", d.nearCrises)
                    put("checkins", d.checkIns)
                    put("horas_sono", d.sleepHours)
                    put("sobrecarga_sonora_media", d.soundOverload)
                    put("agitacao_media", d.agitation)
                    putJsonArray("emocoes_dominantes") { d.emotions.forEach { add(it) } }
                    putJsonArray("gatilhos") { d.triggers.forEach { add(it) } }
                }
            }
        }
        putJsonArray("previsoes_locais") {
            payload.localForecasts.forEach { f ->
                addJsonObject {
                    put("inicio", iso(f.windowStart))
                    put("fim", iso(f.windowEnd))
                    put("tipo_provavel", f.crisisType)
                    put("probabilidade", f.probability)
                    put("confianca", f.confidence)
                    putJsonArray("fatores") { f.factors.forEach { add(it) } }
                    put("observacao", "gerada por modelo local simples no aparelho")
                }
            }
        }
        putJsonArray("padroes_locais") { payload.localPatterns.forEach { add(it) } }
        putJsonArray("achados_locais") { payload.localFindings.forEach { add(it) } }
        put("aviso_analise", payload.analysisDisclaimer)
        payload.accuracy?.let { a ->
            putJsonObject("precisao_local") {
                put("crises_totais", a.totalCrises)
                put("crises_previstas", a.predictedCrises)
                put("falsos_alarmes", a.falseAlarms)
                put("antecedencia_media_min", a.meanLeadMinutes)
                put("brier", a.brierScore)
                put("recall", a.recall)
                put("precisao", a.precision)
            }
        }
        putJsonArray("agenda") {
            payload.calendarEvents.forEach { e ->
                addJsonObject {
                    put("titulo", e.title)
                    put("inicio", iso(e.startMillis))
                    put("fim", iso(e.endMillis))
                    put("dia_inteiro", e.allDay)
                }
            }
        }
    }.toString()

    fun parse(text: String, model: String, payload: GeminiPayload): GeminiAnalysis {
        val root = json.parseToJsonElement(extractJson(text)).jsonObject

        fun string(key: String): String? =
            root[key]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" }

        fun int(key: String, default: Int = 0): Int =
            root[key]?.jsonPrimitive?.intOrNull ?: default

        fun float(key: String, default: Float = 0f): Float =
            root[key]?.jsonPrimitive?.floatOrNull ?: default

        fun stringList(key: String): List<String> =
            (root[key] as? JsonArray)?.mapNotNull { it.jsonPrimitive.content?.takeIf { s -> s.isNotBlank() } }
                ?: emptyList()

        val level = when (string("nivel")?.lowercase()) {
            "alto" -> RiskLevel.ALTO
            "moderado", "médio", "medio" -> RiskLevel.MODERADO
            else -> RiskLevel.BAIXO
        }

        val forecasts = (root["previsoes"] as? JsonArray)?.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val start = parseIso(obj["inicio"]?.jsonPrimitive?.content) ?: return@mapNotNull null
            val end = parseIso(obj["fim"]?.jsonPrimitive?.content) ?: return@mapNotNull null
            var probability = obj["probabilidade"]?.jsonPrimitive?.floatOrNull ?: 0f
            if (probability > 1f) probability /= 100f
            val confidence = when (obj["confianca"]?.jsonPrimitive?.content?.lowercase()) {
                "alta" -> ConfidenceLevel.ALTA
                "media", "média" -> ConfidenceLevel.MEDIA
                else -> ConfidenceLevel.BAIXA
            }
            val factors = (obj["fatores"] as? JsonArray)
                ?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            val plan = (obj["plano_preventivo"] as? JsonArray)
                ?.mapNotNull { it.jsonPrimitive.content } ?: emptyList()
            RiskForecast(
                windowStart = start,
                windowEnd = end,
                crisisType = crisisTypeFrom(obj["tipo_provavel"]?.jsonPrimitive?.content),
                probability = probability.coerceIn(0.01f, 0.99f),
                confidence = confidence,
                factors = factors.take(3),
                preventivePlan = plan.take(4),
                source = com.equilibrio.core.model.ForecastSource.GEMINI
            )
        } ?: emptyList()

        var confidence = float("confianca", 0f)
        if (confidence > 1f) confidence /= 100f
        confidence = confidence.coerceIn(0f, 1f)

        val warnings = sanitizeWarnings(stringList("avisos"), payload)

        val analysisText = string("analise_do_periodo")
            ?: string("analise")
            ?: "Análise indisponível."

        return GeminiAnalysis(
            createdAt = System.currentTimeMillis(),
            periodStart = payload.periodStart,
            periodEnd = payload.periodEnd,
            riskCurrent = int("risco_atual", 0).coerceIn(0, 100),
            level = level,
            triggers = stringList("gatilhos_provaveis"),
            patterns = stringList("padroes_detectados"),
            analysisText = analysisText.take(4000),
            suggestions = stringList("sugestoes_regulacao"),
            confidence = confidence,
            warnings = warnings,
            forecasts = forecasts,
            recurringPatterns = stringList("padroes_recorrentes"),
            questions = stringList("perguntas_para_o_usuario").take(4),
            model = model,
            crisisCountUsed = payload.totalCrises,
            daysOfData = payload.daysOfData
        )
    }

    private fun sanitizeWarnings(raw: List<String>, payload: GeminiPayload): List<String> {
        val list = raw.toMutableList()
        val standard = "Ferramenta de autoconhecimento. Não substitui avaliação, diagnóstico ou acompanhamento profissional."
        if (list.none { it.contains("não substitui", ignoreCase = true) }) list += standard
        if (payload.totalCrises < 5) {
            list += "Dados insuficientes: há menos de 5 crises registradas; as conclusões são preliminares."
        }
        if (!payload.baselineReady) {
            list += "Baseline pessoal ainda em aprendizado (menos de 14 dias); as estimativas tendem a ser imprecisas."
        }
        val accuracy = payload.accuracy
        if (accuracy != null && accuracy.totalCrises >= 5 && accuracy.recall < 0.5f) {
            list += "O modelo local acertou menos da metade das crises no período avaliado; use as previsões com cautela."
        }
        return list.distinct()
    }

    private fun crisisTypeFrom(label: String?): CrisisType? {
        val l = label?.lowercase() ?: return null
        return when {
            l.contains("meltdown") -> CrisisType.MELTDOWN
            l.contains("shutdown") -> CrisisType.SHUTDOWN
            l.contains("quase") -> CrisisType.QUASE_CRISE
            l.contains("tdah") || l.contains("paralisia") -> CrisisType.SOBRECARGA_TDAH
            l.contains("sensorial") || l.contains("sobrecarga") -> CrisisType.SOBRECARGA_SENSORIAL
            else -> null
        }
    }

    fun parseIso(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
            .getOrElse {
                runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            }
    }

    fun extractJson(text: String): String {
        var s = text.trim()
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        require(start >= 0 && end > start) { "Resposta sem objeto JSON" }
        return s.substring(start, end + 1)
    }

    /** Schema enviado ao Gemini via proxy (response_schema). */
    fun responseSchema(): JsonObject = buildJsonObject {
        put("type", "OBJECT")
        putJsonObject("properties") {
            putJsonObject("risco_atual") { put("type", "INTEGER") }
            putJsonObject("nivel") { put("type", "STRING"); put("enum", buildJsonArray { add("baixo"); add("moderado"); add("alto") }) }
            stringArray("gatilhos_provaveis")
            stringArray("padroes_detectados")
            putJsonObject("analise_do_periodo") { put("type", "STRING") }
            stringArray("sugestoes_regulacao")
            putJsonObject("confianca") { put("type", "NUMBER") }
            stringArray("avisos")
            putJsonObject("previsoes") {
                put("type", "ARRAY")
                putJsonObject("items") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("inicio") { put("type", "STRING") }
                        putJsonObject("fim") { put("type", "STRING") }
                        putJsonObject("tipo_provavel") { put("type", "STRING") }
                        putJsonObject("probabilidade") { put("type", "NUMBER") }
                        putJsonObject("confianca") { put("type", "STRING"); put("enum", buildJsonArray { add("baixa"); add("media"); add("alta") }) }
                        stringArray("fatores")
                        stringArray("plano_preventivo")
                    }
                    put("required", buildJsonArray {
                        add("inicio"); add("fim"); add("tipo_provavel"); add("probabilidade"); add("confianca"); add("fatores"); add("plano_preventivo")
                    })
                }
            }
            stringArray("padroes_recorrentes")
            stringArray("perguntas_para_o_usuario")
        }
        put("required", buildJsonArray {
            add("risco_atual"); add("nivel"); add("gatilhos_provaveis"); add("padroes_detectados")
            add("analise_do_periodo"); add("sugestoes_regulacao"); add("confianca"); add("avisos")
            add("previsoes"); add("padroes_recorrentes"); add("perguntas_para_o_usuario")
        })
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.stringArray(name: String) {
        putJsonObject(name) {
            put("type", "ARRAY")
            putJsonObject("items") { put("type", "STRING") }
        }
    }
}
