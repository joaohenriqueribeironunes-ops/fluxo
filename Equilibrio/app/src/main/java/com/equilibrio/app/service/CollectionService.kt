package com.equilibrio.app.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.equilibrio.app.service.EquilibrioTileService
import com.equilibrio.app.worker.WorkScheduler
import com.equilibrio.core.domain.feature.SensorWindowAccumulator
import com.equilibrio.core.domain.feature.SoundFeatures
import com.equilibrio.core.domain.feature.SoundWindowAccumulator
import com.equilibrio.core.domain.prediction.BaselineEngine
import com.equilibrio.core.domain.prediction.ForecastEngine
import com.equilibrio.core.domain.prediction.RiskEngine
import com.equilibrio.core.domain.repository.BaselineRepository
import com.equilibrio.core.domain.repository.CheckInRepository
import com.equilibrio.core.domain.repository.CrisisRepository
import com.equilibrio.core.domain.repository.PredictionRepository
import com.equilibrio.core.domain.repository.SensorRepository
import com.equilibrio.core.domain.repository.SettingsRepository
import com.equilibrio.core.domain.time.TimeUtils
import com.equilibrio.core.model.CollectionStatus
import com.equilibrio.core.model.ConfidenceLevel
import com.equilibrio.core.model.PredictionHorizon
import com.equilibrio.core.model.PredictionRecord
import com.equilibrio.core.model.SensorWindow
import com.equilibrio.core.model.UserSettings
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

/**
 * Coleta passiva em primeiro plano. Ciclo padrão de 30 s: 15 s de sensores +
 * microfone, 15 s de pausa total (sem sensores e sem áudio).
 *
 * O áudio é processado em memória e descartado no fim da janela. Apenas
 * agregados (dB, bandas, impulsividade, índice de sobrecarga) são persistidos.
 */
@AndroidEntryPoint
class CollectionService : Service(), SensorEventListener {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var sensorRepository: SensorRepository
    @Inject lateinit var checkInRepository: CheckInRepository
    @Inject lateinit var crisisRepository: CrisisRepository
    @Inject lateinit var baselineRepository: BaselineRepository
    @Inject lateinit var predictionRepository: PredictionRepository
    @Inject lateinit var notifications: NotificationHelper
    @Inject lateinit var bus: CollectionBus
    @Inject lateinit var scheduler: WorkScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val riskEngine = RiskEngine()
    private val forecastEngine = ForecastEngine()
    private val baselineEngine = BaselineEngine()
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    private var loopJob: Job? = null
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var lightSensor: Sensor? = null
    private var proximitySensor: Sensor? = null

    private var accumulator = SensorWindowAccumulator()
    @Volatile private var windowCollecting = false
    @Volatile private var audioActive = false
    private var audioThread: Thread? = null

    private var settingsCache: UserSettings? = null
    private var windowsSinceBaseline = 0
    private var lastPredictionSavedAt = 0L

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannels()
        sensorManager = getSystemService(SensorManager::class.java)
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        lightSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch {
                    runCatching { settingsRepository.update { it.copy(collectionEnabled = false) } }
                }
                shutdown(stopService = true)
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> {
                pauseCollection()
                return START_STICKY
            }
            else -> startCollection()
        }
        return START_STICKY
    }

    private fun refreshTile() = runCatching { EquilibrioTileService.requestRefresh(this) }

    private fun startCollection() {
        startForegroundCompat(paused = false)
        if (loopJob?.isActive == true) {
            refreshTile()
            return
        }
        bus.setRunning(true)
        loopJob = scope.launch { runLoop() }
        refreshTile()
    }

    private suspend fun runLoop() {
        var initialized = false
        while (currentCoroutineContext().isActive) {
            val settings = runCatching { settingsRepository.current() }
                .getOrElse { settingsCache ?: UserSettings() }
            settingsCache = settings

            if (!initialized) {
                initialized = true
                if (settings.baselineStartMillis <= 0L) {
                    settingsRepository.update {
                        it.copy(
                            baselineStartMillis = System.currentTimeMillis(),
                            collectionEnabled = true
                        )
                    }
                }
                scheduler.scheduleAll(settings)
            }

            if (!settings.collectionEnabled) {
                bus.setRunning(false)
                notifications.updateOngoing(
                    "Coleta desativada nas configurações",
                    batteryPercent(),
                    paused = true,
                    micActive = false
                )
                delay(5_000)
                continue
            }

            val battery = batteryPercent()
            bus.setBattery(battery)
            if (battery in 0..(settings.batteryThreshold - 1)) {
                bus.setLowBattery(true, battery)
                notifications.updateOngoing(
                    "Bateria $battery% · coleta pausada automaticamente",
                    battery,
                    paused = true,
                    micActive = false
                )
                delay(30_000)
                continue
            }
            bus.setLowBattery(false, battery)

            val cycleStart = SystemClock.elapsedRealtime()
            runCatching { collectWindow(settings, battery) }
                .onFailure { error ->
                    bus.setError(error.message)
                    notifications.updateOngoing(
                        "Coleta instável · tentando novamente",
                        battery,
                        paused = false,
                        micActive = false
                    )
                }

            val cycleMillis = settings.cycleSeconds.coerceIn(20, 120) * 1000L
            val elapsed = SystemClock.elapsedRealtime() - cycleStart
            delay((cycleMillis - elapsed).coerceAtLeast(1_000L))
        }
    }

    private suspend fun collectWindow(settings: UserSettings, battery: Int) {
        val start = System.currentTimeMillis()
        accumulator = SensorWindowAccumulator(deviceRateHz = 50.0, targetRateHz = 25.0)
        windowCollecting = true
        registerSensors()

        val soundAccumulator = startAudio()
        bus.setMicActive(soundAccumulator != null)
        notifications.updateOngoing(
            if (soundAccumulator != null) {
                "Janela de ${settings.windowSeconds}s · microfone processando e descartando"
            } else {
                "Janela de ${settings.windowSeconds}s · sensores de movimento e luz"
            },
            battery,
            paused = false,
            micActive = soundAccumulator != null
        )

        delay(settings.windowSeconds.coerceIn(5, 60) * 1000L)

        windowCollecting = false
        unregisterSensors()
        stopAudio()
        bus.setMicActive(false)

        val end = System.currentTimeMillis()
        val base = accumulator.finish(start, end, battery)
        val features: SoundFeatures? = soundAccumulator?.finish()
        val window = if (features != null && features.dbMean > 0f) {
            base.copy(
                micCaptured = true,
                soundDbMean = features.dbMean,
                soundDbMax = features.dbMax,
                soundDbVariability = features.dbVariability,
                impulsivePeaks = features.impulsivePeaks,
                lowBandRatio = features.lowBandRatio,
                midBandRatio = features.midBandRatio,
                highBandRatio = features.highBandRatio,
                soundOverloadIndex = features.overloadIndex
            )
        } else {
            base.copy(micCaptured = false)
        }
        processWindow(window, settings)
    }

    private suspend fun processWindow(window: SensorWindow, settings: UserSettings) {
        val now = System.currentTimeMillis()
        val previous = sensorRepository.recent(180).sortedBy { it.startMillis }
        val recent = if (previous.size >= 180) previous.takeLast(179) + window else previous + window
        val checkIns = checkInRepository.recent(40)
        val crises = crisisRepository.recent(120)
        val profile = baselineRepository.load()

        val risk = riskEngine.evaluate(
            recentWindows = recent,
            todayCheckIns = checkIns,
            recentCrises = crises,
            profile = profile,
            now = now,
            micActive = window.micCaptured
        )
        val scored = window.copy(riskScore = risk.score)
        sensorRepository.insert(scored)
        bus.updateWindow(scored)
        bus.updateRisk(risk)

        windowsSinceBaseline++
        if (profile == null || windowsSinceBaseline >= 10) {
            val baselineStart = settings.baselineStartMillis.takeIf { it > 0L } ?: now
            val updated = baselineEngine.update(
                current = profile,
                windows = listOf(scored),
                now = now,
                fallbackStart = baselineStart
            )
            baselineRepository.save(updated)
            windowsSinceBaseline = 0
        }

        maybeSavePrediction(risk, recent, crises, now)
        maybeAlert(settings, risk, now)

        val status = when {
            !risk.baselineReady -> "Aprendendo seus padrões · ${(risk.baselineProgress * 100).toInt()}%"
            risk.status == CollectionStatus.BATERIA_BAIXA -> "Bateria baixa · pausado"
            else -> "Risco ${risk.score}/100 · ${risk.level.label.lowercase()}"
        }
        notifications.updateOngoing(status, batteryPercent(), paused = false, micActive = false)
    }

    private suspend fun maybeSavePrediction(
        risk: com.equilibrio.core.model.LocalRisk,
        recent: List<SensorWindow>,
        crises: List<com.equilibrio.core.model.CrisisEvent>,
        now: Long
    ) {
        if (!risk.baselineReady || risk.score < 30) return
        if (now - lastPredictionSavedAt < 20 * 60_000L) return
        lastPredictionSavedAt = now
        val type = forecastEngine.likelyType(risk, recent, crises, now)
        val confidence = when {
            crises.size >= 20 -> ConfidenceLevel.MEDIA
            crises.size >= 8 -> ConfidenceLevel.BAIXA
            else -> ConfidenceLevel.BAIXA
        }
        runCatching {
            predictionRepository.insert(
                PredictionRecord(
                    createdAt = now,
                    horizon = PredictionHorizon.CURTO,
                    windowStart = now,
                    windowEnd = now + 60 * 60_000L,
                    probability = (risk.score / 100f).coerceIn(0.02f, 0.95f),
                    crisisType = type,
                    confidence = confidence,
                    factors = risk.factors.map { it.label },
                    plan = forecastEngine.preventivePlan(risk),
                    riskScore = risk.score
                )
            )
        }
    }

    private suspend fun maybeAlert(
        settings: UserSettings,
        risk: com.equilibrio.core.model.LocalRisk,
        now: Long
    ) {
        if (!risk.baselineReady || risk.score < settings.alertThreshold) return
        if (settings.quietPredictions) return

        val today = TimeUtils.epochDay(now)
        val alertsToday = if (settings.alertsDayEpoch == today) settings.alertsToday else 0
        if (alertsToday >= settings.maxAlertsPerDay) return
        if (now - settings.lastAlertAt < 30 * 60_000L) return
        if (now < settings.lastAlertAt) return

        notifications.showAlert(risk, forecastEngine.preventivePlan(risk))
        runCatching {
            settingsRepository.update {
                it.copy(
                    alertsToday = alertsToday + 1,
                    alertsDayEpoch = today,
                    lastAlertAt = now
                )
            }
        }
    }

    private fun pauseCollection() {
        loopJob?.cancel()
        loopJob = null
        windowCollecting = false
        unregisterSensors()
        stopAudio()
        bus.setMicActive(false)
        bus.setRunning(false)
        notifications.updateOngoing(
            "Coleta pausada",
            batteryPercent(),
            paused = true,
            micActive = false
        )
        refreshTile()
    }

    private fun shutdown(stopService: Boolean) {
        loopJob?.cancel()
        loopJob = null
        windowCollecting = false
        unregisterSensors()
        stopAudio()
        bus.setMicActive(false)
        bus.setRunning(false)
        notifications.cancelAlert()
        refreshTile()
        if (stopService) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startForegroundCompat(paused: Boolean) {
        val micGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val type = when {
            Build.VERSION.SDK_INT >= 34 -> {
                var value = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                if (micGranted) value = value or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                value
            }
            Build.VERSION.SDK_INT >= 30 && micGranted ->
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            else -> 0
        }

        val notification = notifications.ongoingNotification(
            statusText = if (paused) "Coleta pausada" else "Preparando coleta passiva",
            batteryPercent = batteryPercent(),
            paused = paused,
            micActive = false
        )
        ServiceCompat.startForeground(
            this,
            NotificationHelper.ID_ONGOING,
            notification,
            type
        )
    }

    private fun registerSensors() {
        val manager = sensorManager ?: return
        accelerometer?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME, mainHandler)
        }
        gyroscope?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME, mainHandler)
        }
        lightSensor?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, mainHandler)
        }
        proximitySensor?.let {
            manager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL, mainHandler)
        }
    }

    private fun unregisterSensors() {
        runCatching { sensorManager?.unregisterListener(this) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!windowCollecting) return
        val values = event.values
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                if (values.size >= 3) {
                    accumulator.onAccelerometer(
                        values[0], values[1], values[2], event.timestamp
                    )
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                if (values.size >= 3) {
                    accumulator.onGyroscope(
                        values[0], values[1], values[2], event.timestamp
                    )
                }
            }
            Sensor.TYPE_LIGHT -> accumulator.onLight(values.firstOrNull() ?: 0f)
            Sensor.TYPE_PROXIMITY -> accumulator.onProximity(values.firstOrNull() ?: 0f)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun startAudio(): SoundWindowAccumulator? {
        val settings = settingsCache ?: return null
        if (!settings.micEnabled) return null
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        return runCatching {
            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) return null
            val bufferBytes = max(minBuffer * 2, 8192)
            val record = AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferBytes
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return null
            }
            val accumulator = SoundWindowAccumulator(sampleRateHz = SAMPLE_RATE)
            audioActive = true
            record.startRecording()
            val thread = Thread({
                val buffer = ShortArray(2048)
                try {
                    while (audioActive) {
                        val read = record.read(buffer, 0, buffer.size)
                        if (read > 0) accumulator.feed(buffer, read)
                    }
                } catch (_: Throwable) {
                    // Áudio é descartado mesmo em falha; nada sensível vaza.
                } finally {
                    runCatching { record.stop() }
                    runCatching { record.release() }
                }
            }, "equilibrio-audio")
            audioThread = thread
            thread.start()
            accumulator
        }.getOrNull()
    }

    private fun stopAudio() {
        audioActive = false
        val thread = audioThread
        audioThread = null
        if (thread != null) {
            runCatching { thread.join(2_000L) }
        }
        bus.setMicActive(false)
    }

    private fun batteryPercent(): Int {
        val manager = getSystemService(BatteryManager::class.java)
        val capacity = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        if (capacity in 0..100) return capacity
        val intent = registerReceiver(
            null,
            android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
        )
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        return if (level >= 0 && scale > 0) level * 100 / scale else 100
    }

    override fun onDestroy() {
        loopJob?.cancel()
        windowCollecting = false
        unregisterSensors()
        stopAudio()
        bus.setRunning(false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.equilibrio.action.START"
        const val ACTION_RESUME = "com.equilibrio.action.RESUME"
        const val ACTION_PAUSE = "com.equilibrio.action.PAUSE"
        const val ACTION_STOP = "com.equilibrio.action.STOP"

        private const val SAMPLE_RATE = 16_000

        fun startIntent(context: Context): Intent =
            Intent(context, CollectionService::class.java).setAction(ACTION_START)

        fun pauseIntent(context: Context): Intent =
            Intent(context, CollectionService::class.java).setAction(ACTION_PAUSE)

        fun stopIntent(context: Context): Intent =
            Intent(context, CollectionService::class.java).setAction(ACTION_STOP)
    }
}
