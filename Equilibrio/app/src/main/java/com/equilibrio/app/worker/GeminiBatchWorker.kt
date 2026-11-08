package com.equilibrio.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.equilibrio.core.domain.gemini.GeminiPayloadBuilder
import com.equilibrio.core.domain.insights.InsightsEngine
import com.equilibrio.core.domain.prediction.ForecastEngine
import com.equilibrio.core.domain.prediction.RiskEngine
import com.equilibrio.core.domain.report.DailySummaryBuilder
import com.equilibrio.core.domain.repository.AnalysisRepository
import com.equilibrio.core.domain.repository.BaselineRepository
import com.equilibrio.core.domain.repository.CalendarRepository
import com.equilibrio.core.domain.repository.CheckInRepository
import com.equilibrio.core.domain.repository.CrisisRepository
import com.equilibrio.core.domain.repository.GeminiClient
import com.equilibrio.core.domain.repository.PredictionRepository
import com.equilibrio.core.domain.repository.SensorRepository
import com.equilibrio.core.domain.repository.SettingsRepository
import com.equilibrio.core.domain.time.TimeUtils
import com.equilibrio.core.model.DailySummary
import com.equilibrio.core.model.ForecastSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Envia ao Gemini apenas agregados: janelas de 15–30 min, check-ins, crises e
 * previsões locais. Nunca envia áudio, formas de onda ou amostras de sensor.
 */
@HiltWorker
class GeminiBatchWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val settingsRepository: SettingsRepository,
    private val sensorRepository: SensorRepository,
    private val checkInRepository: CheckInRepository,
    private val crisisRepository: CrisisRepository,
    private val analysisRepository: AnalysisRepository,
    private val predictionRepository: PredictionRepository,
    private val baselineRepository: BaselineRepository,
    private val calendarRepository: CalendarRepository,
    private val geminiClient: GeminiClient
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.current()
        if (!settings.geminiEnabled || !settings.geminiConsent) return Result.success()

        val now = System.currentTimeMillis()
        val force = inputData.getBoolean(KEY_FORCE, false)
        if (!force && now - settings.lastGeminiBatchAt < settings.geminiBatchMinutes * 60_000L) {
            return Result.success()
        }

        return runCatching {
            val periodStart = now - 24 * 3_600_000L
            val windows = sensorRepository.windowsBetween(periodStart - 12 * 3_600_000L, now)
            val checkIns = checkInRepository.between(now - 48 * 3_600_000L, now)
            val crises = crisisRepository.recent(200)
            val baseline = baselineRepository.load()
            val accuracy = predictionRepository.accuracy(now - 28 * 86_400_000L, now)
            val insights = InsightsEngine().build(windows, checkIns, crises, now, 28, baseline)

            val daily: List<DailySummary> = (0..6).map { offset ->
                DailySummaryBuilder.build(
                    dayMillis = now - offset * 86_400_000L,
                    windows = windows,
                    checkIns = checkIns,
                    crises = crises
                )
            }.reversed()

            val recentWindows = windows.takeLast(240)
            val risk = baseline?.takeIf { it.ready }?.let { profile ->
                RiskEngine().evaluate(
                    recentWindows = recentWindows,
                    todayCheckIns = checkIns,
                    recentCrises = crises,
                    profile = profile,
                    now = now,
                    micActive = false
                )
            }
            val forecasts = if (risk != null) {
                val engine = ForecastEngine()
                engine.shortTerm(risk, recentWindows, crises, now) +
                    engine.today(risk, crises, now)
            } else {
                emptyList()
            }

            val calendarEvents = if (settings.calendarConsent) {
                calendarRepository
                    .observeEvents(now - 24 * 3_600_000L, now + 48 * 3_600_000L)
                    .first()
            } else {
                emptyList()
            }

            val payload = GeminiPayloadBuilder.build(
                now = now,
                windows = windows,
                checkIns = checkIns,
                crises = crises,
                dailySummaries = daily,
                localForecasts = forecasts.map {
                    it.copy(source = ForecastSource.LOCAL)
                },
                insights = insights,
                baseline = baseline,
                accuracy = accuracy,
                calendarEvents = calendarEvents,
                periodStart = periodStart,
                periodEnd = now,
                bucketMinutes = 30
            )

            if (GeminiPayloadBuilder.isEmpty(payload)) return Result.success()

            val result = geminiClient.analyze(payload)
            result.fold(
                onSuccess = { analysis ->
                    runCatching { analysisRepository.insert(analysis) }
                    runCatching { settingsRepository.update { it.copy(lastGeminiBatchAt = now) } }
                    Result.success()
                },
                onFailure = {
                    if (runAttemptCount < 2) Result.retry() else {
                        // Fallback local: preserva utilidade sem depender da rede/JSON do Gemini.
                        runCatching {
                            analysisRepository.insert(localFallbackAnalysis(now, periodStart, insights, payload.daysOfData, payload.totalCrises))
                            settingsRepository.update { it.copy(lastGeminiBatchAt = now) }
                        }
                        Result.success()
                    }
                }
            )
        }.getOrElse {
            if (runAttemptCount < 2) Result.retry() else {
                runCatching {
                    val periodStart = now - 24 * 3_600_000L
                    analysisRepository.insert(localFallbackAnalysis(now, periodStart, null, 0, 0))
                }
                Result.success()
            }
        }
    }

    private fun localFallbackAnalysis(
        now: Long,
        periodStart: Long,
        insights: com.equilibrio.core.domain.insights.InsightsBundle?,
        daysOfData: Int,
        crisisCount: Int
    ): com.equilibrio.core.model.GeminiAnalysis {
        val patterns = (insights?.recurringPatterns.orEmpty() +
            insights?.correlations.orEmpty().take(4).map { it.description } +
            insights?.anomalies.orEmpty().take(2).map { it.description }).take(8)
        val warnings = buildList {
            add("Análise local de padrões (fallback). Não é diagnóstico e não substitui avaliação profissional.")
            add("A análise com IA não estava disponível ou retornou resposta inválida; estes achados vêm do motor local.")
            if (crisisCount < 5) add("Dados insuficientes: menos de 5 eventos de crise registrados.")
            addAll(insights?.dataLimitations.orEmpty())
        }.distinct()
        return com.equilibrio.core.model.GeminiAnalysis(
            createdAt = now,
            periodStart = periodStart,
            periodEnd = now,
            riskCurrent = 0,
            level = com.equilibrio.core.model.RiskLevel.BAIXO,
            triggers = insights?.topTriggers.orEmpty().take(5).map { it.first },
            patterns = patterns,
            analysisText = if (patterns.isEmpty()) "Ainda não há dados suficientes para identificar padrões locais confiáveis. Continue registrando para fortalecer a análise." else "Resumo local de padrões com base nos dados registrados. Correlações são observacionais e não indicam causalidade.",
            suggestions = listOf("Revise os padrões com calma e observe o contexto.", "Use pausas sensoriais e regulação de baixo risco quando notar sinais precoces."),
            confidence = if (crisisCount >= 5) 0.35f else 0.15f,
            warnings = warnings,
            forecasts = emptyList(),
            recurringPatterns = insights?.recurringPatterns.orEmpty().take(6),
            questions = listOf("O que estava diferente nos momentos mais calmos?", "Quais sinais precoces você percebeu antes dos eventos?"),
            model = "local-fallback",
            crisisCountUsed = crisisCount,
            daysOfData = daysOfData
        )
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}
