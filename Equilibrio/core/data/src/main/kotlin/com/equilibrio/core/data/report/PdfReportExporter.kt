package com.equilibrio.core.data.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.equilibrio.core.data.db.AnalysisDao
import com.equilibrio.core.data.db.BaselineDao
import com.equilibrio.core.data.db.CheckInDao
import com.equilibrio.core.data.db.CrisisDao
import com.equilibrio.core.data.db.PredictionDao
import com.equilibrio.core.data.db.SensorWindowDao
import com.equilibrio.core.data.mapper.toModel
import com.equilibrio.core.domain.repository.ReportExporter
import com.equilibrio.core.domain.repository.SettingsRepository
import com.equilibrio.core.domain.time.TimeUtils
import com.equilibrio.core.model.AccuracyReport
import com.equilibrio.core.model.DailySummary
import com.equilibrio.core.model.GeminiAnalysis
import com.equilibrio.core.model.TimelineItem
import com.equilibrio.core.model.WeeklySummary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfReportExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sensorDao: SensorWindowDao,
    private val checkInDao: CheckInDao,
    private val crisisDao: CrisisDao,
    private val analysisDao: AnalysisDao,
    private val predictionDao: PredictionDao,
    private val baselineDao: BaselineDao,
    private val settings: SettingsRepository
) : ReportExporter {

    private val dateTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val dateOnly = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    override suspend fun exportDailyPdf(
        summary: DailySummary,
        analysis: GeminiAnalysis?,
        accuracy: AccuracyReport?
    ): File = withContext(Dispatchers.IO) {
        val file = reportFile("equilibrio_diario_${summary.epochDay}.pdf")
        val document = PdfDocument()
        val writer = PageWriter(document)
        val dayLabel = Instant.ofEpochMilli(summary.startMillis)
            .atZone(ZoneId.systemDefault()).format(dateOnly)

        writer.startPage()
        writer.brand("Equilíbrio")
        writer.title("Relatório diário — $dayLabel")
        writer.muted("Gerado localmente em ${nowLabel()}. Nenhum áudio bruto é armazenado.")
        writer.divider()

        writer.heading("Resumo do dia")
        writer.line("Risco médio: ${summary.averageRisk}/100   ·   Risco máximo: ${summary.maxRisk}/100")
        writer.line("Crises: ${summary.crisisCount}   ·   Quase-crises: ${summary.nearCrisisCount}   ·   Check-ins: ${summary.checkInCount}")
        writer.line("Sono médio registrado: ${format1(summary.averageSleepHours)} h")
        writer.line("Sobrecarga sonora média: ${summary.averageSoundOverload.toInt()}/100   ·   Agitação média: ${summary.averageAgitation.toInt()}/100")
        if (summary.dominantEmotions.isNotEmpty()) {
            writer.line("Emoções mais registradas: ${summary.dominantEmotions.joinToString(", ") { it.label }}")
        }
        if (summary.topTriggers.isNotEmpty()) {
            writer.line("Gatilhos citados: ${summary.topTriggers.joinToString(", ")}")
        }
        writer.spacer()

        writer.heading("Linha do tempo")
        if (summary.timeline.isEmpty()) {
            writer.muted("Sem dados registrados neste dia.")
        } else {
            summary.timeline.take(140).forEach { item ->
                writer.line(timelineLine(item), size = 9f, spacing = 2f)
            }
            if (summary.timeline.size > 140) {
                writer.muted("Linha do tempo truncada em 140 itens para manter o PDF legível.")
            }
        }
        writer.spacer()

        if (analysis != null) {
            writer.heading("Análise assistida por IA (Gemini)")
            writer.muted(
                "Baseada em ${analysis.crisisCountUsed} crises e ${analysis.daysOfData} dias de dados. " +
                    "Confiança informada: ${(analysis.confidence * 100).toInt()}%."
            )
            writer.paragraph(analysis.analysisText)
            if (analysis.triggers.isNotEmpty()) {
                writer.subheading("Gatilhos prováveis")
                analysis.triggers.forEach { writer.bullet(it) }
            }
            if (analysis.patterns.isNotEmpty()) {
                writer.subheading("Padrões detectados")
                analysis.patterns.forEach { writer.bullet(it) }
            }
            if (analysis.suggestions.isNotEmpty()) {
                writer.subheading("Sugestões de regulação")
                analysis.suggestions.forEach { writer.bullet(it) }
            }
            if (analysis.forecasts.isNotEmpty()) {
                writer.subheading("Janelas de risco estimadas")
                analysis.forecasts.forEach { f ->
                    val start = Instant.ofEpochMilli(f.windowStart).atZone(ZoneId.systemDefault())
                    val end = Instant.ofEpochMilli(f.windowEnd).atZone(ZoneId.systemDefault())
                    writer.bullet(
                        "${start.format(dateTime)} a ${end.format(dateTime)} · " +
                            "${f.percent}% · ${f.crisisType?.label ?: "tipo incerto"} · confiança ${f.confidence.label}"
                    )
                }
            }
            if (analysis.warnings.isNotEmpty()) {
                writer.subheading("Avisos e limitações")
                analysis.warnings.forEach { writer.bullet(it) }
            }
        } else {
            writer.heading("Análise assistida por IA")
            writer.muted("Nenhuma análise Gemini disponível para este dia.")
        }

        if (accuracy != null && accuracy.hasData) {
            writer.heading("Precisão do app (período)")
            writer.line("Crises: ${accuracy.totalCrises} · previstas: ${accuracy.predictedCrises} · perdidas: ${accuracy.missedCrises} · falsos alarmes: ${accuracy.falseAlarms}")
            writer.line("Antecedência média: ${accuracy.meanLeadMinutes} min · recall: ${(accuracy.recall * 100).toInt()}% · Brier: ${format2(accuracy.brierScore)}")
            writer.muted("Métricas calculadas apenas sobre dados locais. Podem variar com pouco histórico.")
        }

        writer.spacer()
        writer.divider()
        writer.muted(
            "Este relatório é uma ferramenta de autoconhecimento e NÃO substitui avaliação, " +
                "diagnóstico ou acompanhamento profissional. Em crise com risco à segurança, " +
                "procure alguém de confiança, CVV 188 ou SAMU 192."
        )
        writer.close()
        document.writeTo(file.outputStream())
        document.close()
        file
    }

    override suspend fun exportWeeklyPdf(
        summary: WeeklySummary,
        analysis: GeminiAnalysis?
    ): File = withContext(Dispatchers.IO) {
        val file = reportFile("equilibrio_semanal_${summary.startMillis}.pdf")
        val document = PdfDocument()
        val writer = PageWriter(document)

        writer.startPage()
        writer.brand("Equilíbrio")
        writer.title(
            "Relatório semanal — ${Instant.ofEpochMilli(summary.startMillis).atZone(ZoneId.systemDefault()).format(dateOnly)} " +
                "a ${Instant.ofEpochMilli(summary.endMillis).atZone(ZoneId.systemDefault()).format(dateOnly)}"
        )
        writer.muted("Gerado localmente em ${nowLabel()}.")
        writer.divider()

        writer.heading("Resumo")
        writer.line("Crises: ${summary.crisisCount} · previstas com antecedência: ${summary.predictedCount}")
        writer.line(
            "Precisão: ${summary.accuracy.predictedCrises}/${summary.accuracy.totalCrises} crises previstas · " +
                "${summary.accuracy.falseAlarms} falsos alarmes · antecedência média ${summary.accuracy.meanLeadMinutes} min"
        )
        writer.spacer()

        writer.heading("Dia a dia")
        summary.days.forEach { day ->
            val label = Instant.ofEpochMilli(day.startMillis).atZone(ZoneId.systemDefault()).format(dateOnly)
            writer.line(
                "$label — risco médio ${day.averageRisk}, máximo ${day.maxRisk}; " +
                    "${day.crisisCount} crise(s), ${day.nearCrisisCount} quase-crise(s), ${day.checkInCount} check-in(s); " +
                    "sono ${format1(day.averageSleepHours)} h"
            )
        }
        writer.spacer()

        if (summary.topPatterns.isNotEmpty()) {
            writer.heading("Padrões recorrentes")
            summary.topPatterns.forEach { writer.bullet(it) }
        }

        if (analysis != null) {
            writer.heading("Análise assistida por IA (Gemini)")
            writer.muted("Baseada em ${analysis.crisisCountUsed} crises e ${analysis.daysOfData} dias. Estimativas, não certezas.")
            writer.paragraph(analysis.analysisText)
            if (analysis.forecasts.isNotEmpty()) {
                writer.subheading("Próximas janelas de risco")
                analysis.forecasts.forEach { f ->
                    writer.bullet(
                        "${Instant.ofEpochMilli(f.windowStart).atZone(ZoneId.systemDefault()).format(dateTime)} · " +
                            "${f.percent}% · ${f.crisisType?.label ?: "tipo incerto"} · confiança ${f.confidence.label}"
                    )
                }
            }
            if (analysis.questions.isNotEmpty()) {
                writer.subheading("Perguntas para observar")
                analysis.questions.forEach { writer.bullet(it) }
            }
        }

        writer.spacer()
        writer.divider()
        writer.muted(
            "Ferramenta de autoconhecimento. Não substitui avaliação profissional. " +
                "Em risco à segurança: CVV 188 (24 h) ou SAMU 192."
        )
        writer.close()
        document.writeTo(file.outputStream())
        document.close()
        file
    }

    override suspend fun exportJson(): File = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val sensors = sensorDao.between(0, Long.MAX_VALUE).map { it.toModel() }
        val checkIns = checkInDao.between(0, Long.MAX_VALUE).map { it.toModel() }
        val crises = crisisDao.all().map { it.toModel() }
        val analyses = analysisDao.recent(10_000).map { it.toModel() }
        val predictions = predictionDao.recent(10_000).map { it.toModel() }
        val baseline = baselineDao.get()?.toModel()
        val current = settings.current()

        val root = buildJsonObject {
            put("schema_version", 1)
            put("exported_at", now)
            put("disclaimer", "Dados pessoais de saúde. Compartilhe apenas com quem você confia.")
            putJsonObject("settings") {
                put("cycle_seconds", current.cycleSeconds)
                put("window_seconds", current.windowSeconds)
                put("baseline_days", current.baselineDays)
                put("alert_threshold", current.alertThreshold)
                put("gemini_consent", current.geminiConsent)
            }
            baseline?.let {
                putJsonObject("baseline") {
                    put("started_at", it.startedAt)
                    put("days_observed", it.daysObserved)
                    put("sample_count", it.sampleCount)
                    put("ready", it.ready)
                }
            }
            putJsonArray("sensor_windows") {
                sensors.forEach { w ->
                    add(buildJsonObject {
                        put("start", w.startMillis)
                        put("end", w.endMillis)
                        put("agitation", w.agitationIndex)
                        put("sound_overload", w.soundOverloadIndex)
                        put("db_mean", w.soundDbMean)
                        put("db_max", w.soundDbMax)
                        put("impulsive_peaks", w.impulsivePeaks)
                        put("high_band", w.highBandRatio)
                        put("light_lux", w.lightMeanLux)
                        put("proximity_near", w.proximityNear)
                        put("risk", w.riskScore)
                    })
                }
            }
            putJsonArray("check_ins") {
                checkIns.forEach { c ->
                    add(buildJsonObject {
                        put("timestamp", c.timestamp)
                        put("emotions", c.emotions.joinToString(",") { it.name })
                        put("intensity", c.emotionIntensity)
                        put("sleep_hours", c.sleepHours)
                        put("hunger", c.hunger)
                        put("pain", c.pain)
                        put("tension", c.tension)
                        put("sensory", c.sensorySensitivity)
                        put("consumed", c.consumed.joinToString(" | "))
                        put("caffeine", c.caffeine)
                        put("medication", c.medication.joinToString(" | "))
                        put("activities", c.activities.joinToString(" | "))
                        put("transitions", c.abruptTransitions)
                        put("thoughts", c.thoughts)
                        put("note", c.note)
                        put("quick", c.quick)
                        put("retroactive", c.retroactive)
                    })
                }
            }
            putJsonArray("crises") {
                crises.forEach { c ->
                    add(buildJsonObject {
                        put("start", c.startMillis)
                        put("end", c.endMillis ?: 0L)
                        put("type", c.type.name)
                        put("intensity", c.intensity)
                        put("triggers", c.triggers.joinToString(" | "))
                        put("helped", c.helped.joinToString(" | "))
                        put("worsened", c.worsened.joinToString(" | "))
                        put("recovery_minutes", c.recoveryMinutes)
                        put("notes", c.notes)
                        put("retroactive", c.retroactive)
                        put("debrief_done", c.debriefDone)
                    })
                }
            }
            putJsonArray("analyses") {
                analyses.forEach { a ->
                    add(buildJsonObject {
                        put("created_at", a.createdAt)
                        put("period_start", a.periodStart)
                        put("period_end", a.periodEnd)
                        put("risk", a.riskCurrent)
                        put("level", a.level.name)
                        put("analysis", a.analysisText)
                        put("triggers", a.triggers.joinToString(" | "))
                        put("suggestions", a.suggestions.joinToString(" | "))
                        put("confidence", a.confidence)
                        put("model", a.model)
                    })
                }
            }
            putJsonArray("predictions") {
                predictions.forEach { p ->
                    add(buildJsonObject {
                        put("created_at", p.createdAt)
                        put("window_start", p.windowStart)
                        put("window_end", p.windowEnd)
                        put("probability", p.probability)
                        put("outcome", p.outcome.name)
                        put("risk", p.riskScore)
                    })
                }
            }
        }

        val dir = File(context.filesDir, "exports").apply { mkdirs() }
        val file = File(dir, "equilibrio_backup_$now.json")
        file.writeText(root.toString(), Charsets.UTF_8)
        file
    }

    private fun reportFile(name: String): File {
        val dir = File(context.filesDir, "reports").apply { mkdirs() }
        return File(dir, name)
    }

    private fun nowLabel(): String =
        Instant.now().atZone(ZoneId.systemDefault()).format(dateTime)

    private fun format1(value: Float): String = "%.1f".format(value)

    private fun format2(value: Float): String = "%.2f".format(value)

    private fun timelineLine(item: TimelineItem): String = when (item) {
        is TimelineItem.SensorItem -> {
            val w = item.window
            "${TimeUtils.formatHm(w.startMillis)}  Sensores — risco ${w.riskScore} · " +
                "som ${w.soundDbMean.toInt()} dB · agitação ${w.agitationIndex.toInt()} · " +
                "luz ${w.lightMeanLux.toInt()} lx"
        }
        is TimelineItem.CheckInItem -> {
            val c = item.checkIn
            "${TimeUtils.formatHm(c.timestamp)}  Check-in — " +
                "${c.emotions.joinToString(", ") { it.label }.ifBlank { "sem emoção marcada" }} · " +
                "intensidade ${c.emotionIntensity}/10 · sono ${format1(c.sleepHours)} h"
        }
        is TimelineItem.CrisisItem -> {
            val c = item.crisis
            "${TimeUtils.formatHm(c.startMillis)}  CRISE — ${c.type.label} · " +
                "intensidade ${c.intensity}/10" +
                if (c.triggers.isNotEmpty()) " · gatilhos: ${c.triggers.joinToString(", ")}" else ""
        }
        is TimelineItem.AnalysisItem -> {
            "${TimeUtils.formatHm(item.analysis.createdAt)}  Análise Gemini — " +
                "risco ${item.analysis.riskCurrent} (${item.analysis.level.label})"
        }
        is TimelineItem.ForecastItem -> {
            "${TimeUtils.formatHm(item.forecast.windowStart)}  Janela de risco prevista — ${item.forecast.percent}%"
        }
    }

    private class PageWriter(private val document: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var canvas: Canvas? = null
        private var y = 0f
        private var pageNumber = 0
        private val margin = 44f
        private val pageWidth = 595
        private val pageHeight = 842
        private val contentWidth = pageWidth - 2 * margin

        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 32, 36)
            textSize = 10f
        }

        private fun startPageInternal() {
            finishPage()
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val newPage = document.startPage(info)
            page = newPage
            canvas = newPage.canvas
            y = margin
        }

        private fun finishPage() {
            page?.let { document.finishPage(it) }
            page = null
            canvas = null
        }

        fun startPage() = startPageInternal()

        private fun ensureSpace(height: Float) {
            if (page == null || y + height > pageHeight - margin) startPageInternal()
        }

        private fun drawLines(
            text: String,
            size: Float,
            typeface: Typeface,
            color: Int,
            spacing: Float,
            bullet: Boolean = false
        ) {
            paint.textSize = size
            paint.typeface = typeface
            paint.color = color
            val prefix = if (bullet) "•  " else ""
            val available = contentWidth - if (bullet) 12f else 0f
            val lines = wrap(prefix + text, available)
            val lineHeight = size * 1.35f
            ensureSpace(lineHeight * lines.size + spacing)
            for ((index, line) in lines.withIndex()) {
                ensureSpace(lineHeight)
                val x = margin + if (bullet && index > 0) 12f else 0f
                canvas?.drawText(line, x, y + size, paint)
                y += lineHeight
            }
            y += spacing
        }

        fun brand(text: String) = drawLines(text, 10f, Typeface.SANS_SERIF, Color.rgb(47, 111, 106), 2f)
        fun title(text: String) = drawLines(text, 18f, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD), Color.rgb(20, 28, 30), 6f)
        fun heading(text: String) = drawLines(text, 13f, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD), Color.rgb(47, 111, 106), 4f)
        fun subheading(text: String) = drawLines(text, 10.5f, Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD), Color.rgb(38, 48, 52), 3f)
        fun line(text: String, size: Float = 10f, spacing: Float = 3f) =
            drawLines(text, size, Typeface.SANS_SERIF, Color.rgb(38, 48, 52), spacing)
        fun paragraph(text: String) =
            drawLines(text, 9.5f, Typeface.SANS_SERIF, Color.rgb(38, 48, 52), 6f)
        fun muted(text: String) =
            drawLines(text, 8.5f, Typeface.SANS_SERIF, Color.rgb(96, 106, 112), 4f)
        fun bullet(text: String) =
            drawLines(text, 9.5f, Typeface.SANS_SERIF, Color.rgb(38, 48, 52), 2f, bullet = true)
        fun spacer() { y += 6f }

        fun divider() {
            ensureSpace(10f)
            paint.color = Color.rgb(210, 218, 220)
            paint.strokeWidth = 0.8f
            canvas?.drawLine(margin, y, pageWidth - margin, y, paint)
            y += 10f
        }

        fun close() = finishPage()

        private fun wrap(text: String, width: Float): List<String> {
            if (text.isBlank()) return listOf("")
            val result = mutableListOf<String>()
            val line = StringBuilder()
            for (word in text.replace("\n", " \n ").split(" ")) {
                if (word == "\n") {
                    result += line.toString()
                    line.clear()
                    continue
                }
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= width) {
                    line.clear()
                    line.append(candidate)
                } else {
                    if (line.isNotEmpty()) result += line.toString()
                    line.clear()
                    if (paint.measureText(word) <= width) {
                        line.append(word)
                    } else {
                        var chunk = StringBuilder()
                        for (char in word) {
                            if (paint.measureText(chunk.toString() + char) > width && chunk.isNotEmpty()) {
                                result += chunk.toString()
                                chunk = StringBuilder()
                            }
                            chunk.append(char)
                        }
                        line.append(chunk)
                    }
                }
            }
            if (line.isNotEmpty()) result += line.toString()
            return result.ifEmpty { listOf("") }
        }
    }
}
