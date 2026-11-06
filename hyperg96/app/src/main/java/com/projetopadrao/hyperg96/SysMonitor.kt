package com.projetopadrao.hyperg96

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Amostrador periodico (1s) de CPU, RAM, temperatura termica e bateria.
 * Toda leitura usa APIs publicas: /proc/stat, /proc/loadavg, ActivityManager
 * e o listener de status termico (API 29+).
 */
class SysMonitor(
    private val context: Context,
    private val onUpdate: (Snapshot) -> Unit
) {

    data class Snapshot(
        val cpuPercent: Int?,
        val load1: String?,
        val ramUsedMb: Long,
        val ramTotalMb: Long,
        val thermal: String,
        val batteryPercent: Int,
        val charging: Boolean,
        val lowMemory: Boolean
    )

    private val handler = Handler(Looper.getMainLooper())
    private var lastCpu: LongArray? = null
    private var lastCpuAtMs = 0L
    private var batteryPercent = -1
    private var charging = false
    private var thermalLabel = "—"
    private var running = false

    private val thermalListener =
        PowerManager.OnThermalStatusChangedListener { status -> thermalLabel = thermalLabel(status) }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else -1
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        }
    }

    private val tick = object : Runnable {
        override fun run() {
            onUpdate(sample())
            handler.postDelayed(this, 1000)
        }
    }

    fun start() {
        if (running) return
        running = true
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= 29) pm.addThermalStatusListener(thermalListener)
        ContextCompat.registerReceiver(
            context, batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        handler.post(tick)
    }

    fun stop() {
        if (!running) return
        running = false
        handler.removeCallbacks(tick)
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
        if (Build.VERSION.SDK_INT >= 29) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.removeThermalStatusListener(thermalListener)
        }
    }

    private fun sample(): Snapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val totalMb = mi.totalMem / (1024 * 1024)
        val availMb = mi.availMem / (1024 * 1024)
        val (cpu, load1) = readCpu()
        return Snapshot(
            cpuPercent = cpu,
            load1 = load1,
            ramUsedMb = totalMb - availMb,
            ramTotalMb = totalMb,
            thermal = thermalLabel,
            batteryPercent = batteryPercent,
            charging = charging,
            lowMemory = mi.lowMemory
        )
    }

    /** Delta de /proc/stat entre amostras; fallback para /proc/loadavg. */
    private fun readCpu(): Pair<Int?, String?> {
        var load1: String? = null
        try {
            load1 = File("/proc/loadavg").readText().trim().split(Regex("\\s+")).firstOrNull()
        } catch (_: Exception) {
        }

        val percent = try {
            val line = File("/proc/stat").readLines().firstOrNull { it.startsWith("cpu ") }
            val fields = line?.trim()?.split(Regex("\\s+"))?.drop(1)
                ?.mapNotNull { it.toLongOrNull() }
            if (fields != null && fields.size >= 4 && fields.any { it > 0 }) {
                val total = fields.sum()
                val idle = fields[3] + fields.getOrElse(4) { 0L }
                val now = SystemClock.elapsedRealtime()
                val prev = lastCpu
                val prevAt = lastCpuAtMs
                lastCpu = longArrayOf(total, idle)
                lastCpuAtMs = now
                if (prev != null && total > prev[0] && now > prevAt) {
                    val dTotal = total - prev[0]
                    val dIdle = idle - prev[1]
                    ((dTotal - dIdle) * 100 / dTotal).toInt().coerceIn(0, 100)
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
        return percent to load1
    }

    private fun thermalLabel(status: Int): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "Normal"
        PowerManager.THERMAL_STATUS_LIGHT -> "Leve"
        PowerManager.THERMAL_STATUS_MODERATE -> "Moderado"
        PowerManager.THERMAL_STATUS_SEVERE -> "Severo"
        PowerManager.THERMAL_STATUS_CRITICAL -> "Crítico"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergência"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Desligamento iminente"
        else -> "—"
    }
}
