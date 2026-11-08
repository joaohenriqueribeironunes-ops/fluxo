package com.equilibrio.app.ui

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.equilibrio.app.crash.CrashReporter
import com.equilibrio.app.service.CollectionBus
import com.equilibrio.app.service.CollectionService
import com.equilibrio.app.service.EquilibrioTileService
import com.equilibrio.app.worker.WorkScheduler
import com.equilibrio.core.data.privacy.DataWipeManager
import com.equilibrio.core.data.security.KeystoreSecretStore
import com.equilibrio.core.domain.gemini.GeminiPayloadBuilder
import com.equilibrio.core.domain.insights.InsightsBundle
import com.equilibrio.core.domain.insights.InsightsEngine
import com.equilibrio.core.domain.prediction.AccuracyCalculator
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
import com.equilibrio.core.domain.repository.ReportExporter
import com.equilibrio.core.domain.repository.SensorRepository
import com.equilibrio.core.domain.repository.SettingsRepository
import com.equilibrio.core.domain.repository.TimelineRepository
import com.equilibrio.core.domain.time.TimeUtils
import com.equilibrio.core.model.AccuracyReport
import com.equilibrio.core.model.CheckIn
import com.equilibrio.core.model.CrisisEvent
import com.equilibrio.core.model.CrisisType
import com.equilibrio.core.model.DailySummary
import com.equilibrio.core.model.GeminiAnalysis
import com.equilibrio.core.model.LocalRisk
import com.equilibrio.core.model.PredictedOutcome
import com.equilibrio.core.model.PredictionRecord
import com.equilibrio.core.model.RiskForecast
import com.equilibrio.core.model.TimelineItem
import com.equilibrio.core.model.UserSettings
import com.equilibrio.core.model.WeeklySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val sensorRepository: SensorRepository,
    private val checkInRepository: CheckInRepository,
    private val crisisRepository: CrisisRepository,
    private val analysisRepository: AnalysisRepository,
    private val predictionRepository: PredictionRepository,
    private val baselineRepository: BaselineRepository,
    private val timelineRepository: TimelineRepository,
    private val calendarRepository: CalendarRepository,
    private val reportExporter: ReportExporter,
    private val geminiClient: GeminiClient,
    private val dataWipeManager: DataWipeManager,
    private val scheduler: WorkScheduler,
    private val bus: CollectionBus,
    private val secrets: KeystoreSecretStore
) : ViewModel() {

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    val settings: StateFlow<UserSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val collection = bus.state

    val risk: StateFlow<LocalRisk?> = bus.state
        .map { it.risk }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val selectedDay = MutableStateFlow(TimeUtils.epochDay(System.currentTimeMillis()))

    val timeline: StateFlow<List<TimelineItem>> = selectedDay
        .flatMapLatest { timelineRepository.observeDay(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailySummary: StateFlow<DailySummary?> = selectedDay
        .flatMapLatest { day ->
            val start = TimeUtils.epochDayToMillis(day)
            val end = TimeUtils.endOfDay(start)
            combine(
                sensorRepository.observeWindows(start, end),
                checkInRepository.observeBetween(start, end),
                crisisRepository.observeBetween(start, end),
                analysisRepository.observeBetween(start, end)
            ) { windows, checkIns, crisisList, analysisList ->
                DailySummaryBuilder.build(start, windows, checkIns, crisisList, analysisList)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val crises: StateFlow<List<CrisisEvent>> = crisisRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val analyses: StateFlow<List<GeminiAnalysis>> = analysisRepository.observeRecent(12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val predictions: StateFlow<List<PredictionRecord>> = predictionRepository.observeRecent(120)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val accuracy: StateFlow<AccuracyReport?> = combine(
        predictionRepository.observeRecent(500),
        crisisRepository.observeAll()
    ) { records, crisisList ->
        val now = System.currentTimeMillis()
        AccuracyCalculator.compute(records, crisisList, now - 28 * 86_400_000L, now)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _insights = MutableStateFlow<InsightsBundle?>(null)
    val insights: StateFlow<InsightsBundle?> = _insights

    private val _shortTerm = MutableStateFlow<List<RiskForecast>>(emptyList())
    val shortTerm: StateFlow<List<RiskForecast>> = _shortTerm

    private val _todayForecasts = MutableStateFlow<List<RiskForecast>>(emptyList())
    val todayForecasts: StateFlow<List<RiskForecast>> = _todayForecasts

    private val _weekForecasts = MutableStateFlow<List<RiskForecast>>(emptyList())
    val weekForecasts: StateFlow<List<RiskForecast>> = _weekForecasts

    private val _lastExport = MutableStateFlow<File?>(null)
    val lastExport: StateFlow<File?> = _lastExport

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages

    private val riskEngine = RiskEngine()
    private val forecastEngine = ForecastEngine()

    init {
        viewModelScope.launch {
            settingsRepository.settings.first()
            _loaded.value = true
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { scheduler.scheduleAll(it) }
        }
        viewModelScope.launch {
            bus.state.map { it.risk }.distinctUntilChanged().collect {
                recomputeForecasts()
            }
        }
        viewModelScope.launch {
            crisisRepository.observeAll().collect { recomputeForecasts() }
        }
        viewModelScope.launch {
            bootstrapRiskFromStorage()
            refreshInsights()
        }
    }

    private suspend fun bootstrapRiskFromStorage() {
        if (bus.state.value.risk != null) return
        val profile = runCatching { baselineRepository.load() }.getOrNull() ?: return
        if (!profile.ready) return
        val now = System.currentTimeMillis()
        val windows = sensorRepository.recent(180)
        if (windows.isEmpty()) return
        val risk = riskEngine.evaluate(
            recentWindows = windows,
            todayCheckIns = checkInRepository.recent(40),
            recentCrises = crisisRepository.recent(120),
            profile = profile,
            now = now,
            micActive = false
        )
        bus.updateRisk(risk)
    }

    // ---------- Coleta ----------

    fun startCollection() {
        runCatching {
            ContextCompat.startForegroundService(
                appContext,
                CollectionService.startIntent(appContext)
            )
        }.onFailure {
            _messages.tryEmit("Não foi possível iniciar a coleta: ${it.message}")
        }
        viewModelScope.launch {
            settingsRepository.update { it.copy(collectionEnabled = true) }
        }
        EquilibrioTileService.requestRefresh(appContext)
    }

    fun pauseCollection() {
        runCatching { appContext.startService(CollectionService.pauseIntent(appContext)) }
    }

    fun stopCollection() {
        runCatching { appContext.startService(CollectionService.stopIntent(appContext)) }
        viewModelScope.launch {
            settingsRepository.update { it.copy(collectionEnabled = false) }
        }
        EquilibrioTileService.requestRefresh(appContext)
    }

    fun toggleCollection(enabled: Boolean) {
        if (enabled) startCollection() else stopCollection()
    }

    // ---------- Onboarding e configurações ----------

    fun completeOnboarding() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            settingsRepository.update { current ->
                current.copy(
                    onboardingDone = true,
                    collectionEnabled = true,
                    baselineStartMillis = if (current.baselineStartMillis <= 0L) now
                    else current.baselineStartMillis
                )
            }
            startCollection()
        }
    }

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun saveGeminiApiKey(key: String) {
        secrets.setGeminiApiKey(key)
        _messages.tryEmit(
            if (key.isBlank()) "Chave do Gemini removida do Keystore."
            else "Chave salva no Android Keystore deste aparelho."
        )
    }

    fun hasGeminiKey(): Boolean = runCatching { secrets.hasGeminiApiKey() }.getOrDefault(false)

    fun importCalendar() {
        viewModelScope.launch {
            val imported = runCatching { calendarRepository.importRecent(30) }.getOrDefault(0)
            _messages.tryEmit(
                if (imported > 0) "$imported compromissos importados (somente título e horário)."
                else "Nada importado. Verifique a permissão de calendário."
            )
        }
    }

    fun requestGeminiNow() {
        val current = settings.value
        if (!current.geminiConsent) {
            _messages.tryEmit("Autorize o envio de resumos agregados antes de pedir a análise.")
            return
        }
        scheduler.enqueueGeminiNow()
        _messages.tryEmit("Análise solicitada. Ela roda em segundo plano e aparece nos insights.")
    }

    // ---------- Check-in e crises ----------

    fun saveCheckIn(checkIn: CheckIn, alsoCrisis: CrisisType? = null) {
        viewModelScope.launch {
            runCatching { checkInRepository.insert(checkIn) }
            if (alsoCrisis != null) {
                val id = crisisRepository.insert(
                    CrisisEvent(
                        startMillis = checkIn.timestamp,
                        type = alsoCrisis,
                        intensity = checkIn.emotionIntensity.coerceIn(1, 10),
                        triggers = checkIn.activities,
                        notes = checkIn.thoughts
                    )
                )
                resolveOverlappingPredictions(alsoCrisis)
                if (alsoCrisis != CrisisType.QUASE_CRISE) {
                    scheduler.scheduleDebrief(id, 30)
                }
            }
            refreshInsights()
            recomputeForecasts()
            _messages.tryEmit("Registro salvo. Obrigado por cuidar de você.")
        }
    }

    fun registerCrisis(
        type: CrisisType,
        intensity: Int,
        triggers: List<String> = emptyList(),
        helped: List<String> = emptyList(),
        startMillis: Long = System.currentTimeMillis(),
        retroactive: Boolean = false,
        notes: String = "",
        recoveryMinutes: Int = 0
    ) {
        viewModelScope.launch {
            val id = crisisRepository.insert(
                CrisisEvent(
                    startMillis = startMillis,
                    type = type,
                    intensity = intensity.coerceIn(1, 10),
                    triggers = triggers.distinct(),
                    helped = helped.distinct(),
                    recoveryMinutes = recoveryMinutes.coerceAtLeast(0),
                    notes = notes,
                    retroactive = retroactive
                )
            )
            resolveOverlappingPredictions(type)
            if (!retroactive && type != CrisisType.QUASE_CRISE) {
                scheduler.scheduleDebrief(id, 30)
            }
            refreshInsights()
            recomputeForecasts()
            _messages.tryEmit(
                if (retroactive) "Crise passada registrada. Isso ajuda o aprendizado."
                else "Crise registrada. Nada de julgamento: isso é dado, não falha."
            )
        }
    }

    fun finishCrisis(
        crisisId: Long,
        triggers: List<String>,
        helped: List<String>,
        recoveryMinutes: Int,
        notes: String,
        endMillis: Long = System.currentTimeMillis()
    ) {
        val crisis = crises.value.firstOrNull { it.id == crisisId } ?: return
        viewModelScope.launch {
            crisisRepository.update(
                crisis.copy(
                    endMillis = maxOf(endMillis, crisis.startMillis),
                    triggers = (crisis.triggers + triggers).distinct(),
                    helped = helped.distinct(),
                    recoveryMinutes = recoveryMinutes.coerceAtLeast(0),
                    notes = notes.ifBlank { crisis.notes },
                    debriefDone = true
                )
            )
            refreshInsights()
            _messages.tryEmit("Debrief salvo. Ele vira aprendizado para o seu modelo.")
        }
    }

    private suspend fun resolveOverlappingPredictions(type: CrisisType) {
        val now = System.currentTimeMillis()
        val outcome = if (type == CrisisType.QUASE_CRISE) PredictedOutcome.QUASE_CRISE
        else PredictedOutcome.CRISE
        predictionRepository.recent(80)
            .filter { it.outcome == PredictedOutcome.PENDENTE && now in it.windowStart..it.windowEnd }
            .forEach {
                runCatching {
                    predictionRepository.update(it.copy(outcome = outcome, feedbackAt = now))
                }
            }
    }

    fun markPrediction(record: PredictionRecord, outcome: PredictedOutcome) {
        viewModelScope.launch {
            predictionRepository.update(
                record.copy(outcome = outcome, feedbackAt = System.currentTimeMillis())
            )
            _messages.tryEmit("Feedback registrado. Vai calibrar as próximas estimativas.")
        }
    }

    fun markLatestFeedback(outcome: PredictedOutcome) {
        val now = System.currentTimeMillis()
        val target = predictions.value.firstOrNull {
            it.outcome == PredictedOutcome.PENDENTE &&
                it.createdAt > now - 6 * 3_600_000L
        }
        if (target != null) {
            markPrediction(target, outcome)
        } else {
            _messages.tryEmit("Nenhuma previsão recente para avaliar.")
        }
    }

    // ---------- Previsões e insights ----------

    private suspend fun recomputeForecasts() {
        val currentRisk = bus.state.value.risk ?: return
        val now = System.currentTimeMillis()
        val windows = runCatching { sensorRepository.recent(180) }.getOrDefault(emptyList())
        val crisisList = runCatching { crisisRepository.recent(150) }.getOrDefault(emptyList())
        _shortTerm.value = runCatching {
            forecastEngine.shortTerm(currentRisk, windows, crisisList, now)
        }.getOrDefault(emptyList())
        _todayForecasts.value = runCatching {
            forecastEngine.today(currentRisk, crisisList, now)
        }.getOrDefault(emptyList())
        _weekForecasts.value = runCatching {
            forecastEngine.week(crisisList, now)
        }.getOrDefault(emptyList())
    }

    fun refreshInsights() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val windows = runCatching { sensorRepository.recent(3_000) }.getOrDefault(emptyList())
            val checkIns = runCatching { checkInRepository.recent(500) }.getOrDefault(emptyList())
            val crisisList = runCatching { crisisRepository.all() }.getOrDefault(emptyList())
            _insights.value = withContext(Dispatchers.Default) {
                val baseline = baselineRepository.load()
                InsightsEngine().build(windows, checkIns, crisisList, now, 28, baseline)
            }
        }
    }

    // ---------- Relatórios ----------

    fun exportDaily() {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val start = TimeUtils.startOfDay(now)
                val end = TimeUtils.endOfDay(now)
                val windows = sensorRepository.windowsBetween(start, end)
                val checkIns = checkInRepository.between(start, end)
                val crisisList = crisisRepository.between(start, end)
                val analysis = analysisRepository.recent(1).firstOrNull()
                val summary = DailySummaryBuilder.build(
                    dayMillis = now,
                    windows = windows,
                    checkIns = checkIns,
                    crises = crisisList,
                    analyses = analysis?.let { listOf(it) } ?: emptyList()
                )
                reportExporter.exportDailyPdf(summary, analysis, accuracy.value)
            }.onSuccess {
                _lastExport.value = it
                _messages.tryEmit("PDF diário pronto: ${it.name}")
            }.onFailure {
                _messages.tryEmit("Não foi possível gerar o PDF: ${it.message}")
            }
        }
    }

    fun exportWeekly() {
        viewModelScope.launch {
            runCatching {
                val now = System.currentTimeMillis()
                val windows = sensorRepository.windowsBetween(now - 7 * 86_400_000L, now)
                val checkIns = checkInRepository.between(now - 7 * 86_400_000L, now)
                val crisisList = crisisRepository.between(now - 7 * 86_400_000L, now)
                val days: List<DailySummary> = (6 downTo 0).map { offset ->
                    DailySummaryBuilder.build(
                        dayMillis = now - offset * 86_400_000L,
                        windows = windows,
                        checkIns = checkIns,
                        crises = crisisList
                    )
                }
                val start = days.first().startMillis
                val end = days.last().endMillis
                val report = accuracy.value ?: AccuracyReport(start, end)
                val weekly = WeeklySummary(
                    startMillis = start,
                    endMillis = end,
                    days = days,
                    crisisCount = days.sumOf { it.crisisCount },
                    predictedCount = report.predictedCrises,
                    topPatterns = _insights.value?.recurringPatterns ?: emptyList(),
                    accuracy = report
                )
                reportExporter.exportWeeklyPdf(weekly, analysisRepository.recent(1).firstOrNull())
            }.onSuccess {
                _lastExport.value = it
                _messages.tryEmit("PDF semanal pronto: ${it.name}")
            }.onFailure {
                _messages.tryEmit("Não foi possível gerar o PDF: ${it.message}")
            }
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            runCatching { reportExporter.exportJson() }
                .onSuccess {
                    _lastExport.value = it
                    _messages.tryEmit("Backup JSON pronto: ${it.name}")
                }
                .onFailure {
                    _messages.tryEmit("Não foi possível exportar: ${it.message}")
                }
        }
    }

    fun hasCrashLog(): Boolean = runCatching { CrashReporter.hasLog(appContext) }.getOrDefault(false)

    fun crashLogIntent(): android.content.Intent? = runCatching { CrashReporter.exportIntent(appContext) }.getOrNull()

    fun clearCrashLog() {
        runCatching { CrashReporter.clear(appContext) }
        _messages.tryEmit("Log de erros apagado.")
    }

    fun notifyRecentCrashIfAny() {
        val at = runCatching { CrashReporter.consumeLastCrashAt(appContext) }.getOrDefault(0L)
        if (at > 0L) {
            _messages.tryEmit("O app fechou inesperadamente antes. Você pode exportar o log de erros em Configurações.")
        }
    }

    // ---------- Privacidade ----------

    fun wipeAllData() {
        viewModelScope.launch {
            runCatching {
                stopCollection()
                scheduler.cancelAll()
                dataWipeManager.wipeAll()
            }.onSuccess {
                bus.reset()
                _messages.tryEmit("Todos os dados de saúde foram apagados. O app está zerado.")
            }.onFailure {
                _messages.tryEmit("Falha ao apagar: ${it.message}")
            }
        }
    }

    // ---------- Navegação do dia ----------

    fun selectDay(epochDay: Long) {
        selectedDay.value = epochDay
    }

    fun shiftDay(days: Long) {
        val today = TimeUtils.epochDay(System.currentTimeMillis())
        selectedDay.value = (selectedDay.value + days).coerceAtMost(today)
    }

    /** Mantido para uso futuro de análise sob demanda na própria tela. */
    suspend fun analyzeNow(): Result<GeminiAnalysis>? {
        val current = settings.value
        if (!current.geminiConsent) return null
        val now = System.currentTimeMillis()
        val windows = sensorRepository.windowsBetween(now - 36 * 3_600_000L, now)
        val checkIns = checkInRepository.between(now - 48 * 3_600_000L, now)
        val crisisList = crisisRepository.recent(200)
        val baseline = baselineRepository.load()
        val payload = GeminiPayloadBuilder.build(
            now = now,
            windows = windows,
            checkIns = checkIns,
            crises = crisisList,
            dailySummaries = (0..6).map {
                DailySummaryBuilder.build(now - it * 86_400_000L, windows, checkIns, crisisList)
            }.reversed(),
            localForecasts = _todayForecasts.value,
            insights = _insights.value,
            baseline = baseline,
            accuracy = accuracy.value,
            calendarEvents = emptyList(),
            periodStart = now - 24 * 3_600_000L,
            periodEnd = now
        )
        return geminiClient.analyze(payload).onSuccess { analysisRepository.insert(it) }
    }
}
