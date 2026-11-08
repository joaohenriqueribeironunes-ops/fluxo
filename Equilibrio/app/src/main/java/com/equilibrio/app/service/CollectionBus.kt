package com.equilibrio.app.service

import com.equilibrio.core.model.LocalRisk
import com.equilibrio.core.model.SensorWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class CollectionState(
    val running: Boolean = false,
    val micActive: Boolean = false,
    val batteryPercent: Int = 100,
    val lowBattery: Boolean = false,
    val risk: LocalRisk? = null,
    val lastWindow: SensorWindow? = null,
    val lastError: String? = null
)

/**
 * Estado compartilhado entre o Foreground Service e a UI. Mantém apenas o
 * último risco agregado em memória — nada sensível é exposto fora do processo.
 */
@Singleton
class CollectionBus @Inject constructor() {

    private val _state = MutableStateFlow(CollectionState())
    val state: StateFlow<CollectionState> = _state.asStateFlow()

    fun setRunning(running: Boolean) = _state.update { it.copy(running = running) }

    fun setMicActive(active: Boolean) = _state.update { it.copy(micActive = active) }

    fun setBattery(percent: Int) = _state.update { it.copy(batteryPercent = percent) }

    fun setLowBattery(low: Boolean, percent: Int) =
        _state.update { it.copy(lowBattery = low, batteryPercent = percent) }

    fun updateRisk(risk: LocalRisk) = _state.update { it.copy(risk = risk) }

    fun updateWindow(window: SensorWindow) = _state.update { it.copy(lastWindow = window) }

    fun setError(message: String?) = _state.update { it.copy(lastError = message) }

    fun reset() = _state.update {
        CollectionState(batteryPercent = it.batteryPercent)
    }
}
